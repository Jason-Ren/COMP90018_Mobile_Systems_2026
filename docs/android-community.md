# Android community and deck sharing

The app uses its existing Firebase sign-in for both Firestore and the Python social API. No extra API key, service-account file, or HMAC secret belongs in the Android app.

## Use the feature

1. From **My Decks**, open **Community**. **Discover** lists public decks and filters the loaded catalogue by deck name or creator email as you type. Each row offers **Preview deck** and **Save a copy**, and marks your own decks as **Yours**. You can also save from the preview. The copy is private and editable; **Open my saved copy** opens the normal deck screen for editing and study.
2. Under **Community → Friends**, create a username and display name. Find a friend by exact username, send a request, and have the other account accept it. Incoming requests can be accepted/declined; outgoing requests can be cancelled. Removing a friend ends private sharing access in both directions.
3. Open one of your decks, then **⋮ → Share with friends**. The app waits for local changes to sync before loading the community view. Choose **Share with a friend**, select **Share**, and use **Copy deck code**. Send that code to the recipient yourself. They paste it into **Community → Discover → Open shared deck**. A code does not grant permission on its own.
4. Use **Revoke access** to revoke a friend's share. The API does not list existing share grants, so the sheet offers share/revoke actions without claiming a current grant status. After unfriending, old private grants are already inactive.
5. On your deck page, **⋮ → Publish / Unpublish** asks for confirmation. Publish syncs local edits first and makes the deck readable by all signed-in users; Unpublish removes public access while preserving explicit friend shares. These actions are also available in the cloud preview and use the same API methods. The deck page shows cloud visibility and a source link for server-created copies when online. Previously saved copies are independent and remain available.

Loading and error messages appear on each screen. Actions are disabled while a request is pending; refresh reloads server state. Publish/unpublish, friend actions, and share/revoke are retryable according to the backend contract. A failed Save-a-copy retains its request UUID so the next attempt retrieves the same server copy rather than creating another one. Reopening that source's code exposes **Retry saving copy** even if source access has since ended: the server can return an already-completed copy that belongs to you.

## Connection and data handling

The default API URL is the existing Melbourne deployment:

`https://comp90018-mobile-systems-2026-12898337120.australia-southeast2.run.app`

Override it at build time with the Gradle property `SOCIAL_API_URL`, for example in your uncommitted user Gradle properties. HTTPS is required. `app/google-services.json` must target the same Firebase project as the API. The backend must expose version `0.2.0` endpoints and use the root Firestore rules/indexes.

`SocialApi` attaches a Firebase ID token, refreshes once on HTTP 401, refuses redirects, and checks the originating account before sending and after receiving a response. Tokens are neither logged nor persisted by this client. Navigation is reset when the Firebase account changes. No optimistic permission changes are shown before the server succeeds.

Save-a-copy keeps a pending UUID in private preferences, scoped to the user and source deck. It downloads all copied cards in pages and imports the entire deck into Room in one transaction. No study-state or review-history rows are imported, so the existing scheduler starts new cards fresh. Retrying a completed local import does not overwrite later local edits. If the server copy was deleted, the old receipt is cleared and the next user action can start a new copy.

Source attribution stays on the cloud deck and is displayed as **View source deck**. Normal sync uses merge/content updates and preserves attribution and public visibility. The import keeps HTTPS image references and omits device-local image paths; image binaries are not uploaded or copied by this feature. The preview currently shows text and an attachment indicator.

The backend's copy limit remains 400 cards / 4 MiB. A shared-deck inbox, notifications, portable image uploads, and live catalogue subscriptions are not implemented. Private decks are opened using their shared code; public decks are browsable. Catalogue refresh and social actions require network access, while imported copies can be studied locally.

## Verification

The public deck row and publish menu adapt Jason Ren's UI from [PR #64](https://github.com/pandariayu/COMP90018_Mobile_Systems_2026/pull/64), commit `c2350e0`. They are integrated into the existing Community navigation rather than adding a second Browse screen. `CommunityDecks.publish` is the only Android publish/unpublish implementation, and both catalogue and preview save actions use `DeckCopies.save` with persisted retry receipts.

The direct-Firestore `DeckExchange` copy/visibility path from PR #64 is not imported. Cloud attribution stays in the server-managed `copiedFrom` field; Room remains version 5. Optional Firestore `authorEmail` is written when you Publish (for Discover search only) and is not stored in Room. Source attribution in the preview still uses the source-deck link.

Run from the repository root:

```powershell
.\gradlew.bat check :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

The second command needs a connected Android device or emulator. JVM tests cover token refresh, account changes, API errors, interrupted copying and stable retry IDs. Device tests cover profile/code input, action availability, copy navigation, atomic Room import, preservation of local edits, and persistent copy receipts. These tests use fake API/session boundaries and an in-memory Room database; they do not create live Firebase accounts or decks.

For a live integration check, use two signed-in test accounts: accept a friendship, share a private deck, open its code as the recipient, save a copy, revoke the share, and verify that the source becomes inaccessible while the saved copy remains in My Decks. Also publish/unpublish and check visibility from the second account. This requires real Firebase accounts and deployed rules; a successful build alone does not verify it.
