package com.meetflix.player.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

        MFXPlayerControls(
            state = state,
            onAction = { action -> viewModel.onAction(action) },
            onBackClick = onBackClick
        )
    }
}