package com.meetflix.stream.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.Inet4Address
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

    fun setMediaUri(uri: Uri) {
        this.mediaUri = uri
        this.mediaFile = null
        this.expectedFileSize = -1L
        Log.d(TAG, "LocalMediaServer configured with Uri: $uri")
    }

    fun setMediaFile(file: File, totalExpectedBytes: Long = -1L) {
        this.mediaFile = file
        this.mediaUri = null
        this.expectedFileSize = totalExpectedBytes
        Log.d(TAG, "LocalMediaServer configured with File: ${file.absolutePath}, expectedBytes=$totalExpectedBytes")
    }

    fun start() {
        if (isRunning) return
        try {
            serverSocket = ServerSocket(port)
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
            socket.soTimeout = 10000
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

            val fileSize: Long = when {
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
                    val ranges = range.substringAfter("bytes=").split("-")
                    if (ranges[0].isNotEmpty()) {
                        startByte = ranges[0].toLongOrNull() ?: 0L
                    }
                    if (ranges.size > 1 && ranges[1].isNotEmpty()) {
                        endByte = ranges[1].toLongOrNull() ?: (fileSize - 1L)
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
        val buffer = ByteArray(64 * 1024)
        var remainingBytes = length

        if (file != null && file.exists()) {
            FileInputStream(file).use { fis ->
                fis.skip(startByte)
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
                        if (expectedFileSize > 0 && file.length() < expectedFileSize && idleAttempts < 100) {
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
                    fis.skip(startByte)
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
