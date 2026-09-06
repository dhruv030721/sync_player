package com.meetflix.party.presentation.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.meetflix.connections.domain.models.AvailableHost
import com.meetflix.core.ui.theme.meetflixTheme
import com.meetflix.party.presentation.screen.PartyRoomScreen
import com.meetflix.party.presentation.viewmodel.PartyViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Design Pattern: Clean Architecture Presentation Activity / Hilt Entry Point
 * Dedicated Activity opening when a user launches or joins a party watch room.
 * Manages orientation locking, lifecycle cleanup, and renders PartyRoomScreen.
 */
@AndroidEntryPoint
class PartyRoomActivity : ComponentActivity() {

    private val viewModel: PartyViewModel by viewModels()

    companion object {
        private const val EXTRA_IS_HOST = "extra_is_host"
        private const val EXTRA_VIDEO_URI = "extra_video_uri"
        private const val EXTRA_PARTY_TITLE = "extra_party_title"
        private const val EXTRA_HOST_ENDPOINT_ID = "extra_host_endpoint_id"
        private const val EXTRA_HOST_ENDPOINT_NAME = "extra_host_endpoint_name"

        fun launchAsHost(context: Context, videoUri: Uri, partyTitle: String = "Watch Party") {
            val intent = Intent(context, PartyRoomActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setData(videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(EXTRA_IS_HOST, true)
                putExtra(EXTRA_VIDEO_URI, videoUri.toString())
                putExtra(EXTRA_PARTY_TITLE, partyTitle)
            }
            context.startActivity(intent)
        }

        fun launchAsGuest(context: Context, host: AvailableHost) {
            val intent = Intent(context, PartyRoomActivity::class.java).apply {
                putExtra(EXTRA_IS_HOST, false)
                putExtra(EXTRA_HOST_ENDPOINT_ID, host.endpointId)
                putExtra(EXTRA_HOST_ENDPOINT_NAME, host.endpointName)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide top status bar and navigation bars across all Android & Samsung devices
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
        insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        @Suppress("DEPRECATION")
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val isHost = intent.getBooleanExtra(EXTRA_IS_HOST, false)
        val videoUri = intent.data ?: intent.getStringExtra(EXTRA_VIDEO_URI)?.let { Uri.parse(it) }
        val partyTitle = intent.getStringExtra(EXTRA_PARTY_TITLE) ?: "Watch Party"
        val hostEndpointId = intent.getStringExtra(EXTRA_HOST_ENDPOINT_ID)
        val hostEndpointName = intent.getStringExtra(EXTRA_HOST_ENDPOINT_NAME) ?: "Party Host"

        if (isHost && videoUri != null) {
            viewModel.launchParty(videoUri, partyTitle)
        } else if (!isHost && hostEndpointId != null) {
            val host = AvailableHost(endpointId = hostEndpointId, endpointName = hostEndpointName)
            viewModel.joinParty(host)
        }

        setContent {
            meetflixTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black
                ) { innerPadding ->
                    PartyRoomScreen(
                        viewModel = viewModel,
                        onExitParty = { finish() }
                    )
                }
            }
        }
    }
}
