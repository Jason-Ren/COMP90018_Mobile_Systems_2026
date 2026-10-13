package com.comp90018.flashcards.di

import com.comp90018.flashcards.data.audio.AnswerSounds
import com.comp90018.flashcards.data.audio.SoundPoolAnswerSounds
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// tells hilt which class to hand out when something asks for the answer sounds
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    // one shared instance, so the sounds are loaded once for the whole app
    @Binds
    @Singleton
    abstract fun bindAnswerSounds(impl: SoundPoolAnswerSounds): AnswerSounds
}
