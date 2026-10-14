package com.comp90018.flashcards.testutil

import com.comp90018.flashcards.data.local.entity.CardEntity
import com.comp90018.flashcards.data.local.entity.DeckEntity
import com.comp90018.flashcards.data.local.model.DeckWithCardCount
import com.comp90018.flashcards.domain.fsrs.FsrsCard
import com.comp90018.flashcards.domain.model.CardWithState
import com.comp90018.flashcards.domain.model.Rating
import com.comp90018.flashcards.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// one deck with some cards, which is all the game screens read, the rest of the repository is not used
class FakeCardRepository(
    private val deck: DeckEntity? = null,
    private val cards: List<CardEntity> = emptyList(),
) : CardRepository {
    override suspend fun getDeckById(deckId: String): DeckEntity? = deck

    override fun getCardsByDeckId(deckId: String): Flow<List<CardEntity>> = flowOf(cards)

    override fun getAllDecks(ownerId: String): Flow<List<DeckEntity>> = error("not used")

    override fun getDecksWithCardCounts(ownerId: String): Flow<List<DeckWithCardCount>> = error("not used")

    override suspend fun insertDeck(deck: DeckEntity) = error("not used")

    override suspend fun updateDeck(deck: DeckEntity) = error("not used")

    override suspend fun deleteDeck(deck: DeckEntity) = error("not used")

    override fun getDueCards(
        userId: String,
        deckId: String,
    ): Flow<List<CardWithState>> = error("not used")

    override suspend fun getCardById(cardId: String): CardEntity? = error("not used")

    override suspend fun insertCard(card: CardEntity) = error("not used")

    override suspend fun updateCard(card: CardEntity) = error("not used")

    override suspend fun deleteCard(card: CardEntity) = error("not used")

    override suspend fun reviewCard(
        userId: String,
        cardId: String,
        rating: Rating,
    ): FsrsCard = error("not used")
}
