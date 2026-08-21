package com.meetflix.party.domain.models

import com.meetflix.connections.domain.models.AvailableHost

/**
 * Design Pattern: Value Object / Immutable State DTO
 * Encapsulates full immutable UI state for Party Mode watch room.
 */
data class PartyModeState(
    val mode: PartyScreenMode = PartyScreenMode.IDLE,
    val hasRequiredPermissions: Boolean = false,
    val localDeviceName: String = "",
    val activePartyTitle: String? = null,
    val selectedMediaUri: String? = null,
    val discoveredParties: List<AvailableHost> = emptyList(),
    val connectedMembers: List<AvailableHost> = emptyList(),
    val isPreparingMedia: Boolean = false,
    val mediaPreparationMessage: String? = null,
    val isTransferring: Boolean = false,
    val transferProgress: Float = 0f,
    val transferStatusMessage: String? = null,
    val statusMessage: String = "Ready to start or join a party"
)
