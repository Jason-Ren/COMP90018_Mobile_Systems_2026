package com.comp90018.flashcards.testutil

import com.comp90018.flashcards.data.local.entity.CardEntity

fun testCard(index: Int) = CardEntity(cardId = "c$index", deckId = "deck", front = "front $index", back = "back $index")

fun testCards(count: Int) = List(count) { testCard(it + 1) }
