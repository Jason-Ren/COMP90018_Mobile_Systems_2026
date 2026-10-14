package com.comp90018.flashcards.data.social

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import javax.inject.Inject

internal val socialJson = Json { ignoreUnknownKeys = true }

class SocialApiException(
    val status: Int,
    val code: String,
    message: String,
) : IOException(message)

data class SocialHttpResponse(
    val status: Int,
    val body: String,
)

interface SocialTransport {
    suspend fun execute(
        url: String,
        method: String,
        token: String,
        body: String?,
    ): SocialHttpResponse
}

class UrlSocialTransport
    @Inject
    constructor() : SocialTransport {
        override suspend fun execute(
            url: String,
            method: String,
            token: String,
            body: String?,
        ): SocialHttpResponse =
            withContext(Dispatchers.IO) {
                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = method
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 60_000
                    connection.instanceFollowRedirects = false
                    connection.useCaches = false
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Accept", "application/json")
                    if (body != null) {
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    }
                    val status = connection.responseCode
                    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    currentCoroutineContext().ensureActive()
                    SocialHttpResponse(status, text)
                } finally {
                    connection.disconnect()
                }
            }
    }

class SocialApi(
    private val baseUrl: String,
    private val session: SocialSession,
    private val transport: SocialTransport,
) {
    init {
        val uri = URI(baseUrl)
        require(uri.scheme == "https" && uri.host != null && uri.userInfo == null) { "Social API requires HTTPS." }
    }

    suspend fun request(
        uid: String,
        method: String,
        path: String,
        body: JsonObject? = null,
    ): String {
        require(path.startsWith("/v1/"))
        var response = send(uid, method, path, body, refresh = false)
        if (response.status == 401) response = send(uid, method, path, body, refresh = true)
        session.requireUser(uid)
        if (response.status !in 200..299) throw response.toException()
        return response.body
    }

    private suspend fun send(
        uid: String,
        method: String,
        path: String,
        body: JsonObject?,
        refresh: Boolean,
    ): SocialHttpResponse {
        val token = session.token(uid, refresh)
        session.requireUser(uid)
        return transport.execute(baseUrl.trimEnd('/') + path, method, token, body?.toString())
    }
}

private fun SocialHttpResponse.toException(): SocialApiException {
    val error = runCatching { socialJson.parseToJsonElement(body).jsonObject["error"]?.jsonObject }.getOrNull()
    val code = error?.get("code")?.jsonPrimitive?.content ?: "service_unavailable"
    val message =
        when (status) {
            401 -> "Please sign out and sign in again."
            413 -> "This deck is too large to copy. The limit is 400 cards and 4 MiB."
            else -> error?.get("message")?.jsonPrimitive?.content ?: "Connection failed. Please try again."
        }
    return SocialApiException(status, code, message)
}
