package com.meetflix.connections.domain.models

enum class ConnectionRole {
    IDLE,
    ADVERTISING,
    DISCOVERING
}

data class NearbyConnectionState(
    val isAdvertising: Boolean = false,
    val isDiscovering: Boolean = false,
    val localEndpointName: String = "",
    val activeRole: ConnectionRole = ConnectionRole.IDLE,
    val discoveredHosts: List<AvailableHost> = emptyList(),
    val connectedHosts: List<AvailableHost> = emptyList(),
    val lastReceivedPayload: String? = null,
    val statusMessage: String? = null,
    val hasRequiredPermissions: Boolean = false
)
