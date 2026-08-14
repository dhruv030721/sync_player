package com.meetflix.connections.presentation.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meetflix.R
import com.meetflix.connections.domain.models.HostConnectionStatus
import com.meetflix.connections.presentation.component.ConnectionDeviceCard
import com.meetflix.connections.presentation.viewmodel.ConnectionDiscoveryViewModel
import com.meetflix.core.ui.components.MFXButton
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.core.ui.theme.PrimaryColor
import com.meetflix.core.ui.theme.blackRedGradient
import com.meetflix.core.ui.theme.whiteRedGradientBorder

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

    var messageInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Status Header & Device Info Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(blackRedGradient)
                .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RFC-01 Local Connections",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Geist_Font,
                            color = Color.White
                        )
                        Text(
                            text = "Device Name: ${uiState.localEndpointName}",
                            fontSize = 12.sp,
                            fontFamily = Geist_Font,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    uiState.connectedHosts.isNotEmpty() -> Color(0xFF10B981)
                                    uiState.isAdvertising || uiState.isDiscovering -> Color(0xFFF59E0B)
                                    else -> Color.Gray
                                }
                            )
                    )
                }

                uiState.statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        fontFamily = Geist_Font,
                        color = Color.Red.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Panel: Host (Advertise) & Search (Discover) Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Advertise Button
            MFXButton(
                text = if (uiState.isAdvertising) "Stop Advertising" else "Host Room",
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (uiState.isAdvertising) androidx.compose.ui.graphics.SolidColor(Color(0xFF8B1210).copy(alpha = 0.8f))
                        else Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Red.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(12.dp)),
                onClick = {
                    if (!permissionsGranted) {
                        permissionLauncher.launch(requiredPermissions)
                    } else {
                        viewModel.toggleAdvertising()
                    }
                }
            )

            // Discover Button
            MFXButton(
                text = if (uiState.isDiscovering) "Stop Search" else stringResource(R.string.search_connection),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (uiState.isDiscovering) androidx.compose.ui.graphics.SolidColor(Color(0xFF8B1210).copy(alpha = 0.8f))
                        else Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Red.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(12.dp)),
                onClick = {
                    if (!permissionsGranted) {
                        permissionLauncher.launch(requiredPermissions)
                    } else {
                        viewModel.toggleDiscovery()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Payload Transfer section if connected
        AnimatedVisibility(visible = uiState.connectedHosts.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(blackRedGradient)
                    .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Connected Payload Channel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Geist_Font,
                    color = Color.White
                )

                uiState.lastReceivedPayload?.let { lastMsg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Received: $lastMsg",
                        fontSize = 12.sp,
                        fontFamily = Geist_Font,
                        color = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Enter RFC payload...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = Color.White.copy(0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MFXButton(
                        text = "Send",
                        modifier = Modifier
                            .width(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryColor)
                            .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(8.dp)),
                        height = 48.dp,
                        onClick = {
                            val activeEndpoint = uiState.connectedHosts.firstOrNull()?.endpointId
                            if (activeEndpoint != null && messageInput.isNotBlank()) {
                                viewModel.sendPayload(activeEndpoint, messageInput)
                                messageInput = ""
                            }
                        }
                    )
                }
            }
        }

        // Main List Box: Discovered Endpoints or Empty State
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                !permissionsGranted -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.wifi_icon),
                            contentDescription = null,
                            tint = Color.Red.copy(0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nearby permissions required",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Geist_Font,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Grant location & nearby devices permissions to discover peers",
                            fontSize = 12.sp,
                            fontFamily = Geist_Font,
                            color = Color.White.copy(0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MFXButton(
                            text = "Grant Permissions",
                            modifier = Modifier
                                .width(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryColor)
                                .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(12.dp)),
                            onClick = { permissionLauncher.launch(requiredPermissions) }
                        )
                    }
                }

                uiState.isDiscovering && uiState.discoveredHosts.isEmpty() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryColor,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Searching for nearby hosts...",
                            fontSize = 14.sp,
                            fontFamily = Geist_Font,
                            color = Color.White.copy(0.7f)
                        )
                    }
                }

                uiState.discoveredHosts.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.discoveredHosts,
                            key = { it.endpointId }
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

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.wifi_icon),
                            contentDescription = null,
                            tint = Color.White.copy(0.2f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Nearby Devices Discovered",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Geist_Font,
                            color = Color.White.copy(0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Host Room' to advertise or 'Search for Devices' to discover",
                            fontSize = 12.sp,
                            fontFamily = Geist_Font,
                            color = Color.White.copy(0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
