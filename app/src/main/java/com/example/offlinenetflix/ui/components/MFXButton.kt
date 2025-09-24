package com.example.meetflix.ui.components

import android.R.attr.textColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.meetflix.ui.theme.Geist_Font

@Composable
fun MFXButton(
    text: String,
    modifier: Modifier,
    height: Dp = 50.dp,
    cornerRadius: Dp = 10.dp,
    backgroundColor: Color = Color(0xFF7C3BED),
    borderColor: Color = Color.Transparent,
    borderSize: Dp = 0.1.dp,
    textColor: Color = Color.White,
    textStyle: TextStyle = TextStyle(fontSize = 16.sp),
    onClick: () -> Unit
) {
    Button(
        onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(borderSize, borderColor),
        colors = ButtonDefaults.buttonColors(
            contentColor = textColor,
            containerColor = backgroundColor,
        )
    ) {
        Text(
            text = text,
            style = textStyle,
            fontFamily = Geist_Font,
        )
    }
}