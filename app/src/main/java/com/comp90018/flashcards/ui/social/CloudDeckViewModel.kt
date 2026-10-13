package com.comp90018.flashcards.ui.social

import androidx.lifecycle.SavedStateHandle
import com.comp90018.flashcards.data.social.CommunityDecks
import com.comp90018.flashcards.data.social.DeckCopies
import com.comp90018.flashcards.data.social.FriendsRepository
import com.comp90018.flashcards.data.social.SocialCard
import com.comp90018.flashcards.data.social.SocialDeck
import com.comp90018.flashcards.data.social.SocialFriend
import com.comp90018.flashcards.data.social.SocialSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CloudDeckState(
    val deck: SocialDeck? = null,
    val cards: List<SocialCard> = emptyList(),
    val moreCards: Boolean = false,
    val friends: List<SocialFriend>? = null,
    val savedCopyId: String? = null,
    val pendingCopy: Boolean = false,
)

@HiltViewModel
class CloudDeckViewModel
    @Inject
    constructor(
        private val decks: CommunityDecks,
        private val copies: DeckCopies,
        private val friends: FriendsRepository,
        private val savedState: SavedStateHandle,
        session: SocialSession,
    ) : SocialViewModel(session) {
        val deckId: String = checkNotNull(savedState["deckId"])
        private val state = MutableStateFlow(CloudDeckState(savedCopyId = savedState["savedCopyId"]))
        val uiState = state.asStateFlow()

        init {
            refresh()
        }

        fun refresh() =
            runAction {
                state.update { it.copy(deck = null, cards = emptyList(), friends = null) }
                val pending = copies.hasPending(userId, deckId)
                state.update { it.copy(pendingCopy = pending) }
                val deck = decks.prepare(userId, deckId)
                val cards = decks.cards(userId, deckId, 0)
                state.update { it.copy(deck = deck, cards = cards, moreCards = cards.size == 50) }
            }

        fun moreCards() =
            runAction {
                val page = decks.cards(userId, deckId, state.value.cards.size)
                state.update {
                    it.copy(
                        cards = (it.cards + page).distinctBy(SocialCard::cardId),
                        moreCards =
                            page.size == 50,
                    )
                }
            }

        fun publish(public: Boolean) =
            runAction(if (public) "Deck published." else "Deck is private. Friend shares remain.") {
                val deck = decks.publish(userId, deckId, public)
                state.update { it.copy(deck = deck) }
            }

        fun loadFriends() =
            runAction {
                val rows = friends.friends(userId)
                state.update { it.copy(friends = rows) }
            }

        fun share(
            recipientId: String,
            revoke: Boolean,
        ) = runAction(
            if (revoke) {
                "Share revoked. Public decks are still visible to everyone signed in."
            } else {
                "Shared. Send your friend the deck code so they can open it in Community."
            },
        ) {
            if (revoke) decks.revoke(userId, deckId, recipientId) else decks.share(userId, deckId, recipientId)
        }

        fun saveCopy() =
            runAction("Copy saved to My Decks with fresh study progress.") {
                state.update { it.copy(pendingCopy = true) }
                val id = copies.save(userId, deckId)
                savedState["savedCopyId"] = id
                state.update { it.copy(savedCopyId = id, pendingCopy = false) }
            }
    }
