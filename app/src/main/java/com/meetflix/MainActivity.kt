package com.meetflix

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meetflix.core.ui.components.MFXHeader
import com.meetflix.core.ui.components.MFXTabBar
import com.meetflix.core.ui.components.MFXTabItem
import com.meetflix.core.ui.theme.meetflixTheme
import com.meetflix.home.presentation.screen.HomeScreen
import com.meetflix.party.presentation.screen.HostPartyScreen
import com.meetflix.party.presentation.screen.JoinPartyScreen
import com.meetflix.profile.presentation.screen.ProfileScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            meetflixTheme {
                val tabs = listOf(
                    MFXTabItem(R.string.tab_home, R.drawable.ic_tab_home),
                    MFXTabItem(R.string.tab_host_party, R.drawable.ic_tab_party),
                    MFXTabItem(R.string.tab_search_party, R.drawable.wifi_icon)
                )
                var selectedTabIndex by remember { mutableIntStateOf(0) }

                fun tabHandler(index: Int) {
                    selectedTabIndex = index
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black,
                    topBar = {
                        MFXHeader(
                            onProfileClick = { tabHandler(3) }
                        )
                    },
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
                        )
                    }
                ) { innerPadding ->
                    Crossfade(
                        targetState = selectedTabIndex,
                        label = stringResource(R.string.main_tab_transition)
                    ) { tab ->
                        when (tab) {
                            0 -> HomeScreen(innerPadding)
                            1 -> HostPartyScreen(innerPadding)
                            2 -> JoinPartyScreen(innerPadding)
                            3 -> ProfileScreen(innerPadding)
                        }
                    }
                }
            }
        }
    }
}
