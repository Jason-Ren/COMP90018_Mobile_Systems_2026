package com.comp90018.flashcards.ui.deck

import com.comp90018.flashcards.data.local.entity.CardEntity
import com.comp90018.flashcards.data.social.SocialCard

data class CardUiModel(
    val cardId: String,
    val front: String,
    val back: String,
    val frontImageUri: String? = null,
    val backImageUri: String? = null,
)

fun CardEntity.toUiModel(): CardUiModel =
    CardUiModel(
        cardId = cardId,
        front = front,
        back = back,
        frontImageUri = frontImageUri,
        backImageUri = backImageUri,
    )

fun SocialCard.toUiModel(): CardUiModel =
    CardUiModel(
        cardId = cardId,
        front = front,
        back = back,
        frontImageUri = frontImageUri,
        backImageUri = backImageUri,
    )
