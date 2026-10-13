package com.comp90018.flashcards.data.social

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder
import javax.inject.Inject

class FriendsRepository
    @Inject
    constructor(
        private val api: SocialApi,
    ) {
        suspend fun profile(uid: String): SocialProfile? =
            try {
                socialJson.decodeFromString<SocialProfile>(api.request(uid, "GET", "/v1/me"))
            } catch (error: SocialApiException) {
                if (error.code == "profile_required") null else throw error
            }

        suspend fun saveProfile(
            uid: String,
            username: String,
            name: String,
        ): SocialProfile =
            socialJson.decodeFromString(
                api.request(
                    uid,
                    "PUT",
                    "/v1/me",
                    buildJsonObject {
                        put("username", username.trim())
                        put("display_name", name.trim())
                    },
                ),
            )

        suspend fun addFriend(
            uid: String,
            username: String,
        ) {
            val query = URLEncoder.encode(username.trim(), "UTF-8")
            val recipient =
                socialJson.decodeFromString<SocialProfile>(
                    api.request(uid, "GET", "/v1/users/lookup?username=$query"),
                )
            api.request(uid, "POST", "/v1/friend-requests", buildJsonObject { put("recipient_id", recipient.id) })
        }

        suspend fun friends(uid: String): List<SocialFriend> = pages(uid, "/v1/friends?")

        suspend fun requests(
            uid: String,
            incoming: Boolean,
            status: String = "pending",
        ): List<SocialRequest> {
            val direction = if (incoming) "incoming" else "outgoing"
            return pages(uid, "/v1/friend-requests?direction=$direction&status=$status&")
        }

        suspend fun respond(
            uid: String,
            requestId: String,
            action: String,
        ) {
            api.request(
                uid,
                "POST",
                "/v1/friend-requests/$requestId/respond",
                buildJsonObject { put("action", action) },
            )
        }

        suspend fun remove(
            uid: String,
            friendId: String,
        ) {
            api.request(uid, "DELETE", "/v1/friends/$friendId")
        }

        private suspend inline fun <reified T> pages(
            uid: String,
            path: String,
        ): List<T> {
            val rows = mutableListOf<T>()
            do {
                val page =
                    socialJson.decodeFromString<List<T>>(
                        api.request(uid, "GET", "${path}limit=100&offset=${rows.size}"),
                    )
                rows.addAll(page)
            } while (page.size == 100 && rows.size <= 10_000)
            return rows
        }
    }
