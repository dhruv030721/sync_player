package com.meetflix.connections.presentation.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meetflix.connections.domain.models.HostConnectionStatus
import com.meetflix.connections.presentation.component.ConnectionDeviceCard
import com.meetflix.connections.presentation.viewmodel.ConnectionDiscoveryViewModel
import com.meetflix.connections.presentation.widgets.NearbyConnectionsControlWidget
import com.meetflix.connections.presentation.widgets.NearbyConnectionsEmptyWidget
import com.meetflix.connections.presentation.widgets.NearbyConnectionsHeaderWidget
import com.meetflix.connections.presentation.widgets.NearbyConnectionsPermissionView
import com.meetflix.connections.presentation.widgets.NearbyConnectionsSearchingWidget
import com.meetflix.core.ui.theme.Geist_Font

@Composable
fun ConnectionDiscoveryScreen(
    viewModel: ConnectionDiscoveryViewModel = viewModel(),
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Required runtime permissions based on SDK version
    val requiredPermissions = remember {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        permissions.toTypedArray()
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        permissionsGranted = granted
        viewModel.updatePermissionsState(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.updatePermissionsState(permissionsGranted)
        if (!permissionsGranted) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Widget 1: Status Header Banner
        NearbyConnectionsHeaderWidget(
            localDeviceName = uiState.localEndpointName,
            isConnected = uiState.connectedHosts.isNotEmpty(),
            isActiveRole = uiState.isAdvertising || uiState.isDiscovering,
            statusMessage = uiState.statusMessage
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Widget 2: Control Panel (Host Room & Search Buttons)
        NearbyConnectionsControlWidget(
            isAdvertising = uiState.isAdvertising,
            isDiscovering = uiState.isDiscovering,
            onToggleAdvertising = {
                if (!permissionsGranted) {
                    permissionLauncher.launch(requiredPermissions)
                } else {
                    viewModel.toggleAdvertising()
                }
            },
            onToggleDiscovery = {
                if (!permissionsGranted) {
                    permissionLauncher.launch(requiredPermissions)
                } else {
                    viewModel.toggleDiscovery()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main Connection Lists Area
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                !permissionsGranted -> {
                    // Stable Permission View
                    NearbyConnectionsPermissionView(
                        onRequestPermissions = { permissionLauncher.launch(requiredPermissions) }
                    )
                }

                uiState.isDiscovering && uiState.discoveredHosts.isEmpty() && uiState.connectedHosts.isEmpty() -> {
                    // Searching Loading Widget
                    NearbyConnectionsSearchingWidget()
                }

                uiState.connectedHosts.isNotEmpty() || uiState.discoveredHosts.isNotEmpty() -> {
                    // Two Lists Layout: Connected Connections on Top, Discovered Connections Below
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // SECTION 1: Connected Connections List
                        if (uiState.connectedHosts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "CONNECTED CONNECTIONS (${uiState.connectedHosts.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Geist_Font,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(
                                items = uiState.connectedHosts,
                                key = { "connected_${it.endpointId}" }
                            ) { host ->
                                ConnectionDeviceCard(
                                    host = host,
                                    onActionClick = { viewModel.disconnectHost(host.endpointId) }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // SECTION 2: Discovered Connections List (Clears when discovery stops)
                        if (uiState.discoveredHosts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "DISCOVERED CONNECTIONS (${uiState.discoveredHosts.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Geist_Font,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(
                                items = uiState.discoveredHosts,
                                key = { "discovered_${it.endpointId}" }
                            ) { host ->
                                ConnectionDeviceCard(
                                    host = host,
                                    onActionClick = {
                                        if (host.status == HostConnectionStatus.CONNECTED) {
                                            viewModel.disconnectHost(host.endpointId)
                                        } else {
                                            viewModel.connectToHost(host)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Empty Widget
                    NearbyConnectionsEmptyWidget()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
