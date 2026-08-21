package com.meetflix.player.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.meetflix.player.domain.models.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

import android.util.Log

/**
 * Design Pattern: Manager / Wrapper
 * Encapsulates Media3 ExoPlayer instance, manages player listener lifecycle,
 * position polling coroutines, and exposes reactive StateFlow.
 */
class ExoPlayerManager(context: Context) {

    companion object {
        private const val TAG = "MeetflixStream"
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var positionUpdateJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "ExoPlayer onIsPlayingChanged: $isPlaying")
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startPositionPolling()
                } else {
                    stopPositionPolling()
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateString = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($playbackState)"
                }
                Log.d(TAG, "ExoPlayer onPlaybackStateChanged: $stateString")
                if (playbackState == Player.STATE_READY) {
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                    _playerState.update { it.copy(durationMs = duration) }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "ExoPlayer onPlayerError: ${error.message}", error)
            }
        })
    }

    fun setMediaUri(uri: String, title: String = "", autoPlay: Boolean = true) {
        Log.d(TAG, "ExoPlayerManager setMediaUri: $uri (title='$title', autoPlay=$autoPlay)")
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.playWhenReady = autoPlay
        exoPlayer.prepare()
        if (autoPlay) {
            exoPlayer.play()
        }
        _playerState.update {
            it.copy(
                mediaUri = uri,
                videoTitle = if (title.isNotBlank()) title else "Untitled Video"
            )
        }
    }

    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playerState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    private fun startPositionPolling() {
        positionUpdateJob?.cancel()
        positionUpdateJob = mainScope.launch {
            while (isActive) {
                val current = exoPlayer.currentPosition.coerceAtLeast(0L)
                val buffered = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                _playerState.update {
                    it.copy(currentPositionMs = current, bufferedPositionMs = buffered)
                }
                delay(250L.milliseconds) // Update 4 times per second for smooth seekbar
            }
        }
    }

    private fun stopPositionPolling() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun release() {
        stopPositionPolling()
        exoPlayer.release()
    }

}