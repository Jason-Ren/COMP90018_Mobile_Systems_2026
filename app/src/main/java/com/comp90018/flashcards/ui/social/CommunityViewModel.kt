package com.comp90018.flashcards.ui.social

import androidx.lifecycle.SavedStateHandle
import com.comp90018.flashcards.data.remote.RemoteDeck
import com.comp90018.flashcards.data.social.CommunityDecks
import com.comp90018.flashcards.data.social.DeckCopies
import com.comp90018.flashcards.data.social.SocialSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel
    @Inject
    constructor(
        private val repository: CommunityDecks,
        private val copies: DeckCopies,
        private val savedState: SavedStateHandle,
        session: SocialSession,
    ) : SocialViewModel(session) {
        private val catalogue = MutableStateFlow<List<RemoteDeck>>(emptyList())
        val decks = catalogue.asStateFlow()
        private val copyState = MutableStateFlow(CatalogueCopies(saved = savedState["catalogueCopies"] ?: emptyMap()))
        val copyActions = copyState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() =
            runAction {
                catalogue.value = repository.catalogue(userId)
                val pending =
                    catalogue.value
                        .filter { copies.hasPending(userId, it.deckId) }
                        .map { it.deckId }
                        .toSet()
                copyState.update { it.copy(pending = pending) }
            }

        fun saveCopy(deckId: String) =
            runAction("Saved a copy to My Decks with fresh study progress.") {
                copyState.update { it.copy(saving = deckId, pending = it.pending + deckId) }
                try {
                    val id = copies.save(userId, deckId)
                    copyState.update { it.copy(saved = it.saved + (deckId to id), pending = it.pending - deckId) }
                    savedState["catalogueCopies"] = HashMap(copyState.value.saved)
                } finally {
                    copyState.update { it.copy(saving = null) }
                }
            }
    }

data class CatalogueCopies(
    val saving: String? = null,
    val saved: Map<String, String> = emptyMap(),
    val pending: Set<String> = emptySet(),
)

fun sharedDeckId(text: String): String? {
    val pattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    return pattern.matchEntire(text.trim())?.value?.lowercase()
}
