package com.meetflix.player.domain.models

import com.meetflix.player.domain.enums.PlayerSyncActionType
import kotlinx.serialization.Serializable

/**
 * Design Pattern: Data Transfer Object (DTO)
 * Encapsulates playback synchronization action, position, and P2P video metadata over Nearby Connections.
 */
@Serializable
data class PlayerSyncPayload(
    val actionType: PlayerSyncActionType,
    val positionMs: Long,
    val playbackSpeed: Float = 1.0f,
    val mediaUri: String? = null,
    val videoTitle: String? = null,
    val payloadId: Long? = null,
    val timestampMs: Long = System.currentTimeMillis()
)
