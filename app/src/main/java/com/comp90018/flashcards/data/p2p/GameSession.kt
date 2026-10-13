package com.comp90018.flashcards.data.p2p

import com.comp90018.flashcards.data.local.entity.CardEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** what one phone needs to play a team mode game: its own cards in its own order, and the countdown */
data class GameSetup(
    val deckName: String,
    val secondsPerCard: Int,
    val cards: List<CardEntity>,
)

/**
 * Hands the game setup from the lobby (or join screen) to the game screen.
 *
 * App wide, like the connection, because the screens before and after the game are different
 * destinations and cannot share a ViewModel.
 */
@Singleton
class GameSession
    @Inject
    constructor() {
        var setup: GameSetup? = null
    }

/** the guest rebuilds cards from the text it was sent, with made up ids since they only live for this game */
internal fun GameMessage.Start.toSetup(): GameSetup =
    GameSetup(
        deckName = deckName,
        secondsPerCard = secondsPerCard,
        cards =
            cards.mapIndexed { index, card ->
                CardEntity(cardId = "team-$index", deckId = "", front = card.front, back = card.back)
            },
    )

/**
 * Gives each team its own random order of the deck, so the two teams are never on the same card
 * at the same time.
 *
 * One shuffle is dealt to the first team, and the second team gets the same shuffle turned half
 * way round. Every position then holds a different card, as long as the deck has two or more.
 */
internal fun <T> dealOrders(
    cards: List<T>,
    random: Random = Random,
): Pair<List<T>, List<T>> {
    val first = cards.shuffled(random)
    val half = first.size / 2
    return first to (first.drop(half) + first.take(half))
}
