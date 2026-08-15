package com.meetflix.party.presentation.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.connections.data.NearbyConnectionsManager
import com.meetflix.connections.presentation.component.ConnectionDeviceCard
import com.meetflix.connections.presentation.widgets.NearbyConnectionsHeaderWidget
import com.meetflix.core.ui.components.MFXButton
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.player.data.ExoPlayerManager
import com.meetflix.player.data.PlayerSyncManager
import com.meetflix.player.domain.models.PlaybackAction
import com.meetflix.player.presentation.screen.PlayerScreen
import com.meetflix.player.presentation.viewmodel.PlayerViewModel

/**
 * Design Pattern: Clean Architecture Presentation Screen
 * Dedicated Party Mode watch room combining synchronized ExoPlayer playback with connected party members list.
 */
@Composable
fun PartyModeScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current

    val nearbyConnectionsManager = remember { NearbyConnectionsManager(context) }
    val exoPlayerManager = remember { ExoPlayerManager(context) }
    val playerSyncManager = remember { PlayerSyncManager(exoPlayerManager, nearbyConnectionsManager) }
    val playerViewModel = remember { PlayerViewModel(exoPlayerManager, playerSyncManager) }

    val playerState by playerViewModel.uiState.collectAsState()
    val partyState by nearbyConnectionsManager.uiState.collectAsState()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            playerViewModel.onAction(PlaybackAction.LoadMedia(it.toString(), "Party Watch Video"))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerViewModel.exoPlayerManager.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color.Black)
    ) {
        // 1. VIDEO PLAYER (Top half)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color.DarkGray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (playerState.mediaUri != null) {
                PlayerScreen(
                    viewModel = playerViewModel,
                    onBackClick = { /* Handle back */ }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Video Selected for Party Watch",
                        fontFamily = Geist_Font,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MFXButton(
                        text = "Select Video to Host Party",
                        onClick = { videoPickerLauncher.launch("video/*") }
                    )
                }
            }
        }

        // 2. CONNECTED PARTY MEMBERS LIST (Bottom half)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            NearbyConnectionsHeaderWidget(
                localDeviceName = partyState.localEndpointName,
                isConnected = partyState.connectedHosts.isNotEmpty(),
                isActiveRole = partyState.isAdvertising || partyState.isDiscovering,
                statusMessage = partyState.statusMessage
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PARTY MEMBERS (${partyState.connectedHosts.size})",
                fontFamily = Geist_Font,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF10B981)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (partyState.connectedHosts.isEmpty()) {
                Text(
                    text = "No party members connected yet. Connect via Nearby tab to watch together!",
                    fontFamily = Geist_Font,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    items(partyState.connectedHosts, key = { it.endpointId }) { host ->
                        ConnectionDeviceCard(
                            host = host,
                            onActionClick = { nearbyConnectionsManager.disconnectEndpoint(host.endpointId) }
                        )
                    }
                }
            }
        }
    }
}