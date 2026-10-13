package com.comp90018.flashcards.ui.social

import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.social.FriendRequestWatcher
import com.comp90018.flashcards.data.social.FriendsRepository
import com.comp90018.flashcards.data.social.SocialFriend
import com.comp90018.flashcards.data.social.SocialProfile
import com.comp90018.flashcards.data.social.SocialRequest
import com.comp90018.flashcards.data.social.SocialSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsState(
    val loaded: Boolean = false,
    val profile: SocialProfile? = null,
    val friends: List<SocialFriend> = emptyList(),
    val incoming: List<SocialRequest> = emptyList(),
    val outgoing: List<SocialRequest> = emptyList(),
    val notice: String? = null,
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
        private val watcher = FriendRequestWatcher()

        init {
            refresh()
            viewModelScope.launch {
                while (isActive) {
                    delay(POLL_INTERVAL_MS)
                    poll()
                }
            }
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
            if (action == "cancel") watcher.ignore(requestId)
            repository.respond(userId, requestId, action)
            load()
        }

        fun remove(friendId: String) =
            runAction("Friend removed. Private sharing access has ended.") {
                repository.remove(userId, friendId)
                load()
            }

        @Suppress("TooGenericExceptionCaught")
        private suspend fun poll() {
            if (userId.isBlank() || taskState.value.busy) return
            try {
                load(notify = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // keep the last snapshot; the next tick retries
            }
        }

        private suspend fun load(notify: Boolean = false) {
            val profile = repository.profile(userId)
            if (profile == null) {
                state.value = FriendsState(loaded = true, notice = state.value.notice)
                return
            }
            val friends = repository.friends(userId)
            val incoming = repository.requests(userId, true)
            val outgoing = repository.requests(userId, false)
            val declined =
                if (notify && watcher.needsDeclined(outgoing, friends)) {
                    repository.requests(userId, false, "declined")
                } else {
                    emptyList()
                }
            val notices = watcher.consume(outgoing, friends, declined, notify)
            state.value =
                FriendsState(
                    true,
                    profile,
                    friends,
                    incoming,
                    outgoing,
                    notices.joinToString("\n").ifEmpty { state.value.notice },
                )
        }

        private companion object {
            const val POLL_INTERVAL_MS = 3_000L
        }
    }
