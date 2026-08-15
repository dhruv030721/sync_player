package com.meetflix.player.domain.models

import com.meetflix.player.domain.enums.PlayerSyncActionType
import kotlinx.serialization.Serializable

/** Design Pattern: Data Transfer Object (DTO) - Encapsulates sync payload over NearbyConnections */
@Serializable
data class PlayerSyncPayload(
    val actionType: PlayerSyncActionType,
    val positionMs: Long,
    val playbackSpeed: Float = 1.0f,
    val mediaUri: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)
