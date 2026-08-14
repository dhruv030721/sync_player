package com.meetflix.connections.data

import android.content.Context
import android.os.Build
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.meetflix.connections.domain.models.AvailableHost
import com.meetflix.connections.domain.models.ConnectionRole
import com.meetflix.connections.domain.models.HostConnectionStatus
import com.meetflix.connections.domain.models.NearbyConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.charset.StandardCharsets

class NearbyConnectionsManager(private val context: Context) {

    companion object {
        const val SERVICE_ID = "com.meetflix.RFC01_SERVICE"
        val STRATEGY = Strategy.P2P_STAR
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val _uiState = MutableStateFlow(NearbyConnectionState(localEndpointName = getDeviceName()))
    val uiState: StateFlow<NearbyConnectionState> = _uiState.asStateFlow()

    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }

    fun updatePermissionsState(hasPermissions: Boolean) {
        _uiState.update { it.copy(hasRequiredPermissions = hasPermissions) }
    }

    fun setLocalName(name: String) {
        if (name.isNotBlank()) {
            _uiState.update { it.copy(localEndpointName = name.trim()) }
        }
    }

    // 1. Start Advertising (Host Mode)
    fun startAdvertising(customName: String? = null) {
        val name = customName ?: _uiState.value.localEndpointName
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()

        connectionsClient.startAdvertising(
            name,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            _uiState.update {
                it.copy(
                    isAdvertising = true,
                    activeRole = ConnectionRole.ADVERTISING,
                    statusMessage = "Advertising as '$name'..."
                )
            }
        }.addOnFailureListener { e ->
            _uiState.update {
                it.copy(
                    isAdvertising = false,
                    statusMessage = "Failed to start advertising: ${e.localizedMessage}"
                )
            }
        }
    }

    // 2. Stop Advertising
    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        _uiState.update {
            val nextRole = if (it.isDiscovering) ConnectionRole.DISCOVERING else ConnectionRole.IDLE
            it.copy(
                isAdvertising = false,
                activeRole = nextRole,
                statusMessage = if (it.isDiscovering) "Searching for nearby devices..." else "Advertising stopped"
            )
        }
    }

    // 3. Start Discovery (Client Mode)
    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            _uiState.update {
                it.copy(
                    isDiscovering = true,
                    activeRole = ConnectionRole.DISCOVERING,
                    statusMessage = "Searching for nearby devices..."
                )
            }
        }.addOnFailureListener { e ->
            _uiState.update {
                it.copy(
                    isDiscovering = false,
                    statusMessage = "Failed to start discovery: ${e.localizedMessage}"
                )
            }
        }
    }

    // 4. Stop Discovery
    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _uiState.update {
            val nextRole = if (it.isAdvertising) ConnectionRole.ADVERTISING else ConnectionRole.IDLE
            it.copy(
                isDiscovering = false,
                activeRole = nextRole,
                statusMessage = if (it.isAdvertising) "Advertising..." else "Discovery stopped"
            )
        }
    }

    // 5. Connect to discovered Host
    fun connectToEndpoint(host: AvailableHost) {
        val name = _uiState.value.localEndpointName

        // Update host status to CONNECTING
        updateHostStatus(host.endpointId, HostConnectionStatus.CONNECTING)

        connectionsClient.requestConnection(
            name,
            host.endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {
            _uiState.update { it.copy(statusMessage = "Requesting connection to ${host.endpointName}...") }
        }.addOnFailureListener { e ->
            updateHostStatus(host.endpointId, HostConnectionStatus.DISCONNECTED)
            _uiState.update { it.copy(statusMessage = "Connection request failed: ${e.localizedMessage}") }
        }
    }

    // 6. Disconnect Endpoint
    fun disconnectEndpoint(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        handleDisconnectedEndpoint(endpointId)
    }

    // 7. Send String Payload to Endpoint
    fun sendPayload(endpointId: String, message: String) {
        val payload = Payload.fromBytes(message.toByteArray(StandardCharsets.UTF_8))
        connectionsClient.sendPayload(endpointId, payload)
    }

    // 8. Stop All Operations
    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _uiState.update {
            NearbyConnectionState(
                hasRequiredPermissions = it.hasRequiredPermissions,
                localEndpointName = it.localEndpointName
            )
        }
    }

    // --- Callbacks ---

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val newHost = AvailableHost(
                endpointId = endpointId,
                endpointName = info.endpointName,
                status = HostConnectionStatus.DISCOVERED
            )
            _uiState.update { current ->
                val updatedHosts = current.discoveredHosts.filterNot { it.endpointId == endpointId } + newHost
                current.copy(discoveredHosts = updatedHosts)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            _uiState.update { current ->
                current.copy(
                    discoveredHosts = current.discoveredHosts.filterNot { it.endpointId == endpointId }
                )
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Automatically accept connection for RFC-01 flow
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            updateHostStatus(endpointId, HostConnectionStatus.CONNECTING, info.endpointName)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    updateHostStatus(endpointId, HostConnectionStatus.CONNECTED)
                    _uiState.update { current ->
                        val connectedHost = current.discoveredHosts.find { it.endpointId == endpointId }
                            ?: AvailableHost(endpointId, "Peer Device", HostConnectionStatus.CONNECTED)
                        val newConnectedList = current.connectedHosts.filterNot { it.endpointId == endpointId } + connectedHost
                        current.copy(
                            connectedHosts = newConnectedList,
                            statusMessage = "Connected to ${connectedHost.endpointName}"
                        )
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    updateHostStatus(endpointId, HostConnectionStatus.REJECTED)
                    _uiState.update { it.copy(statusMessage = "Connection rejected") }
                }
                else -> {
                    updateHostStatus(endpointId, HostConnectionStatus.DISCONNECTED)
                    _uiState.update { it.copy(statusMessage = "Connection failed with code: ${result.status.statusCode}") }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            handleDisconnectedEndpoint(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes()
                if (bytes != null) {
                    val message = String(bytes, StandardCharsets.UTF_8)
                    _uiState.update { it.copy(lastReceivedPayload = message) }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Transfer status tracking for RFC-01
        }
    }

    private fun updateHostStatus(endpointId: String, status: HostConnectionStatus, fallbackName: String = "Unknown Host") {
        _uiState.update { current ->
            val updatedList = current.discoveredHosts.map { host ->
                if (host.endpointId == endpointId) host.copy(status = status) else host
            }
            val listWithNewHost = if (updatedList.none { it.endpointId == endpointId }) {
                updatedList + AvailableHost(endpointId, fallbackName, status)
            } else updatedList
            current.copy(discoveredHosts = listWithNewHost)
        }
    }

    private fun handleDisconnectedEndpoint(endpointId: String) {
        _uiState.update { current ->
            val updatedDiscovered = current.discoveredHosts.map { host ->
                if (host.endpointId == endpointId) host.copy(status = HostConnectionStatus.DISCONNECTED) else host
            }
            val updatedConnected = current.connectedHosts.filterNot { it.endpointId == endpointId }
            current.copy(
                discoveredHosts = updatedDiscovered,
                connectedHosts = updatedConnected,
                statusMessage = "Disconnected"
            )
        }
    }
}
