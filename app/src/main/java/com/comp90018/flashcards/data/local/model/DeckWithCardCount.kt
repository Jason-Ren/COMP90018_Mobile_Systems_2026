package com.comp90018.flashcards.data.local.model

import androidx.room.Embedded
import com.comp90018.flashcards.data.local.entity.DeckEntity

/** A deck with its current local card count; the count is computed rather than stored. */
data class DeckWithCardCount(
    @Embedded val deck: DeckEntity,
    val cardCount: Int,
)
