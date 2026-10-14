package com.comp90018.flashcards.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.auth.AuthRepository
import com.comp90018.flashcards.data.auth.AuthState
import com.comp90018.flashcards.data.social.FriendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One foreground poller for the signed-in navigation host, shared by all bottom bars. */
@HiltViewModel
class FriendRequestBadgeViewModel
    @Inject
    constructor(
        private val repository: FriendsRepository,
        authRepository: AuthRepository,
    ) : ViewModel() {
        @OptIn(ExperimentalCoroutinesApi::class)
        val incomingCount =
            authRepository.authState
                .flatMapLatest { state ->
                    val uid = (state as? AuthState.SignedIn)?.user?.uid
                    if (uid == null) flowOf(0) else observeIncoming(uid)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0, 0), 0)

        @Suppress("TooGenericExceptionCaught")
        private fun observeIncoming(uid: String) =
            channelFlow {
                launch {
                    while (isActive) {
                        try {
                            repository.requests(uid, incoming = true)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // Keep the last known count offline; the next foreground tick retries.
                        }
                        delay(POLL_INTERVAL_MS)
                    }
                }
                repository.incomingRequestCount(uid).collect { send(it) }
            }

        private companion object {
            const val POLL_INTERVAL_MS = 3_000L
        }
    }
