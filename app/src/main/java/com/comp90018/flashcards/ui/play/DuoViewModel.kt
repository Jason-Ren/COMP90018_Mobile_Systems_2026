package com.comp90018.flashcards.ui.play

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.audio.AnswerSounds
import com.comp90018.flashcards.data.local.entity.CardEntity
import com.comp90018.flashcards.data.vibration.GameVibration
import com.comp90018.flashcards.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the 2-player screen.
 */
data class DuoUiState(
    val isLoading: Boolean = true,
    val deckName: String = "",
    val phase: DuoPhase = DuoPhase.READY,
    val current: CardEntity? = null,
    val secondsPerCard: Int = 0,
    val secondsLeft: Int = 0,
    val isPaused: Boolean = false,
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val missed: List<CardEntity> = emptyList(),
)

/**
 * ViewModel for a 2-player round on one phone. The round itself is in [GameViewModel], this loads
 * the deck and adds playing again.
 */
@HiltViewModel
class DuoViewModel
    @Inject
    constructor(
        private val repository: CardRepository,
        savedStateHandle: SavedStateHandle,
        // handed to the shared round, which plays it on every marked guess
        sounds: AnswerSounds,
        vibration: GameVibration,
        // how long the Guesser has for each card, picked on the choose-countdown page
    ) : GameViewModel(
            secondsPerCard = checkNotNull(savedStateHandle.get<Int>("seconds")),
            sounds = sounds,
            vibration = vibration,
        ) {
        // The deckId is passed via navigation arguments
        private val deckId: String = checkNotNull(savedStateHandle["deckId"])

        val uiState: StateFlow<DuoUiState> = round

        private var cards: List<CardEntity> = emptyList()

        init {
            viewModelScope.launch {
                val deckName = repository.getDeckById(deckId)?.name.orEmpty()
                cards = repository.getCardsByDeckId(deckId).first()
                // Shuffled so every round comes up in a different order.
                loadCards(cards.shuffled(), deckName)
            }
        }

        /** Starts a new round with the same cards, in a new order, and the same countdown. */
        fun playAgain() {
            loadCards(cards.shuffled(), round.value.deckName)
            start()
        }
    }
