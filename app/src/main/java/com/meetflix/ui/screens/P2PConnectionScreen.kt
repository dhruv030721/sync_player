package com.meetflix.ui.screens

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.meetflix.ui.screens.P2PViewModel
import com.meetflix.R
import com.meetflix.ui.components.MFXButton
import com.meetflix.ui.theme.Geist_Font
import com.meetflix.ui.theme.Gray_Color
import com.meetflix.ui.theme.PrimaryBtnAlpha
import com.meetflix.ui.theme.PrimaryColor

@Composable
fun P2PConnectionScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current.applicationContext as Application

    val p2pViewModel: P2PViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(P2PViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return P2PViewModel(context) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // optional: handle granted/rejected
    }

    val devices = p2pViewModel.devices
    val connectedDevices = p2pViewModel.connectedDevices

    val discoverDevicesHandler = {
        val neededPermissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            neededPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val missing = neededPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            p2pViewModel.discoverPeers()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.wifi_icon),
                    contentDescription = "WiFi",
                    tint = PrimaryColor,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Connect with Friends",
                    fontFamily = Geist_Font,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Find nearby devices to start watching together",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Discover Button
            MFXButton(
                text = "Find your Homies!",
                modifier = Modifier.fillMaxWidth(),
                bgColor = PrimaryColor,
                bgAlpha = PrimaryBtnAlpha,
                borderColor = PrimaryColor,
                borderSize = 1.dp,
                textColor = Color.White,
                icon = R.drawable.wifi_icon,
                iconSize = 24.dp,
                onClick = {
                    discoverDevicesHandler()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Devices List Section
            AnimatedVisibility(
                visible = devices.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Devices",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = PrimaryColor.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "${devices.size}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(devices) { device ->
                            val isConnected = p2pViewModel.connectedDevices.any {
                                it.deviceAddress == device.deviceAddress
                            }
                            DeviceCard(
                                deviceName = device.deviceName ?: "Unknown Device",
                                deviceAddress = device.deviceAddress ?: "No Address",
                                isConnected = isConnected,
                                onConnect = { p2pViewModel.connectToDevice(device) }
                            )
                        }
                    }
                }
            }

            // Empty State
            if (devices.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.wifi_icon),
                        contentDescription = "No devices",
                        tint = Gray_Color,
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No devices found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap the button above to start searching",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DeviceCard(
    deviceName: String,
    deviceAddress: String,
    isConnected: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Gray_Color.copy(0.3f)
        ),
        border = BorderStroke(1.dp, Gray_Color.copy(0.5f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.wifi_icon),
                    contentDescription = "Device",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Device Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = deviceName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = deviceAddress,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Normal
                )
            }

            // Connect Indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isConnected) Color(0xFF10B981).copy(alpha = 0.4f)
                        else PrimaryColor.copy(alpha = PrimaryBtnAlpha)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.wifi_icon),
                    contentDescription = "Connect",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}