package com.comp90018.flashcards.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val deckId: String,
    val name: String,
    val description: String = "",
    val ownerId: String,
    /** Wall-clock millis when the deck was first created; My Decks sorts newest first. */
    val createdAtEpochMillis: Long = 0L,
)
