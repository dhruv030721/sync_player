package com.meetflix.party.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.R
import com.meetflix.connections.domain.models.AvailableHost
import com.meetflix.core.ui.theme.Geist_Font

/**
 * Design Pattern: Stateless Composable Component
 * Elegant, minimalist member card for displaying party hosts and connected joinees.
 */
@Composable
fun PartyMemberListCard(
    member: AvailableHost,
    isHostRole: Boolean,
    onDisconnectClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val badgeColor = if (isHostRole) Color(0xFFF59E0B) else Color(0xFF10B981)
    val badgeText = if (isHostRole) "HOST" else "MEMBER"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141419))
            .border(
                width = 1.dp,
                color = if (isHostRole) badgeColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minimalist Circular Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = if (isHostRole) R.drawable.ic_tab_party else R.drawable.wifi_icon),
                contentDescription = member.endpointName,
                tint = badgeColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Device Name & Info
        Text(
            text = member.endpointName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = Geist_Font,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        // Minimal Pill Tag
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(badgeColor.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Geist_Font,
                color = badgeColor,
                letterSpacing = 0.5.sp
            )
        }

        if (onDisconnectClick != null && !isHostRole) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                    .clickable { onDisconnectClick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Remove",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Geist_Font,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}
