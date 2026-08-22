package com.meetflix.player.domain.enums

enum class PlayerSyncActionType(val value: String) {
    PLAY("PLAY"),
    PAUSE("PAUSE"),
    SEEK("SEEK"),
    HEARTBEAT("HEARTBEAT"),
    SET_MEDIA("SET_MEDIA")
}