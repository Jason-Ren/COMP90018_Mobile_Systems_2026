package com.comp90018.flashcards.data.p2p

import android.content.Context
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
import kotlinx.serialization.json.Json

private const val SERVICE_ID = "com.comp90018.flashcards.team"

// team mode is strictly two phones, so point to point
private val STRATEGY = Strategy.P2P_POINT_TO_POINT

// the name shown to the other phone is "id|team" so the id rides along for reconnecting
internal fun idOf(endpointName: String) = endpointName.substringBefore('|')

internal fun teamOf(endpointName: String) = endpointName.substringAfter('|')

// nearby's missing permission errors are one run of codes: 8029 (nearby wifi devices) to 8039 (bluetooth connect)
private val MISSING_PERMISSION_CODES = 8029..8039

// turns nearby's raw error (like 8032: MISSING_PERMISSION_ACCESS_WIFI_STATE) into something a player can act on
internal fun friendlyReason(e: Exception): String =
    when ((e as? ApiException)?.statusCode) {
        ConnectionsStatusCodes.STATUS_BLUETOOTH_ERROR -> "Bluetooth is off or not available\nTurn it on and try again"
        ConnectionsStatusCodes.MISSING_SETTING_LOCATION_MUST_BE_ON -> "Location is off\nTurn it on and try again"
        in MISSING_PERMISSION_CODES -> "Some permissions are missing\nAllow them in the app settings and try again"
        else -> "Something went wrong starting nearby\nTry again"
    }

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

    fun send(
        endpointId: String,
        message: GameMessage,
    )

    fun decode(payload: Payload): GameMessage?
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
    ) {
        val bytes = json.encodeToString(GameMessage.serializer(), message).toByteArray()
        client.sendPayload(endpointId, Payload.fromBytes(bytes))
    }

    override fun decode(payload: Payload): GameMessage? =
        payload.asBytes()?.let { json.decodeFromString(GameMessage.serializer(), it.decodeToString()) }
}
