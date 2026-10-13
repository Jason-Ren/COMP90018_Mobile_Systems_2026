package com.comp90018.flashcards.di

import com.comp90018.flashcards.data.p2p.ConnectionController
import com.comp90018.flashcards.data.p2p.NearbyConnectionController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// tells hilt which class to hand out when something asks for the connection controller
@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectionModule {
    // one shared instance for the whole app, so the lobby and the game use the same live connection
    @Binds
    @Singleton
    abstract fun bindConnectionController(impl: NearbyConnectionController): ConnectionController
}
