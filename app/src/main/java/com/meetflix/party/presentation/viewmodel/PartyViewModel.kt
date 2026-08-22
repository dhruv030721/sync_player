package com.meetflix.party.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.meetflix.connections.data.NearbyConnectionsManager
import com.meetflix.connections.data.NearbyRepository
import com.meetflix.connections.domain.models.AvailableHost
import com.meetflix.party.domain.models.PartyModeState
import com.meetflix.party.domain.models.PartyScreenMode
import com.meetflix.player.data.ExoPlayerManager
import com.meetflix.player.data.PlayerSyncManager
import com.meetflix.player.domain.models.PlaybackAction
import com.meetflix.player.presentation.viewmodel.PlayerViewModel
import com.meetflix.player.presentation.viewmodel.PlayerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Design Pattern: Observer / ViewModel (MVI State Machine) / ViewModelStoreOwner
 * Central ViewModel consuming application-wide NearbyRepository singletons to manage Party Mode lifecycle,
 * host/guest discovery, P2P video streaming, and synchronized ExoPlayer playback state.
 */
class PartyViewModel(application: Application) : AndroidViewModel(application), ViewModelStoreOwner {

    private val internalViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = internalViewModelStore

    private val repository = NearbyRepository.getInstance(application)
    val nearbyConnectionsManager: NearbyConnectionsManager = repository.nearbyConnectionsManager
    val exoPlayerManager: ExoPlayerManager = repository.exoPlayerManager
    val playerSyncManager: PlayerSyncManager = repository.playerSyncManager

    /** Design Pattern: Factory - PlayerViewModel instantiated via ViewModelProvider with PlayerViewModelFactory */
    val playerViewModel: PlayerViewModel = ViewModelProvider(
        this,
        PlayerViewModelFactory(exoPlayerManager, playerSyncManager)
    )[PlayerViewModel::class.java]

    private val _uiState = MutableStateFlow(PartyModeState())
    val uiState: StateFlow<PartyModeState> = _uiState.asStateFlow()

    init {
        // Observe changes from NearbyConnectionsManager to update PartyModeState
        nearbyConnectionsManager.uiState
            .onEach { nearbyState ->
                _uiState.update { current ->
                    val activeUri = nearbyState.receivedVideoUri ?: current.selectedMediaUri ?: exoPlayerManager.playerState.value.mediaUri
                    val nextMode = when {
                        activeUri != null -> PartyScreenMode.WATCHING
                        nearbyState.isAdvertising -> PartyScreenMode.HOSTING
                        nearbyState.isDiscovering -> PartyScreenMode.DISCOVERING
                        else -> PartyScreenMode.IDLE
                    }

                    current.copy(
                        mode = nextMode,
                        selectedMediaUri = activeUri,
                        hasRequiredPermissions = nearbyState.hasRequiredPermissions,
                        localDeviceName = nearbyState.localEndpointName,
                        discoveredParties = nearbyState.discoveredHosts,
                        connectedMembers = nearbyState.connectedHosts,
                        isTransferring = nearbyState.isTransferring,
                        transferProgress = nearbyState.transferProgress,
                        transferStatusMessage = nearbyState.transferStatusMessage,
                        statusMessage = nearbyState.statusMessage ?: current.statusMessage
                    )
                }
            }
            .launchIn(viewModelScope)

        // Observe player state changes
        exoPlayerManager.playerState
            .onEach { playerState ->
                if (playerState.mediaUri != null) {
                    _uiState.update { it.copy(mode = PartyScreenMode.WATCHING, selectedMediaUri = playerState.mediaUri) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun updatePermissionsState(hasPermissions: Boolean) {
        nearbyConnectionsManager.updatePermissionsState(hasPermissions)
    }

    fun launchParty(videoUri: Uri, videoTitle: String = "Watch Party") {
        val title = if (videoTitle.isNotBlank()) videoTitle else "Watch Party"
        nearbyConnectionsManager.setHostVideoUri(videoUri)
        val localMediaServer = repository.localMediaServer
        localMediaServer.setMediaUri(videoUri)
        val streamUrl = localMediaServer.getStreamUrl()

        _uiState.update {
            it.copy(
                mode = PartyScreenMode.HOSTING,
                activePartyTitle = title,
                selectedMediaUri = streamUrl
            )
        }
        nearbyConnectionsManager.startAdvertising(customName = "${nearbyConnectionsManager.uiState.value.localEndpointName}'s Party")
        playerViewModel.onAction(PlaybackAction.LoadMedia(streamUrl, title))

        nearbyConnectionsManager.uiState.value.connectedHosts.forEach { host ->
            nearbyConnectionsManager.sendPayload(host.endpointId, "STREAM_URL:$streamUrl")
            nearbyConnectionsManager.sendVideoStreamPayload(host.endpointId, videoUri)
        }
    }

    fun broadcastVideoStream(videoUri: Uri) {
        nearbyConnectionsManager.setHostVideoUri(videoUri)
        val localMediaServer = repository.localMediaServer
        localMediaServer.setMediaUri(videoUri)
        val streamUrl = localMediaServer.getStreamUrl()

        _uiState.update { it.copy(selectedMediaUri = streamUrl) }
        playerViewModel.onAction(PlaybackAction.LoadMedia(streamUrl, "Stream Video"))

        nearbyConnectionsManager.uiState.value.connectedHosts.forEach { host ->
            nearbyConnectionsManager.sendPayload(host.endpointId, "STREAM_URL:$streamUrl")
            nearbyConnectionsManager.sendVideoStreamPayload(host.endpointId, videoUri)
        }
    }

    fun startDiscovery() {
        _uiState.update { it.copy(mode = PartyScreenMode.DISCOVERING) }
        nearbyConnectionsManager.startDiscovery()
    }

    fun stopDiscovery() {
        nearbyConnectionsManager.stopDiscovery()
        _uiState.update { it.copy(mode = PartyScreenMode.IDLE) }
    }

    fun joinParty(host: AvailableHost) {
        _uiState.update { it.copy(mode = PartyScreenMode.JOINING) }
        nearbyConnectionsManager.connectToEndpoint(host)
    }

    fun leaveParty() {
        nearbyConnectionsManager.stopAll()
        _uiState.update { PartyModeState(hasRequiredPermissions = it.hasRequiredPermissions, localDeviceName = it.localDeviceName) }
    }

    fun onPlaybackAction(action: PlaybackAction) {
        playerViewModel.onAction(action)
    }

    override fun onCleared() {
        super.onCleared()
        internalViewModelStore.clear()
    }
}
