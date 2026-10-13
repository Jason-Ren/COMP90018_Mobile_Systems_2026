package com.comp90018.flashcards.ui.deck

import androidx.lifecycle.SavedStateHandle
import com.comp90018.flashcards.data.local.dao.DeckDao
import com.comp90018.flashcards.data.social.CommunityDecks
import com.comp90018.flashcards.data.social.SocialApiException
import com.comp90018.flashcards.data.social.SocialDeck
import com.comp90018.flashcards.data.social.SocialSession
import com.comp90018.flashcards.ui.social.SocialViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DeckPublicationViewModel
    @Inject
    constructor(
        private val decks: CommunityDecks,
        private val local: DeckDao,
        savedState: SavedStateHandle,
        session: SocialSession,
    ) : SocialViewModel(session) {
        private val deckId: String = checkNotNull(savedState["deckId"])
        private val state = MutableStateFlow<SocialDeck?>(null)
        val deck = state.asStateFlow()
        private val pendingConfirmation = MutableStateFlow<Boolean?>(null)
        val confirmation = pendingConfirmation.asStateFlow()

        fun requestConfirmation() {
            state.value?.let { pendingConfirmation.value = it.visibility != "public" }
        }

        fun dismissConfirmation() {
            pendingConfirmation.value = null
        }

        fun refresh() =
            runAction {
                state.value = null
                val owned = local.getDeckById(deckId)
                check(owned?.ownerId == userId) { "Only the owner can publish this deck." }
                val cloud =
                    try {
                        decks.get(userId, deckId)
                    } catch (error: SocialApiException) {
                        if (error.status != 404 || error.code != "deck_not_found") throw error
                        // A new local deck can be published after the existing sync workflow uploads it.
                        SocialDeck(deckId, checkNotNull(owned).name, userId, "private", 0)
                    }
                check(cloud.ownerId == userId) { "Only the owner can publish this deck." }
                state.value = cloud
            }

        fun publish(public: Boolean) =
            runAction(
                if (public) "Deck published." else "Deck is private. Friend shares remain.",
            ) {
                check(state.value?.ownerId == userId) { "Refresh the deck before publishing." }
                state.value = decks.publish(userId, deckId, public)
            }
    }
