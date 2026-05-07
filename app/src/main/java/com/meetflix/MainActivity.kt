package com.meetflix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meetflix.connections.presentation.screen.ConnectionDiscoveryScreen
import com.meetflix.core.ui.components.MFXTabBar
import com.meetflix.home.presentation.screen.HomeScreen
import com.meetflix.core.ui.theme.meetflixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val tabs = listOf<String>("Home", "Nearby", "Upload", "Profile")

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            meetflixTheme {
                var selectedTabIndex by remember { mutableIntStateOf(0) }

                fun tabHandler(index: Int) {
                    selectedTabIndex = index
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black,
                    bottomBar = {
                        MFXTabBar(
                            tabs,
                            selectedTabIndex,
                            tabHandler = { index -> tabHandler(index) },
                            modifier = Modifier
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                                )
                                .padding(20.dp)
                                .background(
                                    color = Color.Transparent
                                )
                                .clip(
                                    RoundedCornerShape(16.dp)
                                )
                                .border(1.dp, Color(0xFF303030), RoundedCornerShape(16.dp))
                        )
                    }
                ) { innerPadding ->
                    Crossfade(targetState = selectedTabIndex, label = "") { tab ->
                        when (tab) {
                            0 -> HomeScreen(innerPadding)
                            1 -> ConnectionDiscoveryScreen(innerPadding)
                        }
                    }
                }
            }
        }
    }
}
