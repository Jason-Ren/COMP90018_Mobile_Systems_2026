package com.comp90018.flashcards.ui.social

import com.comp90018.flashcards.data.remote.RemoteDeck
import com.comp90018.flashcards.data.social.CommunityDecks
import com.comp90018.flashcards.data.social.SocialSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel
    @Inject
    constructor(
        private val repository: CommunityDecks,
        session: SocialSession,
    ) : SocialViewModel(session) {
        private val catalogue = MutableStateFlow<List<RemoteDeck>>(emptyList())
        val decks = catalogue.asStateFlow()

        init {
            refresh()
        }

        fun refresh() = runAction { catalogue.value = repository.catalogue(userId) }
    }

fun sharedDeckId(text: String): String? {
    val pattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    return pattern.matchEntire(text.trim())?.value?.lowercase()
}
