package com.comp90018.flashcards.ui.social

import com.comp90018.flashcards.data.social.FriendsRepository
import com.comp90018.flashcards.data.social.SocialFriend
import com.comp90018.flashcards.data.social.SocialProfile
import com.comp90018.flashcards.data.social.SocialRequest
import com.comp90018.flashcards.data.social.SocialSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class FriendsState(
    val loaded: Boolean = false,
    val profile: SocialProfile? = null,
    val friends: List<SocialFriend> = emptyList(),
    val incoming: List<SocialRequest> = emptyList(),
    val outgoing: List<SocialRequest> = emptyList(),
)

@HiltViewModel
class FriendsViewModel
    @Inject
    constructor(
        private val repository: FriendsRepository,
        session: SocialSession,
    ) : SocialViewModel(session) {
        private val state = MutableStateFlow(FriendsState())
        val uiState = state.asStateFlow()

        init {
            refresh()
        }

        fun refresh() = runAction { load() }

        fun addFriend(username: String) =
            runAction("Friend request sent.") {
                repository.addFriend(userId, username)
                load()
            }

        fun respond(
            requestId: String,
            action: String,
        ) = runAction("Request updated.") {
            repository.respond(userId, requestId, action)
            load()
        }

        fun remove(friendId: String) =
            runAction("Friend removed. Private sharing access has ended.") {
                repository.remove(userId, friendId)
                load()
            }

        private suspend fun load() {
            val profile = repository.profile(userId)
            state.value =
                if (profile == null) {
                    FriendsState(loaded = true)
                } else {
                    FriendsState(
                        true,
                        profile,
                        repository.friends(userId),
                        repository.requests(userId, true),
                        repository.requests(userId, false),
                    )
                }
        }
    }
