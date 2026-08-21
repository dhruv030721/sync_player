# Comprehensive Architecture & Code Guide: Virtual Fast-Start MP4 & Embedded Local HTTP Media Server

This document provides a deep, technical, code-level and architectural breakdown of the **Virtual Fast-Start MP4 P2P Streaming Engine** and **Embedded Local HTTP Media Server** (`LocalMediaServer.kt` & `FastStartRemuxer.kt`) implemented in Meetflix.

---

## 1. Executive Summary & Architectural Overview

When sharing large video files (e.g. a 2.3 GB movie or 1-hour 4K recording) between devices over peer-to-peer (P2P) networks, traditional file-transfer methods fail because:
1. **Whole-File Download Latency**: The consumer device must wait for gigabytes of data to copy before playback can start.
2. **MP4 Container Metadata Trap**: MP4 containers default to placing their metadata index atom (`moov`) at the **very end of the file**. A remote player trying to open the file gets stuck in an infinite buffering loop waiting for the last bytes of the 2.3 GB file to arrive.

### The Meetflix Solution
Meetflix resolves both issues simultaneously using a **Zero-Copy Virtual Fast-Start Remuxing Engine** paired with an **Embedded Local HTTP Media Server**:

```text
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│                                    HOST DEVICE (DEVICE A)                                │
│                                                                                          │
│  ┌──────────────────────────┐     FastStartRemuxer     ┌──────────────────────────────┐  │
│  │ 2.3 GB MP4 File on Disk  │ ───────────────────────► │ Patched `moov` Atom in RAM   │  │
│  │  [ftyp]  [mdat]  [moov]  │  (Zero Disk Rewriting)   │ (Sample offsets shifted by   │  │
│  └────────────┬─────────────┘                          │  + moov.size)                │  │
│               │                                        └──────────────┬───────────────┘  │
│               │                                                       │                  │
│               └──────────────────────────┬────────────────────────────┘                  │
│                                          ▼                                               │
│                               LocalMediaServer (Port 8080)                               │
│                         (Translates virtual ranges on the fly)                           │
└──────────────────────────────────────────┬───────────────────────────────────────────────┘
                                           │
                                  Wi-Fi Direct P2P Link
                                 (http://192.168.49.1:8080)
                                           │
┌──────────────────────────────────────────▼───────────────────────────────────────────────┐
│                                 CONSUMER DEVICE (DEVICE B)                               │
│                                                                                          │
│                                 Media3 ExoPlayer                                         │
│                    1. Sends `GET /stream.mp4 Range: bytes=0-2097151`                    │
│                    2. Receives [ftyp] + [patched moov] in ~100 ms!                       │
│                    3. State: STATE_READY ──► Plays instantly at 00:00:00                 │
│                    4. Seeking to 00:45:00 ──► Sends `Range: bytes=X-`                    │
│                       LocalMediaServer seeks X directly ──► Plays instantly!             │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Deep Dive: MP4 Container Anatomy & The `moov` Fast-Start Atom

An MP4 container is composed of discrete, self-contained data structures called **atoms** (or **boxes**). Every box begins with an 8-byte header:

```text
┌──────────────────────────────┬──────────────────────────────┬──────────────────────────────┐
│     4 Bytes: Box Length      │      4 Bytes: Box Type       │         Box Payload          │
│     (Big-Endian uint32)      │        (ASCII String)        │                              │
└──────────────────────────────┴──────────────────────────────┴──────────────────────────────┘
```

### The 3 Core MP4 Boxes

1. **`ftyp` (File Type Box)**:
   - Contains 32 bytes at offset 0 identifying file format compatibility (e.g. `mp42`, `isom`).
2. **`mdat` (Media Data Box)**:
   - Contains raw encoded H.264 / HEVC video frames and AAC audio packets. Takes up 99% of file size (e.g. 2.29 GB).
3. **`moov` (Movie Metadata Box)**:
   - Contains the master timeline map, track definitions, codec parameters, and sample chunk offset tables (`stco` / `co64`).

---

### The `moov` Metadata Problem in Web/P2P Streaming

In standard recorded MP4 files, boxes are ordered like this:
```text
[ftyp (32 B)] ──► [mdat (2.29 GB Raw Media)] ──► [moov (100 KB Index)]
```

When ExoPlayer attempts to open this stream over a network:
1. ExoPlayer reads `ftyp` (first 32 bytes).
2. To build its playback timeline and decode video frames, ExoPlayer **must read the `moov` atom**.
3. ExoPlayer sends an HTTP Range request for the **end of the file** (`Range: bytes=2298700000-`).
4. Over a P2P stream where bytes arrive sequentially from byte 0, byte 2.3 GB is not available yet! ExoPlayer freezes in `STATE_BUFFERING`.

---

### The Fast-Start Solution

Fast-Start reorganizes the MP4 box sequence so `moov` appears **before** `mdat`:
```text
[ftyp (32 B)] ──► [moov (100 KB Index)] ──► [mdat (2.29 GB Raw Media)]
```

With Fast-Start:
1. ExoPlayer sends `GET /stream.mp4 Range: bytes=0-2097151` (first 2 MB).
2. ExoPlayer receives `ftyp` **and** `moov` in the single first response packet!
3. ExoPlayer parses all video tracks, keyframe tables, and duration metadata in $\sim 100\text{ ms}$, transitions to `STATE_READY`, and starts playing immediately!

---

## 3. Code & Algorithm: Zero-Copy `FastStartRemuxer.kt`

Re-writing a 2.3 GB file on storage to move `moov` to the front would take 10–20 seconds and consume double disk space. Meetflix uses **Zero-Copy Virtual Fast-Start Remuxing** in RAM:

### Step 1: Box Parsing & Location Detection
`FastStartRemuxer.analyzeAndPrepareFastStart` scans top-level boxes in the MP4 file:

```kotlin
var currentOffset = 0L
val headerBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)

