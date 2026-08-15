package com.meetflix.connections.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.core.ui.theme.blackRedGradient
import com.meetflix.core.ui.theme.whiteRedGradientBorder

@Composable
fun NearbyConnectionsHeaderWidget(
    localDeviceName: String,
    isConnected: Boolean,
    isActiveRole: Boolean,
    statusMessage: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                        text = "Device Name: $localDeviceName",
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
                                isConnected -> Color(0xFF10B981)
                                isActiveRole -> Color(0xFFF59E0B)
                                else -> Color.Gray
                            }
                        )
                )
            }

            statusMessage?.let { msg ->
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
}
