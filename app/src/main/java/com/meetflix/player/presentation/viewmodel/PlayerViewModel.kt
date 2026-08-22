package com.meetflix.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetflix.player.data.ExoPlayerManager
import com.meetflix.player.data.PlayerSyncManager
import com.meetflix.player.domain.enums.PlayerSyncActionType
import com.meetflix.player.domain.models.PlaybackAction
import com.meetflix.player.domain.models.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Design Pattern: Observer / StateFlow (MVI)
 * Exposes immutable PlayerState to Compose UI, handles user intents via PlaybackAction,
 * manages overlay auto-hide timers and periodic heartbeat synchronization.
 */
class PlayerViewModel(
    val exoPlayerManager: ExoPlayerManager,
    private val playerSyncManager: PlayerSyncManager
) : ViewModel() {

    val uiState: StateFlow<PlayerState> = exoPlayerManager.playerState

    private var controlsHideJob: Job? = null
    private var heartbeatJob: Job? = null

    init {
        // Start auto-hide timer on launch
        resetControlsTimer()

        // Observe play state to start/stop periodic heartbeat broadcast
        viewModelScope.launch {
            uiState.collect { state ->
                if (state.isPlaying && heartbeatJob == null) {
                    startHeartbeatLoop()
                } else if (!state.isPlaying) {
                    stopHeartbeatLoop()
                }
            }
        }
    }

    fun onAction(action: PlaybackAction) {
        resetControlsTimer()

        when (action) {
            PlaybackAction.Play -> {
                exoPlayerManager.play()
                broadcastSync(PlayerSyncActionType.PLAY)
            }
            PlaybackAction.Pause -> {
                exoPlayerManager.pause()
                broadcastSync(PlayerSyncActionType.PAUSE)
            }
            is PlaybackAction.Seek -> {
                exoPlayerManager.seekTo(action.positionMs)
                broadcastSync(PlayerSyncActionType.SEEK)
            }
            is PlaybackAction.SetSpeed -> {
                exoPlayerManager.setPlaybackSpeed(action.speed)
            }
            is PlaybackAction.LoadMedia -> {
                exoPlayerManager.setMediaUri(action.uri, action.title)
                broadcastSync(PlayerSyncActionType.SET_MEDIA, mediaUri = action.uri)
            }
            PlaybackAction.ToggleControls -> {
                val currentVisibility = uiState.value.isControlsVisible
                if (!currentVisibility) {
                    resetControlsTimer()
                }
            }
            PlaybackAction.ToggleFullScreen -> {
                // Toggled by UI layout logic
            }
        }
    }

    private fun broadcastSync(actionType: PlayerSyncActionType, mediaUri: String? = null) {
        val currentState = uiState.value
        playerSyncManager.broadcastPlaybackAction(
            actionType = actionType,
            positionMs = currentState.currentPositionMs,
            mediaUri = mediaUri ?: currentState.mediaUri,
            playbackSpeed = currentState.playbackSpeed
        )
    }

    private fun resetControlsTimer() {
        controlsHideJob?.cancel()
        controlsHideJob = viewModelScope.launch {
            delay(3500L) // Auto-hide controls after 3.5 seconds
        }
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                delay(3000L) // Broadcast heartbeat every 3s
                if (uiState.value.isPlaying) {
                    broadcastSync(PlayerSyncActionType.HEARTBEAT)
                }
            }
        }
    }

    private fun stopHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    override fun onCleared() {
        super.onCleared()
        controlsHideJob?.cancel()
        heartbeatJob?.cancel()
        exoPlayerManager.release()
    }
}