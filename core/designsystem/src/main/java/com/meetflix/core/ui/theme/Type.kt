package com.meetflix.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.meetflix.core.designsystem.R

// Set of Material typography styles to start with
val Geist_Font = FontFamily(
    Font(R.font.geist_regular)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Geist_Font,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)