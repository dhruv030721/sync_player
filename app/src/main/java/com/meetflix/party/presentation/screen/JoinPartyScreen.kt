package com.meetflix.party.presentation.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meetflix.R
import com.meetflix.connections.presentation.component.ConnectionDeviceCard
import com.meetflix.connections.presentation.widgets.NearbyConnectionsHeaderWidget
import com.meetflix.core.ui.components.MFXButton
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.party.presentation.component.PartyActionCard
import com.meetflix.party.presentation.component.PayloadTransferProgressWidget
import com.meetflix.party.presentation.viewmodel.PartyViewModel
import com.meetflix.player.presentation.screen.PlayerScreen

/**
 * Design Pattern: Clean Architecture Presentation Screen
 * Dedicated Join Party screen allowing users to discover nearby party hosts, join party rooms,
 * receive P2P video streams, and enjoy synchronized playback.
 */
@Composable
fun JoinPartyScreen(
    innerPadding: PaddingValues,
    viewModel: PartyViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color.Black)
    ) {
        // 1. VIDEO PLAYER / HEADER AREA
        if (state.selectedMediaUri != null || playerState.mediaUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                PlayerScreen(
                    viewModel = viewModel.playerViewModel,
                    onBackClick = { viewModel.leaveParty() }
                )
            }
        } else {
            NearbyConnectionsHeaderWidget(
                localDeviceName = state.localDeviceName,
                isConnected = state.connectedMembers.isNotEmpty(),
                isActiveRole = state.mode != com.meetflix.party.domain.models.PartyScreenMode.IDLE,
                statusMessage = state.statusMessage,
                modifier = Modifier.padding(16.dp)
            )
        }

        // 2. TRANSFER PROGRESS BANNER
        PayloadTransferProgressWidget(
            isVisible = state.isTransferring,
            progress = state.transferProgress,
            statusMessage = state.transferStatusMessage,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // 3. JOIN WORKFLOW CONTENT
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            if (state.selectedMediaUri == null && playerState.mediaUri == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEARBY PARTY HOSTS (${state.discoveredParties.size})",
                        fontFamily = Geist_Font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF10B981)
                    )

                    MFXButton(
                        text = if (state.mode == com.meetflix.party.domain.models.PartyScreenMode.DISCOVERING) "Stop Search" else "Search Parties",
                        onClick = {
                            if (state.mode == com.meetflix.party.domain.models.PartyScreenMode.DISCOVERING) {
                                viewModel.stopDiscovery()
                            } else {
                                viewModel.startDiscovery()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (state.mode != com.meetflix.party.domain.models.PartyScreenMode.DISCOVERING && state.discoveredParties.isEmpty()) {
                    PartyActionCard(
                        title = "🔍 Search Nearby Parties",
                        subtitle = "Scan for nearby party hosts who have launched a watch room.",
                        iconRes = R.drawable.wifi_icon,
                        accentColor = Color(0xFF10B981),
                        onClick = { viewModel.startDiscovery() }
                    )
                } else if (state.discoveredParties.isEmpty()) {
                    Text(
                        text = "Searching for nearby parties... Make sure the host has selected a video on 'Host Party'.",
                        fontFamily = Geist_Font,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.discoveredParties, key = { it.endpointId }) { host ->
                            ConnectionDeviceCard(
                                host = host,
                                onActionClick = { viewModel.joinParty(host) }
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONNECTED TO PARTY",
                        fontFamily = Geist_Font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF10B981)
                    )

                    MFXButton(
                        text = "Leave Party",
                        onClick = { viewModel.leaveParty() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.connectedMembers, key = { it.endpointId }) { host ->
                        ConnectionDeviceCard(
                            host = host,
                            onActionClick = { viewModel.nearbyConnectionsManager.disconnectEndpoint(host.endpointId) }
                        )
                    }
                }
            }
        }
    }
}
