package com.comp90018.flashcards.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.comp90018.flashcards.R

@Composable
fun NavBar(
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        NavigationBarItem(
            selected = false,
            onClick = onHome,
            modifier = Modifier.offset(y = (2).dp),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_stack),
                    contentDescription = "Decks",
                    modifier = Modifier.size(32.dp)
                )
            },
            label = {
                Text("Decks", style = MaterialTheme.typography.labelLarge)
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = onJoinGame,
            modifier = Modifier.offset(y = (2).dp),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_swords),
                    contentDescription = "Clash",
                    modifier = Modifier.size(32.dp)
                )
            },
            label = {
                Text("Clash", style = MaterialTheme.typography.labelLarge)
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = onCommunity,
            modifier = Modifier.offset(y = (2).dp),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Discover",
                    modifier = Modifier.size(32.dp)
                )
            },
            label = {
                Text("Discover", style = MaterialTheme.typography.labelLarge)
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = onFriends,
            modifier = Modifier.offset(y = (2).dp),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_conversation),
                    contentDescription = "Friends",
                    modifier = Modifier.size(32.dp)
                )
            },
            label = {
                Text("Friends", style = MaterialTheme.typography.labelLarge)
            }
        )
    }
}
