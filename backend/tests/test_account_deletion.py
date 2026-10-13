import time
from uuid import uuid4

import pytest
from conftest import headers, make_friends, rules_headers, share
from google.api_core.exceptions import ServiceUnavailable

from social_api.accounts import AccountService
from social_api.auth import Identity


@pytest.fixture(autouse=True)
def deletion_markers(database):
    yield
    for user in ("alice", "bob", "charlie"):
        database.deletions.document(f"firebase-uid-{user}").delete()


@pytest.mark.parametrize("auth_time", [None, 0, int(time.time()) + 3600])
def test_deletion_requires_recent_sign_in(client, profiles, database, monkeypatch, auth_time):
    monkeypatch.setattr(
        client.app.state.verifier, "verify", lambda _: Identity("firebase-uid-alice", auth_time)
    )
    response = client.delete("/v1/me", headers=headers("alice"))
    assert response.status_code == 403
    assert response.json()["error"]["code"] == "recent_login_required"
    assert database.users.document(database.subject_hash("firebase-uid-alice")).get().exists
    assert not database.deletions.document("firebase-uid-alice").get().exists


def test_deletion_is_scoped_retryable_and_preserves_other_peoples_copies(
    client, profiles, database, deck
):
    make_friends(client, profiles)
    share(client, deck, profiles)
    receipt = str(uuid4())
    copied = client.post(
        f"/v1/decks/{deck.id}/copies", headers=headers("bob"), json={"request_id": receipt}
    )
    assert copied.status_code == 200
    copy_ref = database.client.collection("decks").document(copied.json()["deckId"])
    owned_copy = client.post(
        f"/v1/decks/{deck.id}/copies", headers=headers("alice"), json={"request_id": str(uuid4())}
    )
    assert owned_copy.status_code == 200
    incoming_grant = copy_ref.collection("shares").document("firebase-uid-alice")
    incoming_grant.set({"recipient_id": profiles["alice"]["id"], "namespace": database.root.id})
    try:
        response = client.delete("/v1/me", headers=headers("alice"))
        assert response.status_code == 204, response.text
        assert not deck.get().exists
        assert list(deck.collection("cards").stream()) == []
        assert (
            not database.client.collection("decks")
            .document(owned_copy.json()["deckId"])
            .get()
            .exists
        )
        assert copy_ref.get().to_dict()["ownerId"] == "firebase-uid-bob"
        assert list(copy_ref.collection("cards").stream())
        assert not incoming_grant.get().exists
        assert list(database.relationships.stream()) == []
        assert list(database.request_ids.stream()) == []
        assert (
            client.get("/v1/me", headers=headers("alice")).json()["error"]["code"]
            == "account_deleting"
        )
        assert client.get("/v1/me", headers=headers("bob")).json()["id"] == profiles["bob"]["id"]
        assert (
            list(database.copy_requests.stream())[0].id
            == f"{database.subject_hash('firebase-uid-bob')}_{receipt}"
        )
        # The released username can be claimed; an old deletion retry cannot release it again.
        claimed = client.put(
            "/v1/me",
            headers=headers("charlie"),
            json={"username": "alice", "display_name": "Charlie"},
        )
        assert claimed.status_code == 200
        assert client.delete("/v1/me", headers=headers("alice")).status_code == 204
        assert (
            client.get("/v1/users/lookup?username=alice", headers=headers("bob")).json()["id"]
            == profiles["charlie"]["id"]
        )
    finally:
        database.client.recursive_delete(copy_ref)


def test_partial_deletion_blocks_writes_and_can_resume(
    client, profiles, deck, raw_client, monkeypatch
):
    original = AccountService._delete_shares
    monkeypatch.setattr(
        AccountService,
        "_delete_shares",
        lambda *_: (_ for _ in ()).throw(ServiceUnavailable("down")),
    )
    assert client.delete("/v1/me", headers=headers("alice")).status_code == 503
    assert (
        client.put(
            "/v1/me", headers=headers("alice"), json={"username": "again", "display_name": "Again"}
        ).status_code
        == 409
    )
    assert (
        raw_client.patch(
            f"decks/{uuid4()}",
            headers=rules_headers("alice"),
            json={
                "fields": {
                    "ownerId": {"stringValue": "firebase-uid-alice"},
                    "name": {"stringValue": "Cannot recreate"},
                    "visibility": {"stringValue": "private"},
                    "updatedAt": {"timestampValue": "2026-10-13T00:00:00Z"},
                    "cardCount": {"integerValue": "0"},
                }
            },
        ).status_code
        == 403
    )
    monkeypatch.setattr(AccountService, "_delete_shares", original)
    assert client.delete("/v1/me", headers=headers("alice")).status_code == 204


def test_account_without_community_profile_can_be_deleted(client, database, deck):
    response = client.delete("/v1/me", headers=headers("alice"))
    assert response.status_code == 204
    assert not deck.get().exists
    assert database.deletions.document("firebase-uid-alice").get().to_dict()["complete"]
