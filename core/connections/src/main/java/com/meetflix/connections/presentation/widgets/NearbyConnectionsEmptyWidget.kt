package com.meetflix.connections.presentation.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.core.designsystem.R
import com.meetflix.core.ui.theme.Geist_Font

@Composable
fun NearbyConnectionsEmptyWidget(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.wifi_icon),
            contentDescription = null,
            tint = Color.White.copy(0.2f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No Nearby Devices Discovered",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = Geist_Font,
            color = Color.White.copy(0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap 'Host Room' to advertise or 'Search for Devices' to discover",
            fontSize = 12.sp,
            fontFamily = Geist_Font,
            color = Color.White.copy(0.4f),
            textAlign = TextAlign.Center
        )
    }
}
