package com.meetflix.stream.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

/**
 * Design Pattern: Manager / Local HTTP Server
 * Lightweight embedded ServerSocket HTTP server serving video streams with HTTP Range (206 Partial Content) support
 * to enable zero-latency ExoPlayer seeking and smooth playback without sparse file corruption.
 */
class LocalMediaServer(private val context: Context, private val port: Int = 8080) {

    companion object {
        private const val TAG = "MeetflixStream"
    }

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var isRunning = false

    @Volatile
    private var mediaUri: Uri? = null

    @Volatile
    private var mediaFile: File? = null

    @Volatile
    private var expectedFileSize: Long = -1L

    @Volatile
    private var fastStartMeta: FastStartRemuxer.FastStartMeta? = null

    fun setMediaUri(uri: Uri) {
        this.mediaUri = uri
        this.mediaFile = null
        this.expectedFileSize = -1L
        this.fastStartMeta = null
        Log.d(TAG, "LocalMediaServer configured with Uri: $uri. Analyzing MP4 for Fast-Start...")
        kotlin.concurrent.thread(name = "MeetflixFastStartAnalyzer") {
            val meta = FastStartRemuxer.analyzeAndPrepareFastStart(context, uri)
            fastStartMeta = meta
            Log.d(TAG, "Fast-Start analysis completed for $uri: isFastStartNeeded=${meta?.isFastStartNeeded}, virtualSize=${meta?.totalVirtualSize}")
        }
    }

    fun setMediaFile(file: File, totalExpectedBytes: Long = -1L) {
        this.mediaFile = file
        this.mediaUri = null
        this.expectedFileSize = totalExpectedBytes
        this.fastStartMeta = null
        Log.d(TAG, "LocalMediaServer configured with File: ${file.absolutePath}, expectedBytes=$totalExpectedBytes")
    }

    fun updateExpectedFileSize(totalExpectedBytes: Long) {
        if (totalExpectedBytes > 0L) {
            this.expectedFileSize = totalExpectedBytes
            Log.d(TAG, "LocalMediaServer expectedFileSize updated to: $totalExpectedBytes")
        }
    }

    fun getExpectedFileSize(): Long = expectedFileSize

