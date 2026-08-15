package com.meetflix.connections.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meetflix.R
import com.meetflix.core.ui.components.MFXButton
import com.meetflix.core.ui.theme.whiteRedGradientBorder

@Composable
fun NearbyConnectionsControlWidget(
    isAdvertising: Boolean,
    isDiscovering: Boolean,
    onToggleAdvertising: () -> Unit,
    onToggleDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Host / Advertise Button
        MFXButton(
            text = if (isAdvertising) "Stop Advertising" else "Host Room",
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isAdvertising) SolidColor(Color(0xFF8B1210).copy(alpha = 0.8f))
                    else Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Red.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(12.dp)),
            onClick = onToggleAdvertising
        )

        // Search / Discover Button
        MFXButton(
            text = if (isDiscovering) "Stop Search" else stringResource(R.string.search_connection),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isDiscovering) SolidColor(Color(0xFF8B1210).copy(alpha = 0.8f))
                    else Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Red.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(12.dp)),
            onClick = onToggleDiscovery
        )
    }
}
