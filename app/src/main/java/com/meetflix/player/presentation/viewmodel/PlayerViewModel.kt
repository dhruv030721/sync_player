package com.meetflix.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

/** Design Pattern: Factory - Factory for instantiating PlayerViewModel with ExoPlayerManager and PlayerSyncManager */
class PlayerViewModelFactory(
    private val exoPlayerManager: ExoPlayerManager,
    private val playerSyncManager: PlayerSyncManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlayerViewModel(exoPlayerManager, playerSyncManager) as T
    }
}

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
        // Observe play state to handle 500ms controls auto-hide and heartbeat loop
        viewModelScope.launch {
            uiState.collect { state ->
                if (state.isPlaying) {
                    if (heartbeatJob == null) {
                        startHeartbeatLoop()
                    }
                    if (state.isControlsVisible && (controlsHideJob == null || !controlsHideJob!!.isActive)) {
                        resetControlsTimer()
                    }
                } else {
                    stopHeartbeatLoop()
                    controlsHideJob?.cancel()
                    exoPlayerManager.setControlsVisibility(true)
                }
            }
        }
    }

    fun onAction(action: PlaybackAction) {
        when (action) {
            PlaybackAction.Play -> {
                exoPlayerManager.play()
                broadcastSync(PlayerSyncActionType.PLAY)
                resetControlsTimer()
            }
            PlaybackAction.Pause -> {
                exoPlayerManager.pause()
                broadcastSync(PlayerSyncActionType.PAUSE)
                controlsHideJob?.cancel()
                exoPlayerManager.setControlsVisibility(true)
            }
            is PlaybackAction.Seek -> {
                if (!uiState.value.canSeek) return
                exoPlayerManager.seekTo(action.positionMs)
                broadcastSync(PlayerSyncActionType.SEEK)
                resetControlsTimer()
            }
            is PlaybackAction.SetSpeed -> {
                exoPlayerManager.setPlaybackSpeed(action.speed)
                resetControlsTimer()
            }
            is PlaybackAction.LoadMedia -> {
                exoPlayerManager.setMediaUri(action.uri, action.title)
                broadcastSync(PlayerSyncActionType.SET_MEDIA, mediaUri = action.uri)
                resetControlsTimer()
            }
            PlaybackAction.ToggleControls -> {
                val currentVisibility = uiState.value.isControlsVisible
                val nextVisibility = !currentVisibility
                exoPlayerManager.setControlsVisibility(nextVisibility)
                if (nextVisibility) {
                    resetControlsTimer()
                } else {
                    controlsHideJob?.cancel()
                }
            }
            PlaybackAction.ToggleFullScreen -> {
                val currentFullScreen = uiState.value.isFullScreen
                exoPlayerManager.setFullScreen(!currentFullScreen)
                resetControlsTimer()
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
        if (uiState.value.isPlaying) {
            controlsHideJob = viewModelScope.launch {
                delay(500L) // Hide player controls and seekbar after 500ms of playback
                exoPlayerManager.setControlsVisibility(false)
            }
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