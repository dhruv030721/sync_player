package com.meetflix.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.core.ui.theme.DarkGrayColor
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.core.ui.theme.PrimaryBtnAlpha
import com.meetflix.core.ui.theme.PrimaryColor

@Composable
fun MFXTabBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    tabHandler: (Int) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = DarkGrayColor.copy(0.5f),
            divider = {},
            indicator = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selectedTabIndex == index,
                    onClick = { tabHandler(index) },
                    text = {
                        Text(
                            title,
                            fontSize = 14.sp,
                            fontFamily = Geist_Font,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .then(
                            if (selectedTabIndex == index) {
                                Modifier
                                    .clip(
                                        RoundedCornerShape(12.dp)
                                    )
                                    .background(PrimaryColor.copy(PrimaryBtnAlpha))
                                    .border(1.dp, PrimaryColor, RoundedCornerShape(12.dp))
                            } else {
                                Modifier
                                    .background(Color.Transparent)
                                    .clip(RoundedCornerShape(12.dp))
                            }
                        )
                )
            }
        }
    }
}