while (currentOffset < fileSize - 8) {
    fileChannel.position(currentOffset)
    fileChannel.read(headerBuffer)
    headerBuffer.flip()

    var boxSize = headerBuffer.getInt().toLong() and 0xFFFFFFFFL
    val typeBytes = ByteArray(4)
    headerBuffer.get(typeBytes)
    val boxType = String(typeBytes, Charsets.US_ASCII)

    boxes.add(Mp4BoxInfo(boxType, currentOffset, boxSize))
    currentOffset += boxSize
}
```

If `moovBox.offset < mdatBox.offset`, the file is already in Fast-Start order (`[ftyp][moov][mdat]`). No remuxing is required.

---

### Step 2: Extracting `moov` & Patching Chunk Offsets (`stco` / `co64`)

If `moovBox.offset > mdatBox.offset`, `moov` is at the end. Moving `moov` in front of `mdat` shifts `mdat` backward on disk by $moov.size$ bytes.

Therefore, **every chunk offset inside the `stco` (32-bit) and `co64` (64-bit) sample tables must be patched**:

$$\text{New Chunk Offset} = \text{Old Chunk Offset} + \text{moov.size}$$

```kotlin
// FastStartRemuxer.kt: Patching stco (Sample Table Chunk Offset) boxes
private fun patchChunkOffsets(moovBytes: ByteArray, shiftAmount: Long) {
    var i = 0
    while (i < moovBytes.size - 8) {
        // Match 'stco' (0x7374636F) 32-bit chunk offset box
        if (moovBytes[i] == 's'.code.toByte() &&
            moovBytes[i + 1] == 't'.code.toByte() &&
            moovBytes[i + 2] == 'c'.code.toByte() &&
            moovBytes[i + 3] == 'o'.code.toByte()
        ) {
            val entryCount = readInt32(moovBytes, i + 8)
            var entryPos = i + 12
            for (c in 0 until entryCount) {
                val oldOffset = readInt32(moovBytes, entryPos).toLong() and 0xFFFFFFFFL
                val newOffset = oldOffset + shiftAmount
                writeInt32(moovBytes, entryPos, newOffset.toInt())
                entryPos += 4
            }
        }
        // Match 'co64' (0x636F3634) 64-bit chunk offset box
        else if (moovBytes[i] == 'c'.code.toByte() &&
            moovBytes[i + 1] == 'o'.code.toByte() &&
            moovBytes[i + 2] == '6'.code.toByte() &&
            moovBytes[i + 3] == '4'.code.toByte()
        ) {
            val entryCount = readInt32(moovBytes, i + 8)
            var entryPos = i + 12
            for (c in 0 until entryCount) {
                val oldOffset = readInt64(moovBytes, entryPos)
                val newOffset = oldOffset + shiftAmount
                writeInt64(moovBytes, entryPos, newOffset)
                entryPos += 8
            }
        }
        i++
    }
}
```

---

## 4. Embedded Local HTTP Server (`LocalMediaServer.kt`)

`LocalMediaServer.kt` runs an embedded Java `ServerSocket` on port `8080` on a background thread (`MeetflixLocalMediaServerThread`).

### HTTP `206 Partial Content` & Range Header Response
When ExoPlayer sends `Range: bytes=startByte-endByte`:
1. `LocalMediaServer` parses `startByte` and `endByte`.
2. Computes `contentLength = endByte - startByte + 1`.
3. Responds with `HTTP/1.1 206 Partial Content`:

```http
HTTP/1.1 206 Partial Content
Content-Type: video/mp4
Accept-Ranges: bytes
Content-Length: 2097152
Content-Range: bytes 0-2097151/2298817632
Connection: close
```

---

### On-The-Fly Virtual Byte Range Translation (`streamVirtualFastStartVideo`)

`LocalMediaServer` maps incoming virtual byte offsets to disk/RAM locations dynamically:

```kotlin
// Virtual Layout: [ ftyp (32B) ] [ patched moov (RAM) ] [ mdat (Disk) ]
val ftypEnd = meta.ftypBox.size
val moovEnd = meta.ftypBox.size + meta.moovBox.size

