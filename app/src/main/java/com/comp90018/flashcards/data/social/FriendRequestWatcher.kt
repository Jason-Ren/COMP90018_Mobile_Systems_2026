package com.comp90018.flashcards.data.social

/** Turns pending outgoing requests into a live approved/rejected notice for the sender. */
internal class FriendRequestWatcher {
    private val pending = mutableMapOf<String, SocialRequest>()
    private val ignored = mutableSetOf<String>()
    private var ready = false

    fun ignore(requestId: String) {
        ignored += requestId
    }

    fun needsDeclined(
        currentPending: List<SocialRequest>,
        friends: List<SocialFriend>,
    ): Boolean {
        if (!ready) return false
        val pendingIds = currentPending.map { it.id }.toSet()
        val friendIds = friends.map { it.user.id }.toSet()
        return pending.values.any { request ->
            request.id !in pendingIds && request.id !in ignored && request.recipient.id !in friendIds
        }
    }

    fun consume(
        currentPending: List<SocialRequest>,
        friends: List<SocialFriend>,
        declined: List<SocialRequest>,
        notify: Boolean,
    ): List<String> {
        val notices = if (ready && notify) noticesFor(currentPending, friends, declined) else emptyList()
        pending.clear()
        pending += currentPending.associateBy { it.id }
        ignored.retainAll(pending.keys)
        ready = true
        return notices
    }

    private fun noticesFor(
        currentPending: List<SocialRequest>,
        friends: List<SocialFriend>,
        declined: List<SocialRequest>,
    ): List<String> {
        val pendingIds = currentPending.map { it.id }.toSet()
        val friendIds = friends.map { it.user.id }.toSet()
        val declinedIds = declined.map { it.id }.toSet()
        return pending.values.mapNotNull { request ->
            noticeFor(request, pendingIds, friendIds, declinedIds)
        }
    }

    private fun noticeFor(
        request: SocialRequest,
        pendingIds: Set<String>,
        friendIds: Set<String>,
        declinedIds: Set<String>,
    ): String? {
        if (request.id in pendingIds || request.id in ignored) return null
        val name = request.recipient.displayName
        return when {
            request.recipient.id in friendIds -> "$name approved your friend request"
            request.id in declinedIds -> "$name rejected your friend request"
            else -> null
        }
    }
}
