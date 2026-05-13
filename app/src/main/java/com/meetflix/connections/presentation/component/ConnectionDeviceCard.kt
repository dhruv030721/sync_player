package com.meetflix.connections.presentation.component

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.R
import com.meetflix.core.ui.theme.DarkGrayColor
import com.meetflix.core.ui.theme.Gray_Color
import com.meetflix.core.ui.theme.PrimaryBtnAlpha
import com.meetflix.core.ui.theme.PrimaryColor
import com.meetflix.core.ui.theme.blackRedGradient
import com.meetflix.core.ui.theme.whiteRedGradientBorder

@Composable
fun ConnectionDeviceCard(
    deviceName: String,
    deviceAddress: String,
    isConnected: Boolean,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(blackRedGradient)
            .border(
                width = 1.dp,
                brush = whiteRedGradientBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onConnect() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Device Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.wifi_icon),
                    contentDescription = stringResource(R.string.cd_device),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Device Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = deviceName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = deviceAddress,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Normal
                )
            }

            // Connect Indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        if (isConnected)
                            Color(0xFF10B981).copy(alpha = 0.4f)
                        else
                            PrimaryColor.copy(alpha = PrimaryBtnAlpha)
                    )
                    .border(1.dp, Color.White.copy(0.5f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isConnected) {
                        stringResource(R.string.connected)
                    } else {
                        stringResource(R.string.connect)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
