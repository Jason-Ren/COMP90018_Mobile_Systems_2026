package com.comp90018.flashcards.data.p2p

import kotlinx.serialization.Serializable

// a card sent over the link, just the text because images only exist on the host phone
@Serializable
data class WireCard(
    val front: String,
    val back: String,
)

/** everything the two phones say to each other once connected */
@Serializable
sealed interface GameMessage {
    // host starts the game, the cards are already in the order this phone should play them
    @Serializable
    data class Start(
        val deckName: String,
        val secondsPerCard: Int,
        val cards: List<WireCard>,
    ) : GameMessage

    // a team went through its whole deck, so the other team can compare scores
    @Serializable
    data class Finished(
        val correct: Int,
    ) : GameMessage

    // one side left on purpose, so the other side should not wait for a reconnect
    @Serializable
    data object EndSession : GameMessage
}
