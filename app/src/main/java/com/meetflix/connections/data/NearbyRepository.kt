package com.meetflix.connections.data

import android.content.Context
import com.meetflix.player.data.ExoPlayerManager
import com.meetflix.player.data.PlayerSyncManager

/**
 * Design Pattern: Singleton / Repository
 * Centralized repository providing shared application-wide singletons for NearbyConnectionsManager,
 * ExoPlayerManager, and PlayerSyncManager to prevent state fragmentation across activities and viewmodels.
 */
class NearbyRepository private constructor(context: Context) {

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
