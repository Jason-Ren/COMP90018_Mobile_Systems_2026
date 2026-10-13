package com.comp90018.flashcards.data.p2p

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.IOException

private const val SERVICE_ID = "com.comp90018.flashcards.team"

private const val TAG = "NearbyHub"

// nearby allows a bytes payload of 32 KB (ConnectionsClient.MAX_BYTES_DATA_SIZE), stay under it
private const val MAX_BYTES_PAYLOAD = 30_000

// clash mode is strictly two phones, so point to point
private val STRATEGY = Strategy.P2P_POINT_TO_POINT

// the name shown to the other phone is "id|team" so the id rides along for reconnecting
internal fun idOf(endpointName: String) = endpointName.substringBefore('|')

internal fun teamOf(endpointName: String) = endpointName.substringAfter('|')

// nearby's missing permission errors are one run of codes: 8029 (nearby wifi devices) to 8039 (bluetooth connect)
private val MISSING_PERMISSION_CODES = 8029..8039

@Suppress("DEPRECATION")
// turns nearby's raw error (like 8032: MISSING_PERMISSION_ACCESS_WIFI_STATE) into something a player can act on
internal fun friendlyReason(e: Exception): String {
    val code = (e as? ApiException)?.statusCode
    val message =
        when (code) {
            ConnectionsStatusCodes.STATUS_RADIO_ERROR ->
                "Bluetooth or Wi-Fi is off\nTurn both on and try again"
            ConnectionsStatusCodes.MISSING_SETTING_LOCATION_MUST_BE_ON ->
                "Location is turned off\nTurn on Location in your phone's settings and try again"
            in MISSING_PERMISSION_CODES ->
                "Nearby access isn't allowed\nOpen Settings, allow $permissionName and try again"
            else ->
                "Couldn't connect to nearby phones\nCheck Bluetooth and Wi-Fi are on, then try again"
        }
    // DEBUG ONLY: remove before release, players shouldn't see raw codes
    val debugInfo =
        if (code != null) {
            "${ConnectionsStatusCodes.getStatusCodeString(code)} ($code)"
        } else {
            e::class.java.simpleName
        }
    return "$message\n[$debugInfo]"
}

// android 12 and newer call the permission "Nearby devices", older versions call it "Location"
private val permissionName: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Nearby devices" else "Location"

/** what the controller needs from nearby, so a test can stand in for the real client */
internal interface NearbyTransport {
    fun advertise(
        name: String,
        callback: ConnectionLifecycleCallback,
        onFailure: (Exception) -> Unit,
    )

    fun discover(
        callback: EndpointDiscoveryCallback,
        onFailure: (Exception) -> Unit,
    )

    fun stop()

    fun requestConnection(
        name: String,
        endpointId: String,
        callback: ConnectionLifecycleCallback,
        onFailure: (Exception) -> Unit,
    )

    fun accept(
        endpointId: String,
        callback: PayloadCallback,
    )

    fun reject(endpointId: String)

    fun disconnect(endpointId: String)

    fun disconnectAll()

    // onFailure is called if nearby refuses the message, so a lost deck is not silent
    fun send(
        endpointId: String,
        message: GameMessage,
        onFailure: (Exception) -> Unit = {},
    )

    // suspends for a stream because its bytes arrive over time, null if it cannot be read
    suspend fun decode(payload: Payload): GameMessage?
}

/** every call to the nearby client lives here, so the controller only deals with state */
internal class NearbyHub(
    context: Context,
) : NearbyTransport {
    private val client = Nearby.getConnectionsClient(context)
    private val json = Json { ignoreUnknownKeys = true }

    // stop first so calling it twice never throws already advertising
    override fun advertise(
        name: String,
        callback: ConnectionLifecycleCallback,
        onFailure: (Exception) -> Unit,
    ) {
        client.stopAdvertising()
        client
            .startAdvertising(name, SERVICE_ID, callback, AdvertisingOptions.Builder().setStrategy(STRATEGY).build())
            .addOnFailureListener(onFailure)
    }

    override fun discover(
        callback: EndpointDiscoveryCallback,
        onFailure: (Exception) -> Unit,
    ) {
        client.stopDiscovery()
        client
            .startDiscovery(SERVICE_ID, callback, DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
            .addOnFailureListener(onFailure)
    }

    override fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
    }

    // guest asks a discovered host to connect
    override fun requestConnection(
        name: String,
        endpointId: String,
        callback: ConnectionLifecycleCallback,
        onFailure: (Exception) -> Unit,
    ) {
        client.requestConnection(name, endpointId, callback).addOnFailureListener(onFailure)
    }

    // both sides must accept before nearby reports the connection as made
    override fun accept(
        endpointId: String,
        callback: PayloadCallback,
    ) {
        client.acceptConnection(endpointId, callback)
    }

    override fun reject(endpointId: String) {
        client.rejectConnection(endpointId)
    }

    override fun disconnect(endpointId: String) {
        client.disconnectFromEndpoint(endpointId)
    }

    override fun disconnectAll() {
        client.stopAllEndpoints()
    }

    override fun send(
        endpointId: String,
        message: GameMessage,
        onFailure: (Exception) -> Unit,
    ) {
        val bytes = json.encodeToString(GameMessage.serializer(), message).toByteArray()
        // a whole deck is too big for one bytes payload, so anything large goes as a stream
        val payload =
            if (bytes.size <= MAX_BYTES_PAYLOAD) {
                Payload.fromBytes(bytes)
            } else {
                Payload.fromStream(ByteArrayInputStream(bytes))
            }
        client.sendPayload(endpointId, payload).addOnFailureListener(onFailure)
    }

    override suspend fun decode(payload: Payload): GameMessage? {
        // small messages are already here; a stream is read off the main thread as it arrives
        val bytes = payload.asBytes() ?: readStream(payload)
        return bytes?.let(::parse)
    }

    private suspend fun readStream(payload: Payload): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                payload.asStream()?.asInputStream()?.use { it.readBytes() }
            } catch (e: IOException) {
                Log.w(TAG, "could not read the stream", e)
                null
            }
        }

    // a message that is not valid is dropped instead of crashing the callback
    private fun parse(bytes: ByteArray): GameMessage? =
        runCatching { json.decodeFromString(GameMessage.serializer(), bytes.decodeToString()) }
            .onFailure { Log.w(TAG, "could not read a message", it) }
            .getOrNull()
}
