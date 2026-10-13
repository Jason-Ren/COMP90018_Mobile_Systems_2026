package com.comp90018.flashcards.testutil

import com.comp90018.flashcards.data.p2p.NearbyConnectionController
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

// nearby's result classes cannot be built by hand, so these are mocks that only know what the controller reads
internal fun info(endpointName: String): ConnectionInfo = mock { on { this.endpointName } doReturn endpointName }

internal fun found(endpointName: String): DiscoveredEndpointInfo =
    mock {
        on { this.endpointName } doReturn endpointName
    }

internal fun result(success: Boolean): ConnectionResolution {
    val status: Status = mock { on { isSuccess } doReturn success }
    return mock { on { this.status } doReturn status }
}

// the host side of a finished handshake: a guest asked to join, the host accepted, nearby said connected
internal fun NearbyConnectionController.hostConnectedTo(
    transport: FakeTransport,
    teamName: String = "Team B",
    guestId: String = "g1",
    endpoint: String = "e1",
) {
    startHosting("Team A")
    transport.connectionCallback.onConnectionInitiated(endpoint, info("$guestId|$teamName"))
    accept()
    transport.connectionCallback.onConnectionResult(endpoint, result(true))
}

// the guest side of a finished handshake
internal fun NearbyConnectionController.guestConnectedTo(
    transport: FakeTransport,
    teamName: String = "Team A",
    hostId: String = "h0",
    endpoint: String = "e1",
) {
    startJoining("Team B")
    transport.discoveryCallback.onEndpointFound(endpoint, found("$hostId|$teamName"))
    requestToJoin(hosts.value.first())
    transport.connectionCallback.onConnectionInitiated(endpoint, info("$hostId|$teamName"))
    transport.connectionCallback.onConnectionResult(endpoint, result(true))
}
