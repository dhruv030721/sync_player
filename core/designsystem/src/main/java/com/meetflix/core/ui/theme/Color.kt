package com.meetflix.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color

// Purple Shades
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val PurpleBtnBorderColor = Color(0xFF332155)
val PurpleBtnBGColor = Color(0xFF201a30)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)


// Pink Shades
val Pink80 = Color(0xFFEFB8C8)
val Pink40 = Color(0xFF7D5260)


// Red Shades
val BloodRed = Color(0xFF65000B)


// App Colors
val Old_PrimaryColor = Color(0xFF7C3BED)
val PrimaryColor = Color(0xFF8b1210)
const val PrimaryBtnAlpha = 0.5f
val Gray_Color = Color(0xFF2E2E38)
val DarkGrayColor = Color(0xFF1A1A1F)
val Zinc700Color = Color(0xFF3F3F46)


// Gradient Color
val blackWhiteGradient: Brush = verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.2f),
        Color.Black.copy(alpha = 0.1f),
        Color.Black.copy(alpha = 0.3f)
    )
)

val blackRedGradient: Brush = verticalGradient(
    colors = listOf(
        Color.Black.copy(alpha = 0.16f),
        Color.Black.copy(alpha = 0.4f),
        Color.Red.copy(alpha = 0.1f)
    )
)

// Gradient Border Color
val whiteRedGradientBorder = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.35f),
        Color.Red.copy(alpha = 0.08f)
    )
)