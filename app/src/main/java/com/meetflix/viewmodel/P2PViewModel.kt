package com.example.meetflix.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel

class P2PViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context: Context = application.applicationContext
    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = manager.initialize(context, context.mainLooper, null)

    val devices = mutableStateListOf<WifiP2pDevice>()
    val connectedDevices = mutableStateListOf<WifiP2pDevice>()

    private val peerReceiver = object : BroadcastReceiver() {
        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    // Check permissions before calling requestPeers
                    if (hasWifiP2pPermissions()) {
                        manager.requestPeers(channel) { peerList ->
                            devices.clear()
                            devices.addAll(peerList.deviceList)
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    Log.d("P2P", "Connection changed")
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(peerReceiver, filter)
    }

    fun hasWifiP2pPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val nearbyWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return fineLocation && nearbyWifi
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun discoverPeers() {
        if (!hasWifiP2pPermissions()) return
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d("P2P", "Discovery started") }
            override fun onFailure(reason: Int) { Log.d("P2P", "Discovery failed: $reason") }
        })
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun connectToDevice(device: WifiP2pDevice) {
        if (!hasWifiP2pPermissions()) return
        val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                connectedDevices.add(device)
            }
            override fun onFailure(reason: Int) { Log.d("P2P", "Connection failed: $reason") }
        })
    }

    override fun onCleared() {
        super.onCleared()
        context.unregisterReceiver(peerReceiver)
    }
}
