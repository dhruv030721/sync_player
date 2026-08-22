package com.meetflix.player.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.player.domain.models.PlaybackAction
import com.meetflix.player.presentation.components.MFXPlayerControls
import com.meetflix.player.presentation.components.VideoPlayerSurface
import com.meetflix.player.presentation.viewmodel.PlayerViewModel

/**
 * Design Pattern: Presentation Container Screen
 * Assembles VideoPlayerSurface and MFXPlayerControls overlay into a unified Player View.
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoPlayerSurface(
            exoPlayer = viewModel.exoPlayerManager.exoPlayer,
            onTap = {
                viewModel.onAction(PlaybackAction.ToggleControls)
            }
        )

        // Consumer Syncing Loader Overlay
        if (state.isSyncing || (state.isBuffering && !state.canSeek)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color(0xFF10B981),
                        strokeWidth = 3.dp
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                    androidx.compose.material3.Text(
                        text = "Syncing playback...",
                        fontFamily = com.meetflix.core.ui.theme.Geist_Font,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                    androidx.compose.material3.Text(
                        text = "Aligning stream with Watch Party host",
                        fontFamily = com.meetflix.core.ui.theme.Geist_Font,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        MFXPlayerControls(
            state = state,
            onAction = { action -> viewModel.onAction(action) },
            onBackClick = onBackClick
        )
    }
}