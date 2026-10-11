package com.comp90018.flashcards.data.p2p

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val RECONNECT_GRACE_MS = 8_000L

// small wait so a goodbye payload is sent before we cut the link
private const val FLUSH_MS = 300L

private const val TAG = "NearbyConnection"

private enum class Role { Host, Guest }

/**
 * nearby connections implementation of [ConnectionController]
 *
 * singleton so the same link survives going from the lobby to the game screen
 */
@Singleton
class NearbyConnectionController
    // internal only so a test can pass in a fake transport and a test scope, the app uses the constructor below
    internal constructor(
        private val hub: NearbyTransport,
        private val scope: CoroutineScope,
    ) : ConnectionController,
        DefaultLifecycleObserver {
        // what hilt uses, it builds the real nearby transport
        @Inject
        constructor(
            @ApplicationContext context: Context,
        ) : this(
            NearbyHub(context),
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        ) {
            // watch the whole app (not one activity) so we release when it is minimised
            scope.launch { ProcessLifecycleOwner.get().lifecycle.addObserver(this@NearbyConnectionController) }
        }

        private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
        override val state: StateFlow<ConnectionState> = _state.asStateFlow()

        private val _hosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
        override val hosts: StateFlow<List<DiscoveredHost>> = _hosts.asStateFlow()

        private val _messages = MutableSharedFlow<GameMessage>(extraBufferCapacity = 16)
        override val messages = _messages

        // random id for this app run, sent with our name so a returning phone can be recognised
        private val myId = UUID.randomUUID().toString().take(8)
        private var myTeamName = ""
        private var role: Role? = null

        // the other phone: stable id, display name and its current nearby endpoint
        private var peerId: String? = null
        private var peerName = ""
        private var endpointId: String? = null

        private var graceJob: Job? = null

        // true while advertising/discovering is switched off because the app is in the background
        private var radiosPaused = false

        // name shown to the other phone, see idOf and teamOf
        private val myEndpointName: String get() = "$myId|$myTeamName"

        // the player sees a friendly reason, the raw error goes to logcat for us
        private val onFailure: (Exception) -> Unit = {
            Log.w(TAG, "nearby failed to start", it)
            _state.value = ConnectionState.Failed(friendlyReason(it))
        }

        private val startAdvertising: () -> Unit = { hub.advertise(myEndpointName, connectionCallback, onFailure) }

        private val startDiscovery: () -> Unit = { hub.discover(discoveryCallback, onFailure) }

        // also forget the hosts we had listed
        private val stopRadios: () -> Unit = {
            hub.stop()
            _hosts.value = emptyList()
        }

        override fun startHosting(teamName: String) {
            resetPeer()
            role = Role.Host
            myTeamName = teamName
            _state.value = ConnectionState.Advertising
            startAdvertising()
        }

        override fun startJoining(teamName: String) {
            resetPeer()
            role = Role.Guest
            myTeamName = teamName
            _hosts.value = emptyList()
            _state.value = ConnectionState.Discovering
            startDiscovery()
        }

        override fun requestToJoin(host: DiscoveredHost) {
            if (role != Role.Guest) return
            peerName = host.teamName
            _state.value = ConnectionState.Requested(host.teamName)
            hub.requestConnection(myEndpointName, host.endpointId, connectionCallback) {
                _state.value = ConnectionState.Discovering
            }
        }

        override fun accept() {
            val id = endpointId ?: return
            if (role == Role.Host && _state.value is ConnectionState.Requested) {
                hub.accept(id, payloadCallback)
            }
        }

        override fun reject() {
            val id = endpointId ?: return
            hub.reject(id)
            endpointId = null
            peerId = null
            _state.value = ConnectionState.Advertising
        }

        override fun removePeer() {
            val id = endpointId
            endpointId = null
            peerId = null
            peerName = ""
            _state.value = ConnectionState.Advertising
            scope.launch {
                if (id != null) {
                    hub.send(id, GameMessage.EndSession)
                    delay(FLUSH_MS)
                    hub.disconnect(id)
                }
                // point to point stops advertising once connected, so start again for the next guest
                startAdvertising()
            }
        }

        override fun reconnect() {
            if (_state.value is ConnectionState.Lost) beginReconnecting()
        }

        override fun send(message: GameMessage) {
            endpointId?.let { hub.send(it, message) }
        }

        override fun endSession() {
            val id = endpointId
            scope.launch {
                if (id != null) {
                    hub.send(id, GameMessage.EndSession)
                    delay(FLUSH_MS)
                }
                releaseAll()
                role = null
                resetPeer()
                _state.value = ConnectionState.Idle
            }
        }

        // app went to the background: free the radios and the link so nothing runs while minimised
        override fun onStop(owner: LifecycleOwner) {
            when (val s = _state.value) {
                is ConnectionState.Advertising, is ConnectionState.Discovering -> {
                    stopRadios()
                    radiosPaused = true
                }
                is ConnectionState.Requested -> {
                    releaseAll()
                    endpointId = null
                    peerId = null
                    _state.value = if (role == Role.Host) ConnectionState.Advertising else ConnectionState.Discovering
                    radiosPaused = true
                }
                is ConnectionState.Connected, is ConnectionState.Reconnecting -> {
                    val name =
                        if (s is ConnectionState.Connected) {
                            s.peerName
                        } else {
                            (s as ConnectionState.Reconnecting)
                                .peerName
                        }
                    releaseAll()
                    endpointId = null
                    _state.value = ConnectionState.Lost(name)
                }
                else -> Unit
            }
        }

        // back in the foreground: start searching again if we were only waiting or searching
        override fun onStart(owner: LifecycleOwner) {
            if (!radiosPaused) return
            radiosPaused = false
            when (_state.value) {
                is ConnectionState.Advertising -> startAdvertising()
                is ConnectionState.Discovering -> startDiscovery()
                else -> Unit
            }
        }

        // link dropped: look for the same phone again, and give up after the grace period
        private fun beginReconnecting() {
            _state.value = ConnectionState.Reconnecting(peerName)
            if (role == Role.Host) startAdvertising() else startDiscovery()
            graceJob?.cancel()
            graceJob =
                scope.launch {
                    delay(RECONNECT_GRACE_MS)
                    if (_state.value is ConnectionState.Reconnecting) {
                        stopRadios()
                        _state.value = ConnectionState.Lost(peerName)
                    }
                }
        }

        private fun releaseAll() {
            graceJob?.cancel()
            stopRadios()
            hub.disconnectAll()
        }

        private fun resetPeer() {
            graceJob?.cancel()
            peerId = null
            peerName = ""
            endpointId = null
        }

        private val connectionCallback =
            object : ConnectionLifecycleCallback() {
                override fun onConnectionInitiated(
                    id: String,
                    info: ConnectionInfo,
                ) {
                    val theirId = idOf(info.endpointName)
                    val theirTeam = teamOf(info.endpointName)
                    val current = _state.value
                    when {
                        // guest already chose this host, so accept its side straight away
                        role == Role.Guest -> {
                            endpointId = id
                            peerId = theirId
                            peerName = theirTeam
                            hub.accept(id, payloadCallback)
                        }
                        // the same guest coming back after a drop, no need to ask the host again
                        current is ConnectionState.Reconnecting && theirId == peerId -> {
                            endpointId = id
                            hub.accept(id, payloadCallback)
                        }
                        // a new request, wait for the host to accept or decline
                        current is ConnectionState.Advertising -> {
                            endpointId = id
                            peerId = theirId
                            peerName = theirTeam
                            _state.value = ConnectionState.Requested(theirTeam)
                        }
                        // already busy with someone else
                        else -> hub.reject(id)
                    }
                }

                override fun onConnectionResult(
                    id: String,
                    result: ConnectionResolution,
                ) {
                    if (result.status.isSuccess) {
                        graceJob?.cancel()
                        stopRadios()
                        endpointId = id
                        _state.value = ConnectionState.Connected(peerName)
                    } else if (_state.value !is ConnectionState.Reconnecting) {
                        // refused or failed, go back to waiting or searching
                        endpointId = null
                        peerId = null
                        _state.value =
                            if (role == Role.Host) ConnectionState.Advertising else ConnectionState.Discovering
                    }
                }

                override fun onDisconnected(id: String) {
                    if (id != endpointId) return
                    endpointId = null
                    when (_state.value) {
                        is ConnectionState.Connected -> beginReconnecting()
                        // the requester left before the host answered
                        is ConnectionState.Requested -> {
                            peerId = null
                            _state.value =
                                if (role == Role.Host) ConnectionState.Advertising else ConnectionState.Discovering
                        }
                        else -> Unit
                    }
                }
            }

        private val discoveryCallback =
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(
                    id: String,
                    info: DiscoveredEndpointInfo,
                ) {
                    // while reconnecting, go straight back to the host we lost instead of listing it
                    if (_state.value is ConnectionState.Reconnecting && idOf(info.endpointName) == peerId) {
                        // a failed try is ignored, the grace timer decides when to give up
                        hub.requestConnection(myEndpointName, id, connectionCallback) {}
                    } else {
                        val host = DiscoveredHost(id, teamOf(info.endpointName))
                        _hosts.update { list -> list.filterNot { it.endpointId == id } + host }
                    }
                }

                override fun onEndpointLost(id: String) {
                    _hosts.update { list -> list.filterNot { it.endpointId == id } }
                }
            }

        private val payloadCallback =
            object : PayloadCallback() {
                override fun onPayloadReceived(
                    id: String,
                    payload: Payload,
                ) {
                    val message = hub.decode(payload) ?: return
                    if (message is GameMessage.EndSession) {
                        // peer left on purpose, so skip the reconnect attempt
                        endpointId = null
                        releaseAll()
                        _state.value = ConnectionState.PeerEnded
                    } else {
                        _messages.tryEmit(message)
                    }
                }

                override fun onPayloadTransferUpdate(
                    id: String,
                    update: PayloadTransferUpdate,
                ) = Unit
            }
    }
