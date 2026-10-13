package com.comp90018.flashcards.ui.account

import android.content.Context
import com.comp90018.flashcards.data.account.AccountRepository
import com.comp90018.flashcards.data.social.SocialProfile
import com.comp90018.flashcards.data.social.SocialSession
import com.comp90018.flashcards.ui.social.SocialViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AccountViewModel
    @Inject
    constructor(
        private val accounts: AccountRepository,
        session: SocialSession,
    ) : SocialViewModel(session) {
        private val state = MutableStateFlow<SocialProfile?>(null)
        val profile = state.asStateFlow()
        val usesPassword get() = accounts.usesPassword

        init {
            refresh()
        }

        fun refresh() =
            runAction {
                state.value = accounts.cachedProfile(userId)
                if (!accounts.isDeleting(userId)) state.value = accounts.loadProfile(userId)
            }

        fun createProfile(
            username: String,
            name: String,
        ) = runAction {
            state.value = accounts.saveProfile(userId, username, name)
        }

        fun updateName(name: String) =
            runAction("Display name updated.") {
                val current = checkNotNull(state.value) { "Load your profile before saving." }
                state.value = accounts.saveProfile(userId, current.username, name)
            }

        fun deleteAccount(
            context: Context,
            password: String,
        ) = runAction {
            accounts.deleteAccount(context, userId, password)
        }
    }
