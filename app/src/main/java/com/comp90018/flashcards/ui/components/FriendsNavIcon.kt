package com.comp90018.flashcards.ui.components

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.comp90018.flashcards.R

@Composable
internal fun FriendsNavIcon(
    pendingCount: Int,
    modifier: Modifier = Modifier,
) {
    val description = pluralStringResource(R.plurals.pending_friend_requests, pendingCount, pendingCount)
    BadgedBox(
        modifier = modifier,
        badge = {
            if (pendingCount > 0) {
                Badge(Modifier.semantics { contentDescription = description }) {
                    Text(if (pendingCount > 99) "99+" else pendingCount.toString())
                }
            }
        },
    ) {
        Icon(painterResource(R.drawable.ic_conversation), contentDescription = null)
    }
}