while (remaining > 0 && virtualPos < meta.totalVirtualSize) {
    when {
        // 1. Virtual Range inside ftyp: Read from file at ftyp.offset
        virtualPos < ftypEnd -> {
            fis.channel.position(meta.ftypBox.offset + virtualPos)
            val read = fis.read(buffer, 0, chunkSize)
            outputStream.write(buffer, 0, read)
            virtualPos += read
        }
        // 2. Virtual Range inside moov: Serve directly from RAM!
        virtualPos in ftypEnd until moovEnd -> {
            val moovRamOffset = (virtualPos - ftypEnd).toInt()
            val chunkSize = (meta.patchedMoovBytes.size - moovRamOffset).coerceAtMost(remaining.toInt())
            outputStream.write(meta.patchedMoovBytes, moovRamOffset, chunkSize)
            virtualPos += chunkSize
        }
        // 3. Virtual Range inside mdat: Map virtual offset back to real mdat offset in file!
        else -> {
            val mdatVirtualOffset = virtualPos - moovEnd
            val realMdatOffset = meta.mdatBox.offset + mdatVirtualOffset
            fis.channel.position(realMdatOffset)
            val read = fis.read(buffer, 0, chunkSize)
            outputStream.write(buffer, 0, read)
            virtualPos += read
        }
    }
}
```

---

## 5. End-to-End Data Flow & Dual-Device Sync

```text
[ HOST DEVICE A ]                                                [ CONSUMER DEVICE B ]
─────────────────                                                ─────────────────────
1. Selects 2.3 GB video URI.
2. PartyViewModel sets UI state:
   `isPreparingMedia = true`
   UI shows: "Optimizing video..."
3. FastStartRemuxer parses boxes
   & patches `stco` in RAM (~50 ms).
4. Host ExoPlayer loads local `videoUri`
   directly (100% exact duration,
   controls, & 0 ms loopback delay).
5. LocalMediaServer starts on :8080.
6. P2P payload broadcast:
   `STREAM_URL:http://192.168.49.1:8080/stream.mp4` ────────────► 7. Receives `STREAM_URL`.
                                                                   8. Consumer ExoPlayer calls
                                                                      `setMediaItem("http://192.168.49.1:8080/stream.mp4")`.
                                                                   
                                                                9. Consumer ExoPlayer sends:
                                                                   `GET /stream.mp4 Range: bytes=0-2097151`
10. LocalMediaServer Virtual Streamer: ◄─────────────────────────── (Requests initial 2 MB)
    • Streams [ftyp (32 B)]
    • Streams [patched moov (RAM)] ──────────────────────────────► 11. Consumer ExoPlayer receives ftyp + moov.
                                                                       Parses timeline & track map in ~100 ms!
                                                                       State: STATE_READY
                                                                       Playback STARTS simultaneously at 00:00:00!

                                                                12. User seeks on Consumer to 00:45:00!
                                                                    Consumer ExoPlayer computes byte offset X = 1.4 GB.
                                                                    Sends `GET /stream.mp4 Range: bytes=1400000000-`.
13. LocalMediaServer: ◄────────────────────────────────────────────
    • `RandomAccessFile.seek(1400000000)`
    • Streams 2 MB chunk from offset X ──────────────────────────► 14. Consumer plays at 00:45:00 instantly!
```

---

## 6. Key Configuration & Player Mechanics

### 1. Android Cleartext HTTP Traffic Policy (`AndroidManifest.xml`)
On Android 9+ (API 28+), Android blocks unencrypted `http://` traffic by default.
We declared `android:usesCleartextTraffic="true"` in `AndroidManifest.xml` to allow consumer ExoPlayer instances to connect to `http://192.168.x.x:8080/stream.mp4` over local P2P networks without throwing `CleartextNotPermittedException`.

### 2. Precise `HH:mm:ss` Timestamp Formatting (`MFXSeekBar.kt`)
To correctly display videos over 1 hour (e.g. 1hr 4min movie = 3840s):

```kotlin
private fun formatTime(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
```

---

## Summary Matrix

| Metric | Previous Approach | Fast-Start + LocalMediaServer Engine |
| :--- | :--- | :--- |
| **Preparation Delay** | Wait for 2.3 GB file copy (2–5 mins) | **~50 ms** (Virtual Fast-Start in RAM) |
| **Disk Space Overhead** | Doubles disk usage (copies file) | **0 Bytes** (Zero disk copying) |
| **Consumer Start Time** | Fails or stalls in infinite buffering | **~100 ms** (First 2 MB Range request) |
| **Seeking Latency** | Fails / stalls on deep seeks | **Instant** (`RandomAccessFile.seek(X)`) |
| **Host Duration Display** | `00:00` (loopback error) | **100% Exact** (Direct local storage playback) |
