package com.meetflix.player.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.meetflix.connections.data.NearbyConnectionsManager
import com.meetflix.connections.domain.models.VideoChunkMeta
import com.meetflix.player.domain.enums.PlayerSyncActionType
import com.meetflix.player.domain.models.PlayerSyncPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Design Pattern: Strategy & Observer
 * Encapsulates Party Mode playback synchronization logic over Nearby Connections.
 * Initiates P2P video file payload transfers, computes network latency drift, and triggers aligned ExoPlayer commands.
 */
class PlayerSyncManager(
    private val exoPlayerManager: ExoPlayerManager,
    private val nearbyConnectionsManager: NearbyConnectionsManager
) {
    companion object {
        private const val TAG = "MeetflixStream"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var lastReceivedVideoUri: String? = null

    private var chunkSchedulerJob: Job? = null
    private val sentChunkSet = ConcurrentHashMap.newKeySet<Int>()

    init {
        // Observe incoming string JSON payloads from NearbyConnectionsManager
        nearbyConnectionsManager.uiState
            .onEach { state ->
                state.lastReceivedPayload?.let { payloadString ->
                    handleIncomingPayload(payloadString)
                }

                // Observe completed P2P video payload transfers on guest device
                state.receivedVideoUri?.let { receivedUri ->
                    if (receivedUri != lastReceivedVideoUri) {
                        Log.d(TAG, "PlayerSyncManager observed new receivedVideoUri: $receivedUri. Loading into ExoPlayer...")
                        lastReceivedVideoUri = receivedUri
                        exoPlayerManager.setMediaUri(receivedUri, "Party Video")
                    }
                }
            }
            .launchIn(scope)
    }

    fun startChunkStreamingScheduler(context: Context, videoUri: Uri) {
        chunkSchedulerJob?.cancel()
        sentChunkSet.clear()

        chunkSchedulerJob = scope.launch(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(videoUri, "r") ?: return@launch
                val fileSize = pfd.statSize
                val chunkSize = 64 * 1024 // 64KB chunks
                val totalChunks = kotlin.math.ceil(fileSize.toDouble() / chunkSize.toDouble()).toInt().coerceAtLeast(1)
                val streamId = "stream_" + (videoUri.hashCode() and 0x7FFFFFFF)
                Log.d(TAG, "Started Chunk Streaming Scheduler for $videoUri: fileSize=$fileSize, totalChunks=$totalChunks, streamId=$streamId")

                val fileChannel = java.io.FileInputStream(pfd.fileDescriptor).channel

                while (isActive) {
                    val connectedHosts = nearbyConnectionsManager.uiState.value.connectedHosts
                    if (connectedHosts.isNotEmpty()) {
                        val playerState = exoPlayerManager.playerState.value
                        val currentPosMs = playerState.currentPositionMs
                        val durationMs = playerState.durationMs

                        val progressRatio = if (durationMs > 0) (currentPosMs.toDouble() / durationMs.toDouble()).coerceIn(0.0, 1.0) else 0.0
                        val targetByteOffset = (progressRatio * fileSize).toLong()
                        val currentChunkIndex = (targetByteOffset / chunkSize).toInt().coerceIn(0, totalChunks - 1)

                        // 1. Dispatch Header & Footer priority chunks so client ExoPlayer receives container header + moov atom
                        val priorityChunks = listOf(0, 1, totalChunks - 2, totalChunks - 1).filter { it in 0 until totalChunks }.distinct()
                        for (priorityIdx in priorityChunks) {
                            if (!isActive) break
                            if (sentChunkSet.contains(priorityIdx)) continue

                            val startByte = priorityIdx.toLong() * chunkSize
                            val lengthToRead = ((fileSize - startByte).coerceAtMost(chunkSize.toLong())).toInt()
                            if (lengthToRead <= 0) continue

                            val buffer = java.nio.ByteBuffer.allocate(lengthToRead)
                            fileChannel.position(startByte)
                            fileChannel.read(buffer)
                            val chunkBytes = buffer.array()

                            val chunkMeta = VideoChunkMeta(
                                chunkIndex = priorityIdx,
                                totalChunks = totalChunks,
                                startByte = startByte,
                                chunkSize = lengthToRead,
                                totalFileSize = fileSize,
                                streamId = streamId
                            )
                            sentChunkSet.add(priorityIdx)
                            connectedHosts.forEach { host ->
                                nearbyConnectionsManager.sendChunkPayload(host.endpointId, chunkMeta, chunkBytes)
                            }
                            delay(15L)
                        }

                        // 2. Dispatch sliding window chunks around host's active playback position
                        val windowStart = currentChunkIndex
                        val windowEnd = (currentChunkIndex + 25).coerceAtMost(totalChunks - 1)

                        for (chunkIdx in windowStart..windowEnd) {
                            if (!isActive) break
                            if (sentChunkSet.contains(chunkIdx)) continue

                            val startByte = chunkIdx.toLong() * chunkSize
                            val lengthToRead = ((fileSize - startByte).coerceAtMost(chunkSize.toLong())).toInt()
                            if (lengthToRead <= 0) continue

                            val buffer = java.nio.ByteBuffer.allocate(lengthToRead)
                            fileChannel.position(startByte)
                            fileChannel.read(buffer)
                            val chunkBytes = buffer.array()

                            val chunkMeta = VideoChunkMeta(
                                chunkIndex = chunkIdx,
                                totalChunks = totalChunks,
                                startByte = startByte,
                                chunkSize = lengthToRead,
                                totalFileSize = fileSize,
                                streamId = streamId
                            )

                            sentChunkSet.add(chunkIdx)

                            connectedHosts.forEach { host ->
                                nearbyConnectionsManager.sendChunkPayload(host.endpointId, chunkMeta, chunkBytes)
                            }
                            delay(20L)
                        }
                    }
                    delay(300L)
                }
                fileChannel.close()
                pfd.close()
            } catch (e: Exception) {
                Log.e(TAG, "Chunk Streaming Scheduler error: ${e.localizedMessage}", e)
            }
        }
    }

    fun broadcastPlaybackAction(
        actionType: PlayerSyncActionType,
        positionMs: Long,
        mediaUri: String? = null,
        videoTitle: String? = null,
        playbackSpeed: Float = 1.0f
    ) {
        if (actionType == PlayerSyncActionType.SEEK) {
            sentChunkSet.clear()
        }

        // If host sets media, trigger P2P stream transfer to all connected endpoints
        if (actionType == PlayerSyncActionType.SET_MEDIA && mediaUri != null) {
            val parsedUri = Uri.parse(mediaUri)
            nearbyConnectionsManager.uiState.value.connectedHosts.forEach { host ->
                nearbyConnectionsManager.sendVideoStreamPayload(host.endpointId, parsedUri)
            }
        }

        val payload = PlayerSyncPayload(
            actionType = actionType,
            positionMs = positionMs,
            playbackSpeed = playbackSpeed,
            mediaUri = mediaUri,
            videoTitle = videoTitle,
            timestampMs = System.currentTimeMillis()
        )
        val payloadString = json.encodeToString(payload)

        // Broadcast sync JSON payload to all connected host/guest endpoints
        nearbyConnectionsManager.uiState.value.connectedHosts.forEach { host ->
            nearbyConnectionsManager.sendPayload(host.endpointId, payloadString)
        }
    }

    fun handleIncomingPayload(payloadString: String) {
        try {
            val payload = json.decodeFromString<PlayerSyncPayload>(payloadString)
            val currentClientTime = System.currentTimeMillis()
            val networkLatency = (currentClientTime - payload.timestampMs).coerceAtLeast(0L)
            val adjustedPosition = payload.positionMs + networkLatency

            when (payload.actionType) {
                PlayerSyncActionType.PLAY -> {
                    val currentPos = exoPlayerManager.playerState.value.currentPositionMs
                    if (kotlin.math.abs(currentPos - adjustedPosition) > 1500L) {
                        exoPlayerManager.seekTo(adjustedPosition)
                    }
                    exoPlayerManager.play()
                }
                PlayerSyncActionType.PAUSE -> {
                    exoPlayerManager.seekTo(payload.positionMs)
                    exoPlayerManager.pause()
                }
                PlayerSyncActionType.SEEK -> {
                    exoPlayerManager.seekTo(payload.positionMs)
                }
                PlayerSyncActionType.SET_MEDIA -> {
                    // Set video title metadata
                    payload.videoTitle?.let { title ->
                        // Title updated on guest UI
                    }
                }
                PlayerSyncActionType.HEARTBEAT -> {
                    val currentPos = exoPlayerManager.playerState.value.currentPositionMs
                    if (exoPlayerManager.playerState.value.isPlaying &&
                        kotlin.math.abs(currentPos - adjustedPosition) > 1500L
                    ) {
                        exoPlayerManager.seekTo(adjustedPosition)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}