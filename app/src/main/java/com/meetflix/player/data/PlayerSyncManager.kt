package com.meetflix.player.data

import android.net.Uri
import com.meetflix.connections.data.NearbyConnectionsManager
import com.meetflix.player.domain.enums.PlayerSyncActionType
import com.meetflix.player.domain.models.PlayerSyncPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Design Pattern: Strategy & Observer
 * Encapsulates Party Mode playback synchronization logic over Nearby Connections.
 * Initiates P2P video file payload transfers, computes network latency drift, and triggers aligned ExoPlayer commands.
 */
class PlayerSyncManager(
    private val exoPlayerManager: ExoPlayerManager,
    private val nearbyConnectionsManager: NearbyConnectionsManager
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var lastReceivedVideoUri: String? = null

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
                        lastReceivedVideoUri = receivedUri
                        exoPlayerManager.setMediaUri(receivedUri, "Party Video")
                    }
                }
            }
            .launchIn(scope)
    }

    fun broadcastPlaybackAction(
        actionType: PlayerSyncActionType,
        positionMs: Long,
        mediaUri: String? = null,
        videoTitle: String? = null,
        playbackSpeed: Float = 1.0f
    ) {
        // If host sets media, trigger P2P file transfer to all connected endpoints
        if (actionType == PlayerSyncActionType.SET_MEDIA && mediaUri != null) {
            val parsedUri = Uri.parse(mediaUri)
            nearbyConnectionsManager.uiState.value.connectedHosts.forEach { host ->
                nearbyConnectionsManager.sendVideoFilePayload(host.endpointId, parsedUri)
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