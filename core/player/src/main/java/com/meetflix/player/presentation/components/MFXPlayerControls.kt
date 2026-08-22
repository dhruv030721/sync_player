package com.meetflix.player.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.core.designsystem.R
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.core.ui.theme.PrimaryColor
import com.meetflix.player.domain.models.PlaybackAction
import com.meetflix.player.domain.models.PlayerState

/**
 * Design Pattern: Stateless Reusable Composable
 * Player controls overlay supporting play/pause, seekbar, speed selection, and title header.
 */
@Composable
fun MFXPlayerControls(
    state: PlayerState,
    onAction: (PlaybackAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    val playbackDrawable = if (state.isPlaying) {
        painterResource(R.drawable.ic_pause)
    } else {
        painterResource(R.drawable.ic_play)
    }

    AnimatedVisibility(
        visible = state.isControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.videoTitle,
                    fontFamily = Geist_Font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            // --- CENTER: PLAY / PAUSE BUTTON ---
            IconButton(
                onClick = {
                    if (state.isPlaying) {
                        onAction(PlaybackAction.Pause)
                    } else {
                        onAction(PlaybackAction.Play)
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .background(PrimaryColor, shape = androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    painter = playbackDrawable,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // --- BOTTOM BAR: SEEKBAR & SPEED SELECTOR ---
            val context = androidx.compose.ui.platform.LocalContext.current
            val fullscreenDrawable = if (state.isFullScreen) {
                painterResource(R.drawable.ic_fullscreen_exit)
            } else {
                painterResource(R.drawable.ic_fullscreen)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val activity = context as? android.app.Activity
                            val nextFullScreen = !state.isFullScreen
                            activity?.requestedOrientation = if (nextFullScreen) {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                            onAction(PlaybackAction.ToggleFullScreen)
                        }
                    ) {
                        Icon(
                            painter = fullscreenDrawable,
                            contentDescription = if (state.isFullScreen) "Exit Fullscreen" else "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Text(
                            text = "${state.playbackSpeed}x",
                            fontFamily = Geist_Font,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryColor,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(8.dp)
                                .background(Color.Transparent)
                        )
                        IconButton(onClick = { showSpeedMenu = true }) {}

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x", fontFamily = Geist_Font) },
                                    onClick = {
                                        onAction(PlaybackAction.SetSpeed(speed))
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                MFXSeekBar(
                    currentPositionMs = state.currentPositionMs,
                    durationMs = state.durationMs,
                    enabled = state.canSeek,
                    onSeek = { position ->
                        onAction(PlaybackAction.Seek(position))
                    }
                )
            }
        }
    }
}