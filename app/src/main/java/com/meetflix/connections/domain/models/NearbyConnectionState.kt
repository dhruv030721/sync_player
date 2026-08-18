package com.meetflix.connections.domain.models

/**
 * Design Pattern: State / Value Object
 * Enum defining active Nearby Connection modes.
 */
enum class ConnectionRole {
    IDLE,
    ADVERTISING,
    DISCOVERING
}

/**
 * Design Pattern: Immutable State DTO
 * Holds full immutable state for Nearby P2P connection lifecycle and payload transfers.
 */
data class NearbyConnectionState(
    val isAdvertising: Boolean = false,
    val isDiscovering: Boolean = false,
    val localEndpointName: String = "",
    val activeRole: ConnectionRole = ConnectionRole.IDLE,
    val discoveredHosts: List<AvailableHost> = emptyList(),
    val connectedHosts: List<AvailableHost> = emptyList(),
    val lastReceivedPayload: String? = null,
    val statusMessage: String? = null,
    val hasRequiredPermissions: Boolean = false,
    val isTransferring: Boolean = false,
    val transferProgress: Float = 0f,
    val transferStatusMessage: String? = null,
    val receivedVideoUri: String? = null
)
