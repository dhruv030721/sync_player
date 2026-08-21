package com.meetflix.connections.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.meetflix.connections.domain.models.AvailableHost
import com.meetflix.connections.domain.models.ConnectionRole
import com.meetflix.connections.domain.models.HostConnectionStatus
import com.meetflix.connections.domain.models.NearbyConnectionState
import com.meetflix.connections.domain.models.VideoChunkMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * Design Pattern: Manager / Native Wrapper
 * Encapsulates Google Nearby Connections P2P API for device discovery, connection management,
 * byte control action synchronization, and P2P video file payload transfers.
 */
class NearbyConnectionsManager(private val context: Context) {

    companion object {
        private const val TAG = "MeetflixStream"
        private const val CHNK_MAGIC = 0x43484E4B // "CHNK"
        const val SERVICE_ID = "com.meetflix.RFC01_SERVICE"
        val STRATEGY = Strategy.P2P_STAR
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val _uiState = MutableStateFlow(NearbyConnectionState(localEndpointName = getDeviceName()))
    val uiState: StateFlow<NearbyConnectionState> = _uiState.asStateFlow()

    private val incomingFilePayloads = ConcurrentHashMap<Long, File>()
    private val expectedFileSizes = ConcurrentHashMap<Long, Long>()
    private val receivedChunkSets = ConcurrentHashMap<String, MutableSet<Int>>()
    @Volatile
    private var hostVideoUri: Uri? = null

    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.1f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun updatePermissionsState(hasPermissions: Boolean) {
        _uiState.update { it.copy(hasRequiredPermissions = hasPermissions) }
    }

    fun setLocalName(name: String) {
        if (name.isNotBlank()) {
            _uiState.update { it.copy(localEndpointName = name.trim()) }
        }
    }

    fun setHostVideoUri(uri: Uri?) {
        hostVideoUri = uri
        Log.d(TAG, "setHostVideoUri registered: $uri")
    }

    // Send single video chunk payload
    fun sendChunkPayload(endpointId: String, chunkMeta: VideoChunkMeta, chunkBytes: ByteArray) {
        try {
            val metaJson = json.encodeToString(chunkMeta)
            val metaBytes = metaJson.toByteArray(StandardCharsets.UTF_8)
            val totalLength = 4 + 4 + metaBytes.size + chunkBytes.size
            val buffer = ByteBuffer.allocate(totalLength)
            buffer.putInt(CHNK_MAGIC)
            buffer.putInt(metaBytes.size)
            buffer.put(metaBytes)
            buffer.put(chunkBytes)

            val payload = Payload.fromBytes(buffer.array())
            connectionsClient.sendPayload(endpointId, payload)
            Log.d(TAG, "Sent CHNK payload to $endpointId: chunk ${chunkMeta.chunkIndex + 1}/${chunkMeta.totalChunks} (${chunkBytes.size} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending CHNK payload to $endpointId: ${e.localizedMessage}", e)
        }
    }

    private fun handleReceivedChunk(chunkMeta: VideoChunkMeta, chunkData: ByteArray) {
        val tempFile = File(context.cacheDir, "stream_${chunkMeta.streamId}.mp4")
        try {
            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.seek(chunkMeta.startByte)
                raf.write(chunkData)
            }

            val set = receivedChunkSets.getOrPut(chunkMeta.streamId) { ConcurrentHashMap.newKeySet() }
            set.add(chunkMeta.chunkIndex)

            val progress = (set.size.toFloat() / chunkMeta.totalChunks.toFloat()).coerceIn(0f, 1f)
            val statusMsg = "Streaming chunk ${chunkMeta.chunkIndex + 1}/${chunkMeta.totalChunks} (${(progress * 100).toInt()}%) - ${formatFileSize(set.size * chunkMeta.chunkSize.toLong())} / ${formatFileSize(chunkMeta.totalFileSize)}"

            Log.d(TAG, "Written chunk ${chunkMeta.chunkIndex + 1}/${chunkMeta.totalChunks} at ${chunkMeta.startByte}. Total received: ${set.size}/${chunkMeta.totalChunks}")

            // Early Playback Trigger: Once 16 chunks (~1MB) are written, emit receivedVideoUri
            val earlyTriggerThreshold = 16.coerceAtMost(chunkMeta.totalChunks)
            _uiState.update { current ->
                val newUri = if (set.size >= earlyTriggerThreshold && current.receivedVideoUri == null) {
                    Uri.fromFile(tempFile).toString()
                } else current.receivedVideoUri

                current.copy(
                    isTransferring = set.size < chunkMeta.totalChunks,
                    transferProgress = progress,
                    transferStatusMessage = statusMsg,
                    receivedVideoUri = newUri
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing received chunk ${chunkMeta.chunkIndex}: ${e.localizedMessage}", e)
        }
    }

    // 1. Start Advertising (Host Mode)
    fun startAdvertising(customName: String? = null) {
        val name = customName ?: _uiState.value.localEndpointName
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        Log.d(TAG, "startAdvertising: '$name'")

        connectionsClient.startAdvertising(
            name,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "startAdvertising SUCCESS")
            _uiState.update {
                it.copy(
                    isAdvertising = true,
                    activeRole = ConnectionRole.ADVERTISING,
                    statusMessage = "Advertising party as '$name'..."
                )
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "startAdvertising FAILURE: ${e.localizedMessage}", e)
            _uiState.update {
                it.copy(
                    isAdvertising = false,
                    statusMessage = "Failed to start advertising: ${e.localizedMessage}"
                )
            }
        }
    }

    // 2. Stop Advertising
    fun stopAdvertising() {
        Log.d(TAG, "stopAdvertising")
        connectionsClient.stopAdvertising()
        _uiState.update {
            val nextRole = if (it.isDiscovering) ConnectionRole.DISCOVERING else ConnectionRole.IDLE
            it.copy(
                isAdvertising = false,
                activeRole = nextRole,
                statusMessage = if (it.isDiscovering) "Searching for nearby parties..." else "Advertising stopped"
            )
        }
    }

    // 3. Start Discovery (Client Mode)
    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        Log.d(TAG, "startDiscovery")

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "startDiscovery SUCCESS")
            _uiState.update {
                it.copy(
                    isDiscovering = true,
                    activeRole = ConnectionRole.DISCOVERING,
                    statusMessage = "Searching for nearby parties..."
                )
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "startDiscovery FAILURE: ${e.localizedMessage}", e)
            _uiState.update {
                it.copy(
                    isDiscovering = false,
                    statusMessage = "Failed to start discovery: ${e.localizedMessage}"
                )
            }
        }
    }

    // 4. Stop Discovery
    fun stopDiscovery() {
        Log.d(TAG, "stopDiscovery")
        connectionsClient.stopDiscovery()
        _uiState.update {
            val nextRole = if (it.isAdvertising) ConnectionRole.ADVERTISING else ConnectionRole.IDLE
            it.copy(
                isDiscovering = false,
                activeRole = nextRole,
                statusMessage = if (it.isAdvertising) "Advertising..." else "Discovery stopped",
                discoveredHosts = emptyList()
            )
        }
    }

    // 5. Connect to discovered Host
    fun connectToEndpoint(host: AvailableHost) {
        val name = _uiState.value.localEndpointName
        Log.d(TAG, "connectToEndpoint: target=${host.endpointName} ($host.endpointId)")

        updateHostStatus(host.endpointId, HostConnectionStatus.CONNECTING)

        connectionsClient.requestConnection(
            name,
            host.endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {
            Log.d(TAG, "requestConnection SUCCESS to ${host.endpointId}")
            _uiState.update { it.copy(statusMessage = "Requesting connection to ${host.endpointName}...") }
        }.addOnFailureListener { e ->
            Log.e(TAG, "requestConnection FAILURE to ${host.endpointId}: ${e.localizedMessage}", e)
            updateHostStatus(host.endpointId, HostConnectionStatus.DISCONNECTED)
            _uiState.update { it.copy(statusMessage = "Connection request failed: ${e.localizedMessage}") }
        }
    }

    // 6. Disconnect Endpoint
    fun disconnectEndpoint(endpointId: String) {
        Log.d(TAG, "disconnectEndpoint: $endpointId")
        connectionsClient.disconnectFromEndpoint(endpointId)
        handleDisconnectedEndpoint(endpointId)
    }

    // 7. Send String Payload to Endpoint
    fun sendPayload(endpointId: String, message: String) {
        Log.d(TAG, "sendPayload [String] to $endpointId: $message")
        val payload = Payload.fromBytes(message.toByteArray(StandardCharsets.UTF_8))
        connectionsClient.sendPayload(endpointId, payload)
    }

    // 8. Send Video Stream Payload (P2P Transfer)
    fun sendVideoStreamPayload(endpointId: String, uri: Uri): Long? {
        Log.d(TAG, "sendVideoStreamPayload to $endpointId with uri=$uri")
        hostVideoUri = uri
        return try {
            val fileSize = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            } catch (e: Exception) { -1L }

            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val payload = Payload.fromStream(pfd)
                val payloadId = payload.id
                Log.d(TAG, "Payload.fromStream created. payloadId=$payloadId, fileSize=$fileSize, sending to $endpointId...")

                if (fileSize > 0) {
                    expectedFileSizes[payloadId] = fileSize
                    sendPayload(endpointId, "STREAM_META:$payloadId:$fileSize")
                }

                connectionsClient.sendPayload(endpointId, payload)
                _uiState.update {
                    it.copy(
                        isTransferring = true,
                        transferProgress = 0f,
                        transferStatusMessage = "Initiating P2P video stream transfer..."
                    )
                }
                payloadId
            } else {
                Log.e(TAG, "ParcelFileDescriptor is NULL for uri=$uri")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open video stream for uri=$uri: ${e.localizedMessage}", e)
            _uiState.update { it.copy(statusMessage = "Failed to open video stream: ${e.localizedMessage}") }
            null
        }
    }

    // Legacy File Payload (P2P Transfer)
    fun sendVideoFilePayload(endpointId: String, uri: Uri): Long? {
        return sendVideoStreamPayload(endpointId, uri)
    }

    // 9. Stop All Operations
    fun stopAll() {
        Log.d(TAG, "stopAll operations")
        hostVideoUri = null
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _uiState.update {
            NearbyConnectionState(
                hasRequiredPermissions = it.hasRequiredPermissions,
                localEndpointName = it.localEndpointName
            )
        }
    }

    // --- Callbacks ---

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "onEndpointFound: endpointId=$endpointId, name=${info.endpointName}")
            val newHost = AvailableHost(
                endpointId = endpointId,
                endpointName = info.endpointName,
                status = HostConnectionStatus.DISCOVERED
            )
            _uiState.update { current ->
                val updatedHosts = current.discoveredHosts.filterNot { it.endpointId == endpointId } + newHost
                current.copy(discoveredHosts = updatedHosts)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "onEndpointLost: endpointId=$endpointId")
            _uiState.update { current ->
                current.copy(
                    discoveredHosts = current.discoveredHosts.filterNot { it.endpointId == endpointId }
                )
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "onConnectionInitiated: endpointId=$endpointId, name=${info.endpointName}. Accepting connection...")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            updateHostStatus(endpointId, HostConnectionStatus.CONNECTING, info.endpointName)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "onConnectionResult: endpointId=$endpointId, statusCode=${result.status.statusCode}")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    updateHostStatus(endpointId, HostConnectionStatus.CONNECTED)
                    _uiState.update { current ->
                        val connectedHost = current.discoveredHosts.find { it.endpointId == endpointId }
                            ?: AvailableHost(endpointId, "Peer Device", HostConnectionStatus.CONNECTED)
                        val newConnectedList = current.connectedHosts.filterNot { it.endpointId == endpointId } + connectedHost
                        current.copy(
                            connectedHosts = newConnectedList,
                            statusMessage = "Connected to ${connectedHost.endpointName}"
                        )
                    }

                    // Auto-send video stream payload to newly connected peer if host has an active video!
                    hostVideoUri?.let { videoUri ->
                        Log.d(TAG, "Peer connected ($endpointId). Auto-sending host video stream payload: $videoUri")
                        sendVideoStreamPayload(endpointId, videoUri)
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    updateHostStatus(endpointId, HostConnectionStatus.REJECTED)
                    _uiState.update { it.copy(statusMessage = "Connection rejected") }
                }
                else -> {
                    updateHostStatus(endpointId, HostConnectionStatus.DISCONNECTED)
                    _uiState.update { it.copy(statusMessage = "Connection failed with code: ${result.status.statusCode}") }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "onDisconnected: endpointId=$endpointId")
            handleDisconnectedEndpoint(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d(TAG, "onPayloadReceived: endpointId=$endpointId, payloadId=${payload.id}, type=${payload.type}")
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val bytes = payload.asBytes()
                    if (bytes != null && bytes.size >= 8) {
                        try {
                            val buffer = ByteBuffer.wrap(bytes)
                            val magic = buffer.getInt()
                            if (magic == CHNK_MAGIC) {
                                val headerLen = buffer.getInt()
                                if (headerLen > 0 && headerLen <= bytes.size - 8) {
                                    val headerBytes = ByteArray(headerLen)
                                    buffer.get(headerBytes)
                                    val metaJson = String(headerBytes, StandardCharsets.UTF_8)
                                    val chunkMeta = json.decodeFromString<VideoChunkMeta>(metaJson)

                                    val dataSize = bytes.size - 8 - headerLen
                                    val chunkData = ByteArray(dataSize)
                                    buffer.get(chunkData)

                                    handleReceivedChunk(chunkMeta, chunkData)
                                    return
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error unpacking CHNK payload: ${e.localizedMessage}", e)
                        }
                    }
                    if (bytes != null) {
                        val message = String(bytes, StandardCharsets.UTF_8)
                        Log.d(TAG, "Received BYTES payload from $endpointId: $message")
                        if (message.startsWith("STREAM_URL:")) {
                            val streamUrl = message.substringAfter("STREAM_URL:")
                            Log.d(TAG, "Received STREAM_URL payload: $streamUrl. Updating receivedVideoUri for ExoPlayer...")
                            _uiState.update { it.copy(receivedVideoUri = streamUrl) }
                        } else if (message.startsWith("STREAM_META:")) {
                            val parts = message.split(":")
                            if (parts.size >= 3) {
                                val metaPayloadId = parts[1].toLongOrNull()
                                val metaFileSize = parts[2].toLongOrNull()
                                if (metaPayloadId != null && metaFileSize != null && metaFileSize > 0) {
                                    expectedFileSizes[metaPayloadId] = metaFileSize
                                    Log.d(TAG, "Registered STREAM_META for payloadId=$metaPayloadId: fileSize=$metaFileSize")
                                }
                            }
                        } else {
                            _uiState.update { it.copy(lastReceivedPayload = message) }
                        }
                    }
                }
                Payload.Type.STREAM -> {
                    val inputStream = payload.asStream()?.asInputStream()
                    val metaFileSize = expectedFileSizes[payload.id] ?: -1L
                    Log.d(TAG, "Received STREAM payload! payloadId=${payload.id}, metaFileSize=$metaFileSize, inputStreamNotNull=${inputStream != null}")
                    if (inputStream != null) {
                        val tempFile = File(context.cacheDir, "stream_${payload.id}.mp4")
                        incomingFilePayloads[payload.id] = tempFile
                        _uiState.update {
                            it.copy(
                                isTransferring = true,
                                transferProgress = 0f,
                                transferStatusMessage = "Receiving party video stream P2P..."
                            )
                        }
                        kotlin.concurrent.thread(name = "MeetflixStreamReader_${payload.id}") {
                            Log.d(TAG, "Started Google Official Non-Blocking Reader Thread for payload ${payload.id}")
                            try {
                                var totalBytesRead = 0L
                                var initialPlayTriggered = false
                                var lastRead = android.os.SystemClock.elapsedRealtime()
                                val buffer = ByteArray(64 * 1024)
                                val localMediaServer = NearbyRepository.getInstance(context).localMediaServer

                                tempFile.outputStream().use { output ->
                                    while (!Thread.interrupted()) {
                                        if ((android.os.SystemClock.elapsedRealtime() - lastRead) >= 5000L) {
                                            Log.d(TAG, "Stream read timeout (5000ms idle) for payload ${payload.id}")
                                            break
                                        }

                                        val availableBytes = inputStream.available()
                                        if (availableBytes > 0) {
                                            val bytesToRead = availableBytes.coerceAtMost(buffer.size)
                                            val read = inputStream.read(buffer, 0, bytesToRead)
                                            if (read > 0) {
                                                output.write(buffer, 0, read)
                                                totalBytesRead += read
                                                lastRead = android.os.SystemClock.elapsedRealtime()

                                                if (!initialPlayTriggered && totalBytesRead >= 512 * 1024) {
                                                    initialPlayTriggered = true
                                                    localMediaServer.setMediaFile(tempFile, metaFileSize)
                                                    val serverUrl = localMediaServer.getServerUrl("127.0.0.1")
                                                    Log.d(TAG, "Early playback trigger: 512KB buffered! Configured LocalMediaServer with $serverUrl (expectedSize=$metaFileSize)")
                                                    _uiState.update { current ->
                                                        if (current.receivedVideoUri == null) {
                                                            current.copy(
                                                                receivedVideoUri = serverUrl,
                                                                transferStatusMessage = "Streaming video & playing..."
                                                            )
                                                        } else current
                                                    }
                                                }
                                            }
                                        } else {
                                            Thread.sleep(10L)
                                        }
                                    }
                                }
                                Log.d(TAG, "Finished writing STREAM payload ${payload.id}! Total bytes read=$totalBytesRead, fileSize=${tempFile.length()}")
                                localMediaServer.setMediaFile(tempFile, tempFile.length())
                                val finalUrl = localMediaServer.getServerUrl("127.0.0.1")
                                _uiState.update {
                                    it.copy(
                                        isTransferring = false,
                                        transferProgress = 1.0f,
                                        transferStatusMessage = "P2P video stream completed!",
                                        receivedVideoUri = finalUrl
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error copying STREAM payload ${payload.id}: ${e.localizedMessage}", e)
                                _uiState.update {
                                    it.copy(
                                        isTransferring = false,
                                        transferStatusMessage = "P2P video stream transfer failed: ${e.localizedMessage}"
                                    )
                                }
                            }
                        }
                    }
                }
                Payload.Type.FILE -> {
                    @Suppress("DEPRECATION")
                    val file = payload.asFile()?.asJavaFile()
                    Log.d(TAG, "Received FILE payload! payloadId=${payload.id}, file=${file?.absolutePath}")
                    if (file != null) {
                        incomingFilePayloads[payload.id] = file
                        _uiState.update {
                            it.copy(
                                isTransferring = true,
                                transferProgress = 0f,
                                transferStatusMessage = "Receiving party video P2P..."
                            )
                        }
                    }
                }
                else -> {}
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val metaFileSize = expectedFileSizes[update.payloadId] ?: -1L
            val totalBytes = if (update.totalBytes > 0) update.totalBytes else metaFileSize
            val bytesTransferred = update.bytesTransferred
            val progress = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
            Log.d(TAG, "onPayloadTransferUpdate: endpointId=$endpointId, payloadId=${update.payloadId}, status=${update.status}, transferred=$bytesTransferred/$totalBytes ($progress)")

            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    val statusMsg = when {
                        totalBytes > 0 -> "Streaming video: ${(progress * 100).toInt()}% (${formatFileSize(bytesTransferred)} / ${formatFileSize(totalBytes)})"
                        bytesTransferred > 0 -> "Streaming video: ${formatFileSize(bytesTransferred)} transferred"
                        else -> "Streaming video payload..."
                    }
                    _uiState.update {
                        it.copy(
                            isTransferring = true,
                            transferProgress = progress,
                            transferStatusMessage = statusMsg
                        )
                    }
                }
                PayloadTransferUpdate.Status.SUCCESS -> {
                    val receivedFile = incomingFilePayloads[update.payloadId]
                    val receivedUri = receivedFile?.let { Uri.fromFile(it).toString() }
                    Log.d(TAG, "PayloadTransferUpdate SUCCESS! payloadId=${update.payloadId}, receivedUri=$receivedUri")

                    _uiState.update {
                        it.copy(
                            isTransferring = false,
                            transferProgress = 1.0f,
                            transferStatusMessage = "P2P video stream transfer completed!",
                            receivedVideoUri = receivedUri ?: it.receivedVideoUri
                        )
                    }
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    Log.e(TAG, "PayloadTransferUpdate FAILURE! payloadId=${update.payloadId}")
                    _uiState.update {
                        it.copy(
                            isTransferring = false,
                            transferStatusMessage = "P2P video transfer failed."
                        )
                    }
                }
            }
        }
    }

    private fun updateHostStatus(endpointId: String, status: HostConnectionStatus, fallbackName: String = "Unknown Host") {
        _uiState.update { current ->
            val updatedList = current.discoveredHosts.map { host ->
                if (host.endpointId == endpointId) host.copy(status = status) else host
            }
            val listWithNewHost = if (updatedList.none { it.endpointId == endpointId }) {
                updatedList + AvailableHost(endpointId, fallbackName, status)
            } else updatedList
            current.copy(discoveredHosts = listWithNewHost)
        }
    }

    private fun handleDisconnectedEndpoint(endpointId: String) {
        _uiState.update { current ->
            val updatedDiscovered = current.discoveredHosts.map { host ->
                if (host.endpointId == endpointId) host.copy(status = HostConnectionStatus.DISCONNECTED) else host
            }
            val updatedConnected = current.connectedHosts.filterNot { it.endpointId == endpointId }
            current.copy(
                discoveredHosts = updatedDiscovered,
                connectedHosts = updatedConnected,
                statusMessage = "Disconnected"
            )
        }
    }
}
