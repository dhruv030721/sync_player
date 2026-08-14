package com.meetflix.connections.domain.models

enum class HostConnectionStatus {
    DISCOVERED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    REJECTED
}

data class AvailableHost(
    val endpointId: String,
    val endpointName: String,
    val status: HostConnectionStatus = HostConnectionStatus.DISCOVERED
)