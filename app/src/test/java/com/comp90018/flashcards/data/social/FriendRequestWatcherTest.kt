package com.comp90018.flashcards.data.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendRequestWatcherTest {
    private val watcher = FriendRequestWatcher()
    private val bob = profile("bob", "Bob")
    private val cara = profile("cara", "Cara")
    private val pendingBob = request("r-bob", bob)
    private val pendingCara = request("r-cara", cara)

    @Test
    fun `the first snapshot does not replay old outcomes`() {
        assertFalse(watcher.needsDeclined(listOf(pendingBob), emptyList()))
        assertEquals(emptyList<String>(), watcher.consume(listOf(pendingBob), emptyList(), emptyList(), true))
    }

    @Test
    fun `an accepted request shows a success notice and no longer needs declined rows`() {
        watcher.consume(listOf(pendingBob), emptyList(), emptyList(), false)

        val friends = listOf(SocialFriend(bob))
        assertFalse(watcher.needsDeclined(emptyList(), friends))
        assertEquals(
            listOf("Bob approved your friend request"),
            watcher.consume(emptyList(), friends, emptyList(), true),
        )
    }

    @Test
    fun `a declined request shows a failure notice`() {
        watcher.consume(listOf(pendingBob), emptyList(), emptyList(), false)

        assertTrue(watcher.needsDeclined(emptyList(), emptyList()))
        assertEquals(
            listOf("Bob rejected your friend request"),
            watcher.consume(emptyList(), emptyList(), listOf(pendingBob.copy(status = "declined")), true),
        )
    }

    @Test
    fun `cancelling your own request is not treated as a rejection`() {
        watcher.consume(listOf(pendingBob), emptyList(), emptyList(), false)
        watcher.ignore("r-bob")

        assertFalse(watcher.needsDeclined(emptyList(), emptyList()))
        assertEquals(emptyList<String>(), watcher.consume(emptyList(), emptyList(), listOf(pendingBob), true))
    }

    @Test
    fun `still-pending requests stay silent`() {
        watcher.consume(listOf(pendingBob), emptyList(), emptyList(), false)

        assertFalse(watcher.needsDeclined(listOf(pendingBob), emptyList()))
        assertEquals(
            emptyList<String>(),
            watcher.consume(listOf(pendingBob), emptyList(), emptyList(), true),
        )
    }

    @Test
    fun `two responses in the same poll are both reported`() {
        watcher.consume(listOf(pendingBob, pendingCara), emptyList(), emptyList(), false)

        assertEquals(
            listOf("Bob approved your friend request", "Cara rejected your friend request"),
            watcher.consume(
                emptyList(),
                listOf(SocialFriend(bob)),
                listOf(pendingCara.copy(status = "declined")),
                true,
            ),
        )
    }

    private fun profile(
        username: String,
        name: String,
    ) = SocialProfile(id = username, username = username, displayName = name)

    private fun request(
        id: String,
        recipient: SocialProfile,
    ) = SocialRequest(
        id = id,
        sender = profile("alice", "Alice"),
        recipient = recipient,
        status = "pending",
    )
}
