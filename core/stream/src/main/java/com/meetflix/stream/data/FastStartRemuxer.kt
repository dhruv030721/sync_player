package com.meetflix.stream.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Design Pattern: Transmuxer / Parser
 * Extracts MP4 box structures, parses top-level atoms (ftyp, mdat, moov), and patches moov stco/co64 sample chunk tables
 * for zero-copy virtual fast-start streaming over HTTP.
 */
object FastStartRemuxer {

    private const val TAG = "MeetflixStream"

    data class Mp4BoxInfo(
        val type: String,
        val offset: Long,
        val size: Long
    )

    data class FastStartMeta(
        val ftypBox: Mp4BoxInfo,
        val mdatBox: Mp4BoxInfo,
        val moovBox: Mp4BoxInfo,
        val patchedMoovBytes: ByteArray,
        val totalVirtualSize: Long,
        val isFastStartNeeded: Boolean
    )

    fun analyzeAndPrepareFastStart(context: Context, videoUri: Uri): FastStartMeta? {
        return try {
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(videoUri, "r") ?: return null
            pfd.use { descriptor ->
                val fileSize = descriptor.statSize
                val boxes = mutableListOf<Mp4BoxInfo>()
                val fileChannel = FileInputStream(descriptor.fileDescriptor).channel

                var currentOffset = 0L
                val headerBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)

                while (currentOffset < fileSize - 8) {
                    headerBuffer.clear()
                    fileChannel.position(currentOffset)
                    val read = fileChannel.read(headerBuffer)
                    if (read < 8) break

                    headerBuffer.flip()
                    var boxSize = headerBuffer.getInt().toLong() and 0xFFFFFFFFL
                    val typeBytes = ByteArray(4)
                    headerBuffer.get(typeBytes)
                    val boxType = String(typeBytes, Charsets.US_ASCII)

                    var headerLen = 8L
                    if (boxSize == 1L) {
                        val extBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                        fileChannel.read(extBuffer)
                        extBuffer.flip()
                        boxSize = extBuffer.getLong()
                        headerLen = 16L
                    } else if (boxSize == 0L) {
                        boxSize = fileSize - currentOffset
                    }

                    if (boxSize < headerLen) break
                    boxes.add(Mp4BoxInfo(boxType, currentOffset, boxSize))
                    Log.d(TAG, "Parsed MP4 Box: type=$boxType, offset=$currentOffset, size=$boxSize")

                    if (boxType == "moov" || boxType == "mdat") {
                        val ftyp = boxes.find { it.type == "ftyp" }
                        val mdat = boxes.find { it.type == "mdat" }
                        val moov = boxes.find { it.type == "moov" }
                        if (ftyp != null && mdat != null && moov != null) {
                            break
                        }
                    }
                    currentOffset += boxSize
                }

                val ftypBox = boxes.find { it.type == "ftyp" } ?: return null
                val mdatBox = boxes.find { it.type == "mdat" } ?: return null
                val moovBox = boxes.find { it.type == "moov" } ?: return null

                if (moovBox.offset < mdatBox.offset) {
                    Log.d(TAG, "MP4 file is already in Fast-Start order ([ftyp][moov][mdat]). No remuxing required.")
                    return FastStartMeta(
                        ftypBox = ftypBox,
                        mdatBox = mdatBox,
                        moovBox = moovBox,
                        patchedMoovBytes = ByteArray(0),
                        totalVirtualSize = fileSize,
                        isFastStartNeeded = false
                    )
                }

                Log.d(TAG, "MP4 file needs Fast-Start relocation ([ftyp][mdat][moov] -> [ftyp][moov][mdat]). Extracting moov (${moovBox.size} bytes)...")

                val moovBuffer = ByteBuffer.allocate(moovBox.size.toInt()).order(ByteOrder.BIG_ENDIAN)
                fileChannel.position(moovBox.offset)
                fileChannel.read(moovBuffer)
                val moovBytes = moovBuffer.array()

                patchChunkOffsets(moovBytes, moovBox.size)

                FastStartMeta(
                    ftypBox = ftypBox,
                    mdatBox = mdatBox,
                    moovBox = moovBox,
                    patchedMoovBytes = moovBytes,
                    totalVirtualSize = fileSize,
                    isFastStartNeeded = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing MP4 for Fast-Start: ${e.localizedMessage}", e)
            null
        }
    }

    private fun patchChunkOffsets(moovBytes: ByteArray, shiftAmount: Long) {
        var patchedCount = 0
        var i = 0
        while (i < moovBytes.size - 8) {
            if (moovBytes[i] == 's'.code.toByte() &&
                moovBytes[i + 1] == 't'.code.toByte() &&
                moovBytes[i + 2] == 'c'.code.toByte() &&
                moovBytes[i + 3] == 'o'.code.toByte()
            ) {
                val entryCount = readInt32(moovBytes, i + 8)
                var entryPos = i + 12
                for (c in 0 until entryCount) {
                    if (entryPos + 4 > moovBytes.size) break
                    val oldOffset = readInt32(moovBytes, entryPos).toLong() and 0xFFFFFFFFL
                    val newOffset = oldOffset + shiftAmount
                    writeInt32(moovBytes, entryPos, newOffset.toInt())
                    entryPos += 4
                    patchedCount++
                }
            } else if (moovBytes[i] == 'c'.code.toByte() &&
                moovBytes[i + 1] == 'o'.code.toByte() &&
                moovBytes[i + 2] == '6'.code.toByte() &&
                moovBytes[i + 3] == '4'.code.toByte()
            ) {
                val entryCount = readInt32(moovBytes, i + 8)
                var entryPos = i + 12
                for (c in 0 until entryCount) {
                    if (entryPos + 8 > moovBytes.size) break
                    val oldOffset = readInt64(moovBytes, entryPos)
                    val newOffset = oldOffset + shiftAmount
                    writeInt64(moovBytes, entryPos, newOffset)
                    entryPos += 8
                    patchedCount++
                }
            }
            i++
        }
        Log.d(TAG, "Patched $patchedCount chunk offsets in moov atom by +$shiftAmount bytes.")
    }

    private fun readInt32(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun writeInt32(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 24).toByte()
        bytes[offset + 1] = (value ushr 16).toByte()
        bytes[offset + 2] = (value ushr 8).toByte()
        bytes[offset + 3] = value.toByte()
    }

    private fun readInt64(bytes: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0 until 8) {
            result = (result shl 8) or (bytes[offset + i].toLong() and 0xFFL)
        }
        return result
    }

    private fun writeInt64(bytes: ByteArray, offset: Int, value: Long) {
        for (i in 0 until 8) {
            bytes[offset + i] = (value ushr (56 - i * 8)).toByte()
        }
    }
}
