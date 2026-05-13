package com.meetflix.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.meetflix.core.ui.theme.DarkGrayColor
import com.meetflix.core.ui.theme.Geist_Font

@Composable
fun MFXButton(
    text: String,
    modifier: Modifier,
    icon: Int? = null,
    iconSize: Dp = 20.dp,
    height: Dp = 50.dp,
    textColor: Color = Color.White,
    textStyle: TextStyle = TextStyle(fontSize = 16.sp),
    onClick: () -> Unit
) {
    Button(
        onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        colors = ButtonDefaults.buttonColors(
            contentColor = textColor,
            containerColor = Color.Transparent,
        )
    ) {
        Text(
            text = text,
            style = textStyle,
            fontFamily = Geist_Font,
        )
        icon?.let {
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                painter = painterResource(id = it),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = textColor
            )
        }

    }
}
