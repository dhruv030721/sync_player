package com.meetflix.connections.domain.broadcastreceiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import androidx.annotation.RequiresPermission
import com.meetflix.connections.presentation.viewmodel.ConnectionDiscoveryViewModel

class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager?,
    private val channel: WifiP2pManager.Channel?,
    private val viewModel: ConnectionDiscoveryViewModel
) : BroadcastReceiver() {

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES
        ]
    )
    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {

                manager?.requestPeers(
                    channel,
                    viewModel.peerListListener
                )
            }
        }
    }
}