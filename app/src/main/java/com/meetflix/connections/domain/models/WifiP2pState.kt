package com.meetflix.connections.domain.models

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo

data class WifiP2pState(
    val peers: List<WifiP2pDevice> = emptyList(),
    val isSearching: Boolean = false,
    val isConnected: Boolean = false,
)