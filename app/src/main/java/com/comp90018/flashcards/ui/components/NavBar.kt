package com.comp90018.flashcards.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphNavigator
import androidx.navigation.compose.NavHost
import com.comp90018.flashcards.R

@Composable
fun NavBar(
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit,


    ) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        NavigationBarItem(
            selected = false,
            onClick = onHome,
            icon = {
                Icon(painter = painterResource(id = R.drawable.ic_stack), contentDescription = "Add Deck",)},
            label = { Text("Decks") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onJoinGame,
            icon = { Icon(painterResource(R.drawable.ic_swords), contentDescription = "Clash") },
            label = { Text("Clash") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onCommunity,
            icon = { Icon(Icons.Filled.Search, contentDescription = "Discover") },
            label = { Text("Discover") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onFriends,
            icon = { Icon(painterResource(R.drawable.ic_conversation), contentDescription = "Friends") },
            label = { Text("Friends") }
        )
    }
}
