package com.meetflix.di

import android.content.Context
import com.meetflix.connections.data.NearbyConnectionsManager
import com.meetflix.party.data.NearbyRepository
import com.meetflix.player.data.ExoPlayerManager
import com.meetflix.player.data.PlayerSyncManager
import com.meetflix.stream.data.LocalMediaServer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Design Pattern: Factory / Module - Hilt Dependency Injection Module providing core singletons across features.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Design Pattern: Singleton - Provides application-wide NearbyRepository instance */
    @Provides
    @Singleton
    fun provideNearbyRepository(
        @ApplicationContext context: Context
    ): NearbyRepository {
        return NearbyRepository.getInstance(context)
    }

    /** Design Pattern: Singleton - Provides NearbyConnectionsManager instance */
    @Provides
    @Singleton
    fun provideNearbyConnectionsManager(
        repository: NearbyRepository
    ): NearbyConnectionsManager {
        return repository.nearbyConnectionsManager
    }

    /** Design Pattern: Singleton - Provides ExoPlayerManager instance */
    @Provides
    @Singleton
    fun provideExoPlayerManager(
        repository: NearbyRepository
    ): ExoPlayerManager {
        return repository.exoPlayerManager
    }

    /** Design Pattern: Singleton - Provides PlayerSyncManager instance */
    @Provides
    @Singleton
    fun providePlayerSyncManager(
        repository: NearbyRepository
    ): PlayerSyncManager {
        return repository.playerSyncManager
    }

    /** Design Pattern: Singleton - Provides LocalMediaServer instance */
    @Provides
    @Singleton
    fun provideLocalMediaServer(
        repository: NearbyRepository
    ): LocalMediaServer {
        return repository.localMediaServer
    }
}
