package com.meetflix.connections.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.core.designsystem.R
import com.meetflix.connections.domain.models.AvailableHost
import com.meetflix.connections.domain.models.HostConnectionStatus
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.core.ui.theme.PrimaryBtnAlpha
import com.meetflix.core.ui.theme.PrimaryColor
import com.meetflix.core.ui.theme.blackRedGradient
import com.meetflix.core.ui.theme.whiteRedGradientBorder

@Composable
fun ConnectionDeviceCard(
    host: AvailableHost,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = host.status == HostConnectionStatus.CONNECTED
    val isConnecting = host.status == HostConnectionStatus.CONNECTING

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(blackRedGradient)
            .border(
                width = 1.dp,
                brush = whiteRedGradientBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isConnecting) { onActionClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isConnected) Color(0xFF10B981).copy(alpha = 0.2f)
                        else PrimaryColor.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.wifi_icon),
                    contentDescription = stringResource(R.string.cd_device),
                    tint = if (isConnected) Color(0xFF10B981) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Device Information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = host.endpointName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Geist_Font,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Endpoint ID: ${host.endpointId}",
                    fontSize = 12.sp,
                    fontFamily = Geist_Font,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action / Status Badge
            val badgeBgColor = when (host.status) {
                HostConnectionStatus.CONNECTED -> Color(0xFF10B981).copy(alpha = 0.4f)
                HostConnectionStatus.CONNECTING -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                HostConnectionStatus.REJECTED -> Color(0xFFEF4444).copy(alpha = 0.4f)
                else -> PrimaryColor.copy(alpha = PrimaryBtnAlpha)
            }

            val badgeText = when (host.status) {
                HostConnectionStatus.CONNECTED -> stringResource(R.string.connected)
                HostConnectionStatus.CONNECTING -> "Connecting..."
                HostConnectionStatus.REJECTED -> "Rejected"
                HostConnectionStatus.DISCONNECTED -> stringResource(R.string.connect)
                HostConnectionStatus.DISCOVERED -> stringResource(R.string.connect)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(badgeBgColor)
                    .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color.White,
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Geist_Font,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
