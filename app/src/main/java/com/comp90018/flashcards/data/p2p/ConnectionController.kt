package com.comp90018.flashcards.data.p2p

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** where the link to the other phone is at, the ui only reacts to this and never touches nearby */
sealed interface ConnectionState {
    // nothing running
    data object Idle : ConnectionState

    // host is visible to nearby phones and waiting for a request
    data object Advertising : ConnectionState

    // guest is looking for hosts
    data object Discovering : ConnectionState

    // a join request is waiting for the host to answer (guest: waiting for the host)
    data class Requested(
        val peerName: String,
    ) : ConnectionState

    data class Connected(
        val peerName: String,
    ) : ConnectionState

    // link dropped, trying again for a few seconds (show the loading indicator)
    data class Reconnecting(
        val peerName: String,
    ) : ConnectionState

    // gave up reconnecting, show the lost connection screen (reconnect or end session)
    data class Lost(
        val peerName: String,
    ) : ConnectionState

    // the other phone ended the session or removed us on purpose
    data object PeerEnded : ConnectionState

    // nearby refused to start (usually permissions or bluetooth off)
    data class Failed(
        val reason: String,
    ) : ConnectionState
}

data class DiscoveredHost(
    val endpointId: String,
    val teamName: String,
)

/** owns the nearby connection for team mode: connect, disconnect and reconnect */
interface ConnectionController {
    val state: StateFlow<ConnectionState>

    // hosts the guest can currently see
    val hosts: StateFlow<List<DiscoveredHost>>

    // messages from the other phone, already decoded
    val messages: SharedFlow<GameMessage>

    // the other team's final score once it has arrived, null before that and at game start.
    // unlike messages this is kept, so a screen that opens after it arrived can still read it
    val opponentScore: StateFlow<Int?>

    fun startHosting(teamName: String)

    fun startJoining(teamName: String)

    fun requestToJoin(host: DiscoveredHost)

    // host answers a pending join request
    fun accept()

    fun reject()

    // host kicks the connected guest and goes back to waiting
    fun removePeer()

    // from the lost connection screen
    fun reconnect()

    fun send(message: GameMessage)

    fun endSession()
}
