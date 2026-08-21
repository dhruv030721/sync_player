package com.meetflix.party.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.connections.domain.models.AvailableHost
import com.meetflix.connections.domain.models.HostConnectionStatus
import com.meetflix.core.ui.components.MFXButton
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.party.domain.models.PartyScreenMode
import com.meetflix.party.presentation.component.EndPartyWarningDialog
import com.meetflix.party.presentation.component.PartyMemberListCard
import com.meetflix.party.presentation.component.PayloadTransferProgressWidget
import com.meetflix.party.presentation.viewmodel.PartyViewModel
import com.meetflix.player.presentation.screen.PlayerScreen

/**
 * Design Pattern: Clean Architecture Presentation Screen (MVI)
 * Dedicated Party Watch Room UI with top ExoPlayer surface, real-time P2P video transfer progress,
 * top-pinned Host member list, and end-party exit confirmation dialogs.
 */
@Composable
fun PartyRoomScreen(
    viewModel: PartyViewModel,
    onExitParty: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerViewModel.uiState.collectAsState()

    var showExitWarningDialog by remember { mutableStateOf(false) }
    val isHostMode = state.mode == PartyScreenMode.HOSTING || state.selectedMediaUri != null

    // System Back Press Handler
    BackHandler {
        showExitWarningDialog = true
    }

    // Exit Warning Dialog
    EndPartyWarningDialog(
        isVisible = showExitWarningDialog,
        isHost = isHostMode,
        onConfirm = {
            showExitWarningDialog = false
            viewModel.leaveParty()
            onExitParty()
        },
        onDismiss = { showExitWarningDialog = false }
    )

    if (playerState.isFullScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            PlayerScreen(
                viewModel = viewModel.playerViewModel,
                onBackClick = {
                    viewModel.playerViewModel.onAction(com.meetflix.player.domain.models.PlaybackAction.ToggleFullScreen)
                }
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. TOP VIDEO PLAYER (Top Half)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (state.isPreparingMedia) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color(0xFF10B981),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.mediaPreparationMessage ?: "Optimizing video for Watch Party...",
                            fontFamily = Geist_Font,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                } else if (state.selectedMediaUri != null || playerState.mediaUri != null) {
                    PlayerScreen(
                        viewModel = viewModel.playerViewModel,
                        onBackClick = { showExitWarningDialog = true }
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Preparing Watch Party Stream...",
                            fontFamily = Geist_Font,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Receiving video P2P from party host",
                            fontFamily = Geist_Font,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 2. P2P VIDEO TRANSFER PROGRESS WIDGET (Disabled for HTTP/Fast-Start streaming)
            PayloadTransferProgressWidget(
                isVisible = false,
                progress = 0f,
                statusMessage = null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
            )

            // 3. PARTY MEMBERS LIST (Host pinned at top, Joinees below)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PARTY MEMBERS (${state.connectedMembers.size + 1})",
                        fontFamily = Geist_Font,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF10B981)
                    )

                    MFXButton(
                        text = if (isHostMode) "End Party" else "Leave Party",
                        onClick = { showExitWarningDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Construct member list: Host pinned at top
                val localHostDevice = AvailableHost(
                    endpointId = "local_device",
                    endpointName = "${state.localDeviceName} (You)",
                    status = HostConnectionStatus.CONNECTED
                )

                val hostMember = if (isHostMode) localHostDevice else state.connectedMembers.firstOrNull() ?: localHostDevice
                val joineeMembers = if (isHostMode) state.connectedMembers else state.connectedMembers.filterNot { it.endpointId == hostMember.endpointId }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // PINNED HOST CARD (ALWAYS AT TOP)
                    item(key = "host_pinned_${hostMember.endpointId}") {
                        PartyMemberListCard(
                            member = hostMember,
                            isHostRole = true
                        )
                    }

                    // CONNECTED JOINEES LISTED BELOW HOST
                    items(joineeMembers, key = { it.endpointId }) { joinee ->
                        PartyMemberListCard(
                            member = joinee,
                            isHostRole = false,
                            onDisconnectClick = if (isHostMode) {
                                { viewModel.nearbyConnectionsManager.disconnectEndpoint(joinee.endpointId) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
