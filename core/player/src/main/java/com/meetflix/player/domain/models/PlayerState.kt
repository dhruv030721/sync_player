package com.meetflix.player.domain.models

/** Design Pattern: Domain Model / State - Represents immutable player status for UDF */
data class PlayerState(
    val mediaUri: String? = null,
    val videoTitle: String = "Untitled Video",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isSyncing: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isControlsVisible: Boolean = true,
    val isFullScreen: Boolean = false,
    val canSeek: Boolean = true
)
