package com.example.meetflix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.meetflix.ui.components.MFXTabBar
import com.example.meetflix.ui.screens.HomeScreen
import com.example.meetflix.ui.theme.DarkGrayColor
import com.example.meetflix.ui.theme.meetflixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val tabs = listOf<String>("Upload", "Connect")

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            meetflixTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black,
                    bottomBar = {
                        MFXTabBar(
                            tabs,
                            modifier = Modifier.windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                            ).padding(20.dp).background(
                                color = Color.Transparent
                            ).clip(
                                RoundedCornerShape(16.dp)
                            )
                        )
                    }
                ) { innerPadding ->
                    HomeScreen(innerPadding)
                }
            }
        }
    }
}
