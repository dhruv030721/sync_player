package com.meetflix.party.data

import android.content.Context
import com.meetflix.connections.data.NearbyConnectionsManager
import com.meetflix.player.data.ExoPlayerManager
import com.meetflix.player.data.PlayerSyncManager
import com.meetflix.stream.data.LocalMediaServer

/**
 * Design Pattern: Singleton / Repository
 * Centralized repository providing shared application-wide singletons for NearbyConnectionsManager,
 * ExoPlayerManager, PlayerSyncManager, and LocalMediaServer to prevent state fragmentation.
 */
class NearbyRepository private constructor(context: Context) {

    val localMediaServer = LocalMediaServer(context.applicationContext).apply { start() }
    val nearbyConnectionsManager = NearbyConnectionsManager(context.applicationContext)
    val exoPlayerManager = ExoPlayerManager(context.applicationContext)
    val playerSyncManager = PlayerSyncManager(exoPlayerManager, nearbyConnectionsManager)

    companion object {
        @Volatile
        private var INSTANCE: NearbyRepository? = null

        fun getInstance(context: Context): NearbyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NearbyRepository(context).also { INSTANCE = it }
            }
        }
    }
}
