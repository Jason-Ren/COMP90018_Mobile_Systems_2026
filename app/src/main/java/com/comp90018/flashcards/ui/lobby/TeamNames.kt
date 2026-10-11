package com.comp90018.flashcards.ui.lobby

private val TEAM_WORDS =
    listOf("Mango", "Kiwi", "Plum", "Otter", "Comet", "Pixel", "Maple", "Falcon", "Cactus", "Lemon")

// shared by the host lobby and the join screen
internal fun randomTeamName(): String = "Team ${TEAM_WORDS.random()}"
