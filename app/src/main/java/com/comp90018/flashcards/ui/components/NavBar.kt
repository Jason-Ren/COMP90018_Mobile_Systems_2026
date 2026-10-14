package com.comp90018.flashcards.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.comp90018.flashcards.R

@Composable
fun NavBar(
    selectedDestination: NavDestination?,
    onCommunity: () -> Unit,
    onHome: () -> Unit,
    onJoinGame: () -> Unit,
    onFriends: () -> Unit,
    modifier: Modifier = Modifier,
    pendingFriendRequestCount: Int = 0,
) {
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to background,
                        1f to background,
                    ),
                ).padding(top = 24.dp), // height of the fade zone
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp, // otherwise the tint is drawn over the gradient
        ) {
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
                FriendsNavIcon(pendingCount = pendingFriendRequestCount)
            }
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
