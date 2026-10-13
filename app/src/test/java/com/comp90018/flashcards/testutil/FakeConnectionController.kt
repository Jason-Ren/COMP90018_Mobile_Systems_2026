package com.comp90018.flashcards.testutil

import com.comp90018.flashcards.data.p2p.ConnectionController
import com.comp90018.flashcards.data.p2p.ConnectionState
import com.comp90018.flashcards.data.p2p.DiscoveredHost
import com.comp90018.flashcards.data.p2p.GameMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

// a connection controller that only records what it was asked to do, so view models can be tested without nearby
class FakeConnectionController : ConnectionController {
    override val state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val hosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    override val messages = MutableSharedFlow<GameMessage>(extraBufferCapacity = 16)
    override val opponentScore = MutableStateFlow<Int?>(null)

    val hostedAs = mutableListOf<String>()
    val joinedAs = mutableListOf<String>()
    val requested = mutableListOf<DiscoveredHost>()
    val sent = mutableListOf<GameMessage>()
    var accepts = 0
    var rejects = 0
    var removals = 0
    var reconnects = 0
    var endSessions = 0

    override fun startHosting(teamName: String) {
        hostedAs += teamName
        state.value = ConnectionState.Advertising
    }

    override fun startJoining(teamName: String) {
        joinedAs += teamName
        state.value = ConnectionState.Discovering
    }

    override fun requestToJoin(host: DiscoveredHost) {
        requested += host
    }

    override fun accept() {
        accepts++
    }

    override fun reject() {
        rejects++
    }

    override fun removePeer() {
        removals++
    }

    override fun reconnect() {
        reconnects++
    }

    override fun send(message: GameMessage) {
        sent += message
    }

    override fun endSession() {
        endSessions++
    }
}
