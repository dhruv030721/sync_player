package com.meetflix.player.domain.models

/** Design Pattern: Command / Sealed Action Hierarchy - Encapsulates player intents */
sealed class PlaybackAction {
    object Play : PlaybackAction()
    object Pause : PlaybackAction()
    data class Seek(val positionMs: Long) : PlaybackAction()
    data class SetSpeed(val speed: Float) : PlaybackAction()
    data class LoadMedia(val uri: String, val title: String = "") : PlaybackAction()
    object ToggleControls : PlaybackAction()
    object ToggleFullScreen : PlaybackAction()
}
