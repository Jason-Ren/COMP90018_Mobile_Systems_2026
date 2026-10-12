package com.comp90018.flashcards.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comp90018.flashcards.data.social.SocialApiException
import com.comp90018.flashcards.data.social.SocialSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class SocialTaskState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

abstract class SocialViewModel(
    private val session: SocialSession,
) : ViewModel() {
    val userId = session.uid.orEmpty()
    private val task = MutableStateFlow(SocialTaskState())
    val taskState = task.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    protected fun runAction(
        message: String? = null,
        action: suspend () -> Unit,
    ) {
        if (task.value.busy) return
        task.value = SocialTaskState(busy = true)
        viewModelScope.launch {
            try {
                withTimeout(90_000) {
                    session.requireUser(userId)
                    action()
                    session.requireUser(userId)
                }
                task.value = SocialTaskState(message = message)
            } catch (_: TimeoutCancellationException) {
                task.value = SocialTaskState(error = "This is taking too long. Check your connection and retry.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                task.value = SocialTaskState(error = error.userMessage())
            }
        }
    }
}

private fun Exception.userMessage(): String =
    when (this) {
        is SocialApiException -> message ?: "Please try again."
        is IllegalStateException -> message ?: "Please try again."
        else -> "Could not complete this action. Check your connection and try again."
    }
