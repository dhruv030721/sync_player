package com.meetflix.connections.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.core.designsystem.R
import com.meetflix.core.ui.components.MFXButton
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.core.ui.theme.PrimaryColor
import com.meetflix.core.ui.theme.whiteRedGradientBorder

@Composable
fun NearbyConnectionsPermissionView(
    onRequestPermissions: () -> Unit,
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
            tint = Color.Red.copy(0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Nearby permissions required",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = Geist_Font,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Grant location & nearby devices permissions to discover peers",
            fontSize = 12.sp,
            fontFamily = Geist_Font,
            color = Color.White.copy(0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        MFXButton(
            text = "Grant Permissions",
            modifier = Modifier
                .width(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryColor)
                .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(12.dp)),
            onClick = onRequestPermissions
        )
    }
}