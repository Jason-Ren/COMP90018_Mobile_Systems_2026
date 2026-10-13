package com.comp90018.flashcards.data.account

import android.content.Context
import androidx.room.withTransaction
import com.comp90018.flashcards.data.local.DeckSyncDatabase
import com.comp90018.flashcards.data.local.FlashcardDatabase
import com.comp90018.flashcards.data.social.SocialProfile
import com.comp90018.flashcards.data.social.socialJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: FlashcardDatabase,
        private val syncDatabase: DeckSyncDatabase,
    ) {
        private val preferences = context.getSharedPreferences("accounts", Context.MODE_PRIVATE)

        suspend fun profile(uid: String): SocialProfile? =
            withContext(Dispatchers.IO) {
                preferences.getString("profile_$uid", null)?.let { socialJson.decodeFromString<SocialProfile>(it) }
            }

        suspend fun save(
            uid: String,
            profile: SocialProfile,
        ) = withContext(Dispatchers.IO) {
            check(preferences.edit().putString("profile_$uid", socialJson.encodeToString(profile)).commit())
        }

        suspend fun isDeleting(uid: String): Boolean =
            withContext(Dispatchers.IO) {
                preferences.getBoolean("deleting_$uid", false)
            }

        suspend fun clearProfile(uid: String) =
            withContext(Dispatchers.IO) {
                check(preferences.edit().remove("profile_$uid").commit())
            }

        suspend fun setDeleting(
            uid: String,
            deleting: Boolean,
        ) = withContext(Dispatchers.IO) {
            check(preferences.edit().putBoolean("deleting_$uid", deleting).commit())
        }

        suspend fun eraseLocalData(uid: String) {
            database.withTransaction {
                database.reviewDao().deleteStatesForUser(uid)
                database.reviewDao().deleteLogsForUser(uid)
                database.cardDao().deleteForOwner(uid)
                database.deckDao().deleteForOwner(uid)
            }
            syncDatabase.outboxDao().deleteForOwner(uid)
            withContext(Dispatchers.IO) {
                val copies = context.getSharedPreferences("pending_deck_copies", Context.MODE_PRIVATE)
                val editor = copies.edit()
                copies.all.keys
                    .filter { it.substringBeforeLast('_', "") == uid }
                    .forEach(editor::remove)
                check(editor.commit())
                check(preferences.edit().remove("profile_$uid").commit())
            }
        }
    }
