package com.meetflix.party.presentation.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meetflix.R
import com.meetflix.connections.presentation.component.ConnectionDeviceCard
import com.meetflix.core.ui.components.MFXButton
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.party.domain.models.PartyScreenMode
import com.meetflix.party.presentation.activity.PartyRoomActivity
import com.meetflix.party.presentation.component.PartyActionCard
import com.meetflix.party.presentation.viewmodel.PartyViewModel

/**
 * Design Pattern: Clean Architecture Presentation Screen
 * Modern Join Party tab screen for discovering nearby party hosts, handling runtime permissions,
 * and launching PartyRoomActivity.
 */
@Composable
fun JoinPartyScreen(
    innerPadding: PaddingValues,
    viewModel: PartyViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.updatePermissionsState(allGranted)
        if (allGranted) {
            viewModel.startDiscovery()
        }
    }

    fun startDiscoveryWithPermissionCheck() {
        val hasAllPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasAllPermissions) {
            viewModel.startDiscovery()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color.Black)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Nearby Watch Parties",
                    fontFamily = Geist_Font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${state.discoveredParties.size} host(s) found",
                    fontFamily = Geist_Font,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            MFXButton(
                text = if (state.mode == PartyScreenMode.DISCOVERING) "Stop Scan" else "Scan Parties",
                onClick = {
                    if (state.mode == PartyScreenMode.DISCOVERING) {
                        viewModel.stopDiscovery()
                    } else {
                        startDiscoveryWithPermissionCheck()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.mode != PartyScreenMode.DISCOVERING && state.discoveredParties.isEmpty()) {
            PartyActionCard(
                title = "Search Nearby Parties",
                subtitle = "Scan for active party hosts in your immediate vicinity.",
                iconRes = R.drawable.wifi_icon,
                accentColor = Color(0xFF10B981),
                onClick = { startDiscoveryWithPermissionCheck() }
            )
        } else if (state.discoveredParties.isEmpty()) {
            Text(
                text = "Scanning for nearby hosts... Ensure the host has selected a video on 'Host Party'.",
                fontFamily = Geist_Font,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.discoveredParties, key = { it.endpointId }) { host ->
                    ConnectionDeviceCard(
                        host = host,
                        onActionClick = {
                            PartyRoomActivity.launchAsGuest(context, host)
                        }
                    )
                }
            }
        }
    }
}
