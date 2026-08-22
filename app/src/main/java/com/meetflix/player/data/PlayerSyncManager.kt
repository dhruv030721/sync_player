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
        private const val CONSUMER_TARGET_DELAY_MS = 1000L
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var lastReceivedVideoUri: String? = null

    private var chunkSchedulerJob: Job? = null
    private val sentChunkSet = ConcurrentHashMap.newKeySet<Int>()

    init {
        // Observe incoming string JSON payloads and connection role from NearbyConnectionsManager
        nearbyConnectionsManager.uiState
            .onEach { state ->
                // Update seeking permission for consumers (guests in a Watch Party cannot seek)
                val isPartyConsumer = state.connectedHosts.isNotEmpty() && !state.isAdvertising
                exoPlayerManager.setCanSeek(!isPartyConsumer)

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
        val nearbyState = nearbyConnectionsManager.uiState.value
        // Only host devices broadcast sync events to guests
        if (nearbyState.connectedHosts.isNotEmpty() && !nearbyState.isAdvertising) {
            Log.d(TAG, "Consumer device skipping broadcast of action: $actionType")
            return
        }

        if (actionType == PlayerSyncActionType.SEEK) {
            sentChunkSet.clear()
        }

        // If host sets media, trigger P2P stream transfer to all connected endpoints
        if (actionType == PlayerSyncActionType.SET_MEDIA && mediaUri != null) {
            val parsedUri = Uri.parse(mediaUri)
            nearbyState.connectedHosts.forEach { host ->
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
        nearbyState.connectedHosts.forEach { host ->
            nearbyConnectionsManager.sendPayload(host.endpointId, payloadString)
        }
    }

    /**
     * Design Pattern: Strategy - Smart Consumer Sync & Speed Adjustment Algorithm
     * Evaluates position drift relative to a 1s consumer target delay and adjusts playback speed
     * imperceptibly to maintain sync without pausing/seeking or jarring speed jumps.
     */
    fun handleIncomingPayload(payloadString: String) {
        try {
            // Host device playback is the master authority and must never be interrupted by consumer payloads
            if (nearbyConnectionsManager.uiState.value.isAdvertising) {
                Log.d(TAG, "Host device ignoring incoming sync payload: $payloadString")
                return
            }

            val payload = json.decodeFromString<PlayerSyncPayload>(payloadString)
            val currentClientTime = System.currentTimeMillis()
            val networkLatency = (currentClientTime - payload.timestampMs).coerceAtLeast(0L)
            val adjustedHostPosition = payload.positionMs + networkLatency
            val targetConsumerPosition = (adjustedHostPosition - CONSUMER_TARGET_DELAY_MS).coerceAtLeast(0L)

            when (payload.actionType) {
                PlayerSyncActionType.PLAY -> {
                    val currentPos = exoPlayerManager.playerState.value.currentPositionMs
                    if (kotlin.math.abs(currentPos - targetConsumerPosition) > 1500L) {
                        exoPlayerManager.seekTo(targetConsumerPosition)
                    }
                    exoPlayerManager.setPlaybackSpeed(1.0f)
                    exoPlayerManager.play()
                }
                PlayerSyncActionType.PAUSE -> {
                    val pauseTarget = (payload.positionMs - CONSUMER_TARGET_DELAY_MS).coerceAtLeast(0L)
                    exoPlayerManager.seekTo(pauseTarget)
                    exoPlayerManager.setPlaybackSpeed(1.0f)
                    exoPlayerManager.pause()
                }
                PlayerSyncActionType.SEEK -> {
                    val seekTarget = (payload.positionMs - CONSUMER_TARGET_DELAY_MS).coerceAtLeast(0L)
                    exoPlayerManager.seekTo(seekTarget)
                    exoPlayerManager.setPlaybackSpeed(1.0f)
                }
                PlayerSyncActionType.SET_MEDIA -> {
                    payload.videoTitle?.let { title ->
                        // Title updated on guest UI
                    }
                }
                PlayerSyncActionType.HEARTBEAT -> {
                    val currentPos = exoPlayerManager.playerState.value.currentPositionMs
                    val isPlaying = exoPlayerManager.playerState.value.isPlaying
                    if (!isPlaying) return

                    val driftMs = currentPos - targetConsumerPosition

                    // Hard seek fallback for extreme desync (> 4s)
                    if (kotlin.math.abs(driftMs) > 4000L) {
                        Log.d(TAG, "Extreme sync drift (${driftMs}ms). Performing seek to target position: $targetConsumerPosition")
                        exoPlayerManager.seekTo(targetConsumerPosition)
                        exoPlayerManager.setPlaybackSpeed(1.0f)
                    } else {
                        // Smart imperceptible micro-playback speed adjustment
                        val targetSpeed = when {
                            driftMs < -150L -> {
                                // Lagging behind 1s delay: slightly increase speed between 1.01x and 1.12x
                                1.0f + ((-driftMs - 150L) * 0.00004f).coerceAtMost(0.12f)
                            }
                            driftMs > 150L -> {
                                // Ahead of 1s delay: slightly decrease speed between 0.92x and 0.99x
                                1.0f - ((driftMs - 150L) * 0.00004f).coerceAtMost(0.08f)
                            }
                            else -> 1.0f
                        }

                        val currentSpeed = exoPlayerManager.playerState.value.playbackSpeed
                        val speedDelta = targetSpeed - currentSpeed
                        // Ramp speed in smooth micro-steps (max ±0.015 per heartbeat iteration)
                        val newSpeed = (currentSpeed + speedDelta.coerceIn(-0.015f, 0.015f))
                        val roundedSpeed = (kotlin.math.round(newSpeed * 100) / 100.0f)

                        if (kotlin.math.abs(roundedSpeed - currentSpeed) >= 0.01f) {
                            Log.d(TAG, "Adjusting consumer playback speed smoothly: ${currentSpeed}x -> ${roundedSpeed}x (drift: ${driftMs}ms)")
                            exoPlayerManager.setPlaybackSpeed(roundedSpeed)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}