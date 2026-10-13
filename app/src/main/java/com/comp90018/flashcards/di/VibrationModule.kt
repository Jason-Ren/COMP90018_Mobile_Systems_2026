package com.comp90018.flashcards.di

import com.comp90018.flashcards.data.vibration.GameVibration
import com.comp90018.flashcards.data.vibration.SystemGameVibration
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// tells hilt which class to hand out when something asks for the game vibration
@Module
@InstallIn(SingletonComponent::class)
abstract class VibrationModule {
    // one shared instance, so the vibrator is looked up once for the whole app
    @Binds
    @Singleton
    abstract fun bindGameVibration(impl: SystemGameVibration): GameVibration
}
