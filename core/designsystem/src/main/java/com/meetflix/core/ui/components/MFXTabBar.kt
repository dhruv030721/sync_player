package com.meetflix.core.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.core.ui.theme.DarkGrayColor
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.core.ui.theme.PrimaryColor
import com.meetflix.core.ui.theme.blackWhiteGradient

data class MFXTabItem(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
)

@Composable
fun MFXTabBar(
    tabs: List<MFXTabItem>,
    selectedTabIndex: Int,
    tabHandler: (Int) -> Unit,
    modifier: Modifier
) {
    val cornerRadius = RoundedCornerShape(50.dp)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = modifier
                .shadow(
                elevation = 18.dp,
                shape = cornerRadius,
                ambientColor = PrimaryColor.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.45f))
                .clip(cornerRadius)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            DarkGrayColor.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.42f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = cornerRadius
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTabIndex == index
                val contentColor = if (selected) Color.LightGray else Color(0xFFB9B9C3)

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .then(
                            if (selected) {
                                Modifier
                                    .background(
                                        brush = blackWhiteGradient,
                                        shape = cornerRadius
                                    )
                                    .border(
                                        width = 0.8.dp,
                                        color = Color.White.copy(alpha = 0.22f),
                                        shape = cornerRadius
                                    )
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clip(cornerRadius)
                        .clickable { tabHandler(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(id = tab.titleRes),
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                            fontFamily = Geist_Font,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
