package com.comp90018.flashcards.testutil

import com.comp90018.flashcards.data.p2p.GameMessage
import com.comp90018.flashcards.data.p2p.NearbyTransport
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback

// stands in for nearby, records every call and lets a test decide what decoding a payload gives
internal class FakeTransport : NearbyTransport {
    val advertiseNames = mutableListOf<String>()
    var discoverCalls = 0
    var stops = 0
    var disconnectAlls = 0
    val accepted = mutableListOf<String>()
    val rejected = mutableListOf<String>()
    val disconnected = mutableListOf<String>()
    val requests = mutableListOf<Pair<String, String>>()
    val sent = mutableListOf<Pair<String, GameMessage>>()

    var advertiseFailure: ((Exception) -> Unit)? = null
    var requestFailure: ((Exception) -> Unit)? = null
    var decoded: GameMessage? = null

    // nearby's side of the conversation: the callbacks the controller registered, so a test can call them
    lateinit var connectionCallback: ConnectionLifecycleCallback
    lateinit var discoveryCallback: EndpointDiscoveryCallback
    lateinit var payloadCallback: PayloadCallback

    val advertiseCalls: Int get() = advertiseNames.size

    override fun advertise(
        name: String,
        callback: ConnectionLifecycleCallback,
        onFailure: (Exception) -> Unit,
    ) {
        advertiseNames += name
        connectionCallback = callback
        advertiseFailure = onFailure
    }

    override fun discover(
        callback: EndpointDiscoveryCallback,
        onFailure: (Exception) -> Unit,
    ) {
        discoverCalls++
        discoveryCallback = callback
    }

    override fun stop() {
        stops++
    }

    override fun requestConnection(
        name: String,
        endpointId: String,
        callback: ConnectionLifecycleCallback,
        onFailure: (Exception) -> Unit,
    ) {
        requests += name to endpointId
        connectionCallback = callback
        requestFailure = onFailure
    }

    override fun accept(
        endpointId: String,
        callback: PayloadCallback,
    ) {
        accepted += endpointId
        payloadCallback = callback
    }

    override fun reject(endpointId: String) {
        rejected += endpointId
    }

    override fun disconnect(endpointId: String) {
        disconnected += endpointId
    }

    override fun disconnectAll() {
        disconnectAlls++
    }

    override fun send(
        endpointId: String,
        message: GameMessage,
    ) {
        sent += endpointId to message
    }

    override fun decode(payload: Payload): GameMessage? = decoded
}
