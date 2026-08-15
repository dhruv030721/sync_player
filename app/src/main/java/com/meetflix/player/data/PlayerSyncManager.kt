package com.meetflix.player.data

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
 * Encapsulates Party Mode synchronization logic over Nearby Connections.
 * Computes network latency drift and triggers aligned playback commands on ExoPlayerManager.
 */
class PlayerSyncManager(
    private val exoPlayerManager: ExoPlayerManager,
    private val nearbyConnectionsManager: NearbyConnectionsManager
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        // Observe incoming payloads from NearbyConnectionsManager
        nearbyConnectionsManager.uiState
            .onEach { state ->
                state.lastReceivedPayload?.let { payloadString ->
                    handleIncomingPayload(payloadString)
                }
            }
            .launchIn(scope)
    }

    fun broadcastPlaybackAction(
        actionType: PlayerSyncActionType,
        positionMs: Long,
        mediaUri: String? = null,
        playbackSpeed: Float = 1.0f
    ) {
        val payload = PlayerSyncPayload(
            actionType = actionType,
            positionMs = positionMs,
            playbackSpeed = playbackSpeed,
            mediaUri = mediaUri,
            timestampMs = System.currentTimeMillis()
        )
        val payloadString = json.encodeToString(payload)

        // Broadcast to all connected host/guest endpoints
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
                    payload.mediaUri?.let { uri ->
                        exoPlayerManager.setMediaUri(uri)
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