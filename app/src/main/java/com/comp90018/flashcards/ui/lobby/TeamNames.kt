package com.comp90018.flashcards.ui.lobby


private val TEAM_ADJECTIVES =
    listOf("Cute", "Smart", "Flying", "Lazy", "Bright", "Funny", "Awesome", "Cool", "Curious", "Wise")
private val TEAM_NOUNS =
    listOf("Mango", "Kiwi", "Plum", "Otter", "Comet", "Pixel", "Maple", "Falcon", "Cactus", "Lemon")

// shared by the host lobby and the join screen
internal fun randomTeamName(): String = "Team ${TEAM_ADJECTIVES.random()} ${TEAM_NOUNS.random()}"
