package com.example.meetflix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meetflix.ui.theme.DarkGrayColor
import com.example.meetflix.ui.theme.Geist_Font
import com.example.meetflix.ui.theme.PrimaryBtnAlpha
import com.example.meetflix.ui.theme.PrimaryColor

@Composable
fun MFXTabBar(
    tabs: List<String>,
    modifier: Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = DarkGrayColor,
            divider = {},
            indicator = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selectedTabIndex == index,
                    onClick = { selectedTabIndex == index },
                    text = {
                        Text(
                            title,
                            fontSize = 16.sp,
                            fontFamily = Geist_Font,
                            color = Color.White
                        )
                    },
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .then(
                            if (selectedTabIndex == index) {
                                Modifier
                                    .clip(
                                        RoundedCornerShape(16.dp)
                                    )
                                    .background(PrimaryColor.copy(PrimaryBtnAlpha))
                                    .border(1.dp, PrimaryColor, RoundedCornerShape(16.dp))
                            } else {
                                Modifier.background(Color.Transparent).clip(RoundedCornerShape(16.dp))
                            }
                        )
                )
            }
        }
    }
}