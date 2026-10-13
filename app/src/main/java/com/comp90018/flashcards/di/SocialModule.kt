package com.comp90018.flashcards.di

import android.content.Context
import com.comp90018.flashcards.R
import com.comp90018.flashcards.data.social.CopyStore
import com.comp90018.flashcards.data.social.FirebaseSocialSession
import com.comp90018.flashcards.data.social.LocalCopyStore
import com.comp90018.flashcards.data.social.SocialApi
import com.comp90018.flashcards.data.social.SocialSession
import com.comp90018.flashcards.data.social.SocialTransport
import com.comp90018.flashcards.data.social.UrlSocialTransport
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SocialModule {
    @Binds abstract fun session(implementation: FirebaseSocialSession): SocialSession

    @Binds abstract fun transport(implementation: UrlSocialTransport): SocialTransport

    @Binds abstract fun copies(implementation: LocalCopyStore): CopyStore

    companion object {
        @Provides
        @Singleton
        fun api(
            @ApplicationContext context: Context,
            session: SocialSession,
            transport: SocialTransport,
        ): SocialApi = SocialApi(context.getString(R.string.social_api_base_url), session, transport)
    }
}
