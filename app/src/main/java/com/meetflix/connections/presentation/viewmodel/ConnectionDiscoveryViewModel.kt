package com.meetflix.connections.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.meetflix.connections.data.NearbyConnectionsManager
import com.meetflix.connections.domain.models.AvailableHost
import com.meetflix.connections.domain.models.NearbyConnectionState
import kotlinx.coroutines.flow.StateFlow

class ConnectionDiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val nearbyManager = NearbyConnectionsManager(application)
    val uiState: StateFlow<NearbyConnectionState> = nearbyManager.uiState

    fun updatePermissionsState(hasPermissions: Boolean) {
        nearbyManager.updatePermissionsState(hasPermissions)
    }

    fun setLocalDeviceName(name: String) {
        nearbyManager.setLocalName(name)
    }

    fun startAdvertising() {
        nearbyManager.startAdvertising()
    }

    fun stopAdvertising() {
        nearbyManager.stopAdvertising()
    }

    fun toggleAdvertising() {
        if (uiState.value.isAdvertising) {
            stopAdvertising()
        } else {
            startAdvertising()
        }
    }

    fun startDiscovery() {
        nearbyManager.startDiscovery()
    }

    fun stopDiscovery() {
        nearbyManager.stopDiscovery()
    }

    fun toggleDiscovery() {
        if (uiState.value.isDiscovering) {
            stopDiscovery()
        } else {
            startDiscovery()
        }
    }

    fun connectToHost(host: AvailableHost) {
        nearbyManager.connectToEndpoint(host)
    }

    fun disconnectHost(endpointId: String) {
        nearbyManager.disconnectEndpoint(endpointId)
    }

    fun sendPayload(endpointId: String, message: String) {
        nearbyManager.sendPayload(endpointId, message)
    }

    override fun onCleared() {
        super.onCleared()
        nearbyManager.stopAll()
    }
}