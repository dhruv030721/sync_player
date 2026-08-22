package com.meetflix.party.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.core.ui.theme.PrimaryColor

/**
 * Design Pattern: Stateless Dialog Composable
 * Warning confirmation dialog triggered when host or guest attempts to leave or terminate a party watch room.
 */
@Composable
fun EndPartyWarningDialog(
    isVisible: Boolean,
    isHost: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF18181F),
            title = {
                Text(
                    text = if (isHost) "⚠️ End Watch Party?" else "⚠️ Leave Watch Party?",
                    fontFamily = Geist_Font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = if (isHost)
                        "Ending this party room will disconnect all connected joinees and stop P2P video streaming. Are you sure you want to end the party?"
                    else "Are you sure you want to leave this watch party?",
                    fontFamily = Geist_Font,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = if (isHost) "End Party" else "Leave Party",
                        fontFamily = Geist_Font,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        fontFamily = Geist_Font,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        )
    }
}
