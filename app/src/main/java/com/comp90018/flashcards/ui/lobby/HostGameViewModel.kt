package com.comp90018.flashcards.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.p2p.ConnectionController
import com.comp90018.flashcards.data.p2p.GameMessage
import com.comp90018.flashcards.data.p2p.GameSession
import com.comp90018.flashcards.data.p2p.GameSetup
import com.comp90018.flashcards.data.p2p.WireCard
import com.comp90018.flashcards.data.p2p.dealOrders
import com.comp90018.flashcards.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Starts a team mode game from the host's side once the countdown has been chosen. */
@HiltViewModel
class HostGameViewModel
    @Inject
    constructor(
        private val repository: CardRepository,
        private val connection: ConnectionController,
        private val session: GameSession,
    ) : ViewModel() {
        // deals each team its own order, keeps the host's, sends the guest theirs, then opens the game
        fun startGame(
            deckId: String,
            secondsPerCard: Int,
            onReady: () -> Unit,
        ) {
            viewModelScope.launch {
                val deckName = repository.getDeckById(deckId)?.name.orEmpty()
                val (mine, theirs) = dealOrders(repository.getCardsByDeckId(deckId).first())
                session.setup = GameSetup(deckName, secondsPerCard, mine)
                connection.send(GameMessage.Start(deckName, secondsPerCard, theirs.map { WireCard(it.front, it.back) }))
                onReady()
            }
        }
    }
