package com.comp90018.flashcards.data.p2p

import kotlinx.serialization.Serializable

/** everything the two phones say to each other once connected */
@Serializable
sealed interface GameMessage {
    // host tells the guest the game is starting, seed keeps the card order the same on both phones
    @Serializable
    data class Start(
        val seed: Long,
    ) : GameMessage

    // one side left on purpose, so the other side should not wait for a reconnect
    @Serializable
    data object EndSession : GameMessage

    // later: deck data, answers, scores
}
