package com.meetflix.connections.presentation.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import com.meetflix.connections.domain.models.WifiP2pState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ConnectionDiscoveryViewModel(application: Application) : AndroidViewModel(application) {
    private val manager: WifiP2pManager? =
        application.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?

    private val channel: WifiP2pManager.Channel? =
        manager?.initialize(application, Looper.getMainLooper(), null)

    private val wifiManager =
        application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _uiState = MutableStateFlow(WifiP2pState())
    val uiState: StateFlow<WifiP2pState> = _uiState.asStateFlow()

    /**
     * Check if WiFi is enabled
     */
    private fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    /**
     * Open WiFi settings screen
     */
    private fun openWifiSettings() {
        val intent = Intent(Settings.Panel.ACTION_WIFI)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }


    // 1. Start Discovery
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun discoverPeers() {
        // Check WiFi first
        if (!isWifiEnabled()) {
            openWifiSettings()
            return
        }
        _uiState.update { it.copy(isSearching = true) }
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
            }

            override fun onFailure(reason: Int) {
                _uiState.update { it.copy(isSearching = false) }
            }
        })
    }

    // 2. Peer List Listener (Triggered by BroadcastReceiver)
    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        _uiState.update { it.copy(peers = peerList.deviceList.toList(), isSearching = false) }
    }

    // 3. Connect to a specific device
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { /* Handled by BroadcastReceiver */
            }

            override fun onFailure(reason: Int) { /* Error */
            }
        })
    }

}