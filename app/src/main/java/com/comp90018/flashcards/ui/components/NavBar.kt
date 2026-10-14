package com.comp90018.flashcards.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.comp90018.flashcards.R

@Composable
fun NavBar(
    selectedDestination: NavDestination?,
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        NavItem("Decks", selectedDestination == NavDestination.DECKS, onHome) {
            Icon(
                painter = painterResource(R.drawable.ic_stack),
                contentDescription = null,
            )
        }
        NavItem("Clash", selectedDestination == NavDestination.CLASH, onJoinGame) {
            Icon(
                painter = painterResource(R.drawable.ic_swords),
                contentDescription = null,
            )
        }
        NavItem("Discover", selectedDestination == NavDestination.DISCOVER, onCommunity) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
            )
        }
        NavItem("Friends", selectedDestination == NavDestination.FRIENDS, onFriends) {
            Icon(
                painter = painterResource(R.drawable.ic_conversation),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = { if (!selected) onClick() },
        modifier = modifier,
        icon = icon,
        label = { Text(label) },
    )
}