    fun start() {
        if (isRunning && serverSocket?.isClosed == false) return
        try {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(port))
            serverSocket = socket
            isRunning = true
            Log.d(TAG, "LocalMediaServer started successfully on port $port")

            thread(name = "MeetflixLocalMediaServerThread") {
                while (isRunning && serverSocket?.isClosed == false) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        thread(name = "MeetflixClientHandlerThread") {
                            handleClientRequest(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isRunning) break
                        Log.e(TAG, "Error accepting client connection: ${e.localizedMessage}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocalMediaServer on port $port: ${e.localizedMessage}", e)
        }
    }

    private fun handleClientRequest(socket: Socket) {
        try {
            socket.soTimeout = 60000
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            val buffer = ByteArray(8192)
            val bytesRead = inputStream.read(buffer)
            if (bytesRead <= 0) {
                socket.close()
                return
            }

            val requestString = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
            Log.d(TAG, "HTTP Request received:\n${requestString.lines().firstOrNull()}")

            var rangeHeader: String? = null
            requestString.lines().forEach { line ->
                if (line.startsWith("Range:", ignoreCase = true)) {
                    rangeHeader = line.substringAfter(":").trim()
                }
            }

            val targetFile = mediaFile
            val targetUri = mediaUri
            val meta = fastStartMeta

            val fileSize: Long = when {
                meta != null && meta.isFastStartNeeded -> meta.totalVirtualSize
                expectedFileSize > 0L -> expectedFileSize
                targetFile != null && targetFile.exists() -> targetFile.length()
                targetUri != null -> {
                    try {
                        context.contentResolver.openFileDescriptor(targetUri, "r")?.use { it.statSize } ?: 0L
                    } catch (e: Exception) { 0L }
                }
                else -> 0L
            }

            if (fileSize <= 0L) {
                send404Response(outputStream)
                socket.close()
                return
            }

            var startByte = 0L
            var endByte = fileSize - 1L

            rangeHeader?.let { range ->
                if (range.startsWith("bytes=")) {
                    val rangeSpec = range.substringAfter("bytes=").trim()
                    val ranges = rangeSpec.split("-")
                    if (ranges[0].isEmpty() && ranges.size > 1 && ranges[1].isNotEmpty()) {
                        val suffixLen = ranges[1].toLongOrNull() ?: 0L
                        startByte = (fileSize - suffixLen).coerceAtLeast(0L)
                        endByte = fileSize - 1L
                    } else {
                        if (ranges[0].isNotEmpty()) {
                            startByte = ranges[0].toLongOrNull() ?: 0L
                        }
                        if (ranges.size > 1 && ranges[1].isNotEmpty()) {
                            endByte = ranges[1].toLongOrNull() ?: (fileSize - 1L)
                        }
                    }
                }
            }

            startByte = startByte.coerceIn(0L, fileSize - 1L)
            endByte = endByte.coerceIn(startByte, fileSize - 1L)
            val contentLength = endByte - startByte + 1L

            val statusLine = if (rangeHeader != null) "HTTP/1.1 206 Partial Content" else "HTTP/1.1 200 OK"
            val headers = StringBuilder()
                .append("$statusLine\r\n")
                .append("Content-Type: video/mp4\r\n")
                .append("Accept-Ranges: bytes\r\n")
                .append("Content-Length: $contentLength\r\n")

            if (rangeHeader != null) {
                headers.append("Content-Range: bytes $startByte-$endByte/$fileSize\r\n")
            }
            headers.append("Connection: close\r\n\r\n")

            outputStream.write(headers.toString().toByteArray(StandardCharsets.UTF_8))
            outputStream.flush()

            streamVideoData(targetFile, targetUri, startByte, contentLength, outputStream)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling HTTP client request: ${e.localizedMessage}")
        } finally {
            try {
                socket.close()
            } catch (ignored: Exception) {}
        }
    }

    private fun streamVideoData(
        file: File?,
        uri: Uri?,
        startByte: Long,
        length: Long,
        outputStream: OutputStream
    ) {
        val meta = fastStartMeta
        if (meta != null && meta.isFastStartNeeded && uri != null) {
            streamVirtualFastStartVideo(meta, uri, startByte, length, outputStream)
            return
        }

        val buffer = ByteArray(64 * 1024)
        var remainingBytes = length

        if (file != null && file.exists()) {
            FileInputStream(file).use { fis ->
                if (startByte > 0) {
                    fis.channel.position(startByte)
                }
                var idleAttempts = 0
                while (remainingBytes > 0) {
                    val bytesToRead = remainingBytes.coerceAtMost(buffer.size.toLong()).toInt()
                    val read = fis.read(buffer, 0, bytesToRead)
                    if (read > 0) {
                        idleAttempts = 0
                        outputStream.write(buffer, 0, read)
                        remainingBytes -= read
                    } else {
                        // File currently hasn't reached remainingBytes; wait for background writer thread
                        if (expectedFileSize > 0 && file.length() < expectedFileSize && idleAttempts < 1200) {
                            idleAttempts++
                            Thread.sleep(50L)
                        } else {
                            break
                        }
                    }
                }
            }
        } else if (uri != null) {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            pfd?.use { fd ->
                FileInputStream(fd.fileDescriptor).use { fis ->
                    if (startByte > 0) {
                        fis.channel.position(startByte)
                    }
                    while (remainingBytes > 0) {
                        val bytesToRead = remainingBytes.coerceAtMost(buffer.size.toLong()).toInt()
                        val read = fis.read(buffer, 0, bytesToRead)
                        if (read <= 0) break
                        outputStream.write(buffer, 0, read)
                        remainingBytes -= read
                    }
                }
            }
        }
        outputStream.flush()
    }

    private fun streamVirtualFastStartVideo(
        meta: FastStartRemuxer.FastStartMeta,
        uri: Uri,
        startByte: Long,
        length: Long,
        outputStream: OutputStream
    ) {
        val ftypEnd = meta.ftypBox.size
        val moovEnd = meta.ftypBox.size + meta.moovBox.size
        val buffer = ByteArray(64 * 1024)

        var virtualPos = startByte
        var remaining = length

        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return
        pfd.use { fd ->
            FileInputStream(fd.fileDescriptor).use { fis ->
                while (remaining > 0 && virtualPos < meta.totalVirtualSize) {
                    when {
                        virtualPos < ftypEnd -> {
                            val chunkSize = (ftypEnd - virtualPos).coerceAtMost(remaining).toInt().coerceAtMost(buffer.size)
                            fis.channel.position(meta.ftypBox.offset + virtualPos)
                            val read = fis.read(buffer, 0, chunkSize)
                            if (read <= 0) break
                            outputStream.write(buffer, 0, read)
                            virtualPos += read
                            remaining -= read
                        }
                        virtualPos in ftypEnd until moovEnd -> {
                            val moovRamOffset = (virtualPos - ftypEnd).toInt()
                            val bytesAvailableInMoov = meta.patchedMoovBytes.size - moovRamOffset
                            val chunkSize = bytesAvailableInMoov.coerceAtMost(remaining.toInt()).coerceAtMost(buffer.size)
                            if (chunkSize <= 0) break
                            outputStream.write(meta.patchedMoovBytes, moovRamOffset, chunkSize)
                            virtualPos += chunkSize
                            remaining -= chunkSize
                        }
                        else -> {
                            val mdatVirtualOffset = virtualPos - moovEnd
                            val realMdatOffset = meta.mdatBox.offset + mdatVirtualOffset
                            val chunkSize = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                            fis.channel.position(realMdatOffset)
                            val read = fis.read(buffer, 0, chunkSize)
                            if (read <= 0) break
                            outputStream.write(buffer, 0, read)
                            virtualPos += read
                            remaining -= read
                        }
                    }
                }
            }
        }
        outputStream.flush()
    }

    private fun send404Response(outputStream: OutputStream) {
        val response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        outputStream.write(response.toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (netInterface in interfaces) {
                if (!netInterface.isUp || netInterface.isLoopback) continue
                val addresses = netInterface.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress
                        if (ip != null && !ip.startsWith("127.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering local IP: ${e.localizedMessage}")
        }
        return null
    }

    fun getStreamUrl(hostIp: String? = null): String {
        val ip = hostIp ?: getLocalIpAddress() ?: "127.0.0.1"
        val url = "http://$ip:$port/stream.mp4"
        Log.d(TAG, "Generated LocalMediaServer stream URL: $url (discovered IP: $ip)")
        return url
    }

    fun getServerUrl(hostIp: String = "127.0.0.1"): String {
        return getStreamUrl(hostIp)
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
            serverSocket = null
            Log.d(TAG, "LocalMediaServer stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping LocalMediaServer: ${e.localizedMessage}")
        }
    }
}
