package com.meetflix.connections.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.core.ui.components.MFXButton
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.core.ui.theme.PrimaryColor
import com.meetflix.core.ui.theme.blackRedGradient
import com.meetflix.core.ui.theme.whiteRedGradientBorder

@Composable
fun NearbyConnectionsPayloadWidget(
    lastReceivedPayload: String?,
    onSendPayload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(blackRedGradient)
            .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Connected Payload Channel",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = Geist_Font,
            color = Color.White
        )

        lastReceivedPayload?.let { lastMsg ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Received: $lastMsg",
                fontSize = 12.sp,
                fontFamily = Geist_Font,
                color = Color(0xFF10B981)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Enter RFC payload...", color = Color.Gray, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryColor,
                    unfocusedBorderColor = Color.White.copy(0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            MFXButton(
                text = "Send",
                modifier = Modifier
                    .width(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryColor)
                    .border(1.dp, whiteRedGradientBorder, RoundedCornerShape(8.dp)),
                height = 48.dp,
                onClick = {
                    if (messageInput.isNotBlank()) {
                        onSendPayload(messageInput)
                        messageInput = ""
                    }
                }
            )
        }
    }
}
