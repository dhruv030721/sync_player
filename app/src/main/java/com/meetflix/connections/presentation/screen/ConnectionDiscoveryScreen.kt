package com.meetflix.connections.presentation.screen

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meetflix.R
import com.meetflix.connections.domain.broadcastreceiver.WiFiDirectBroadcastReceiver
import com.meetflix.connections.presentation.component.ConnectionDeviceCard
import com.meetflix.connections.presentation.viewmodel.ConnectionDiscoveryViewModel
import com.meetflix.core.ui.components.MFXButton
import com.meetflix.core.ui.theme.DarkGrayColor
import com.meetflix.core.ui.theme.whiteRedGradientBorder

@Composable
fun ConnectionDiscoveryScreen(
    viewModel: ConnectionDiscoveryViewModel = viewModel(), innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    val manager = remember {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }

    val channel = remember {
        manager.initialize(
            context, Looper.getMainLooper(), null
        )
    }

    DisposableEffect(Unit) {

        val receiver = WiFiDirectBroadcastReceiver(
            manager = manager, channel = channel, viewModel = viewModel
        )

        val intentFilter = IntentFilter().apply {

            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        context.registerReceiver(receiver, intentFilter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }


    Column(
        modifier = Modifier.padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isSearching -> {
                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Red.copy(0.3f))
                    }
                }

                state.peers.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        items(state.peers) { device ->
                            ConnectionDeviceCard(
                                deviceName = device.deviceName,
                                deviceAddress = device.deviceAddress,
                                isConnected = false
                            ) {
                                if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.NEARBY_WIFI_DEVICES
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) viewModel.connectToDevice(device)
                            }
                        }
                    }
                }
            }
        }
        MFXButton(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.16f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Red.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = whiteRedGradientBorder,
                    shape = RoundedCornerShape(16.dp)
                ),
            text = stringResource(R.string.search_connection),
        ) {
            viewModel.discoverPeers()
        }
    }
}
