# Web to Android Parity

This checklist compares current user-visible workflows in the Solid web client and native Android client. A registered route alone does not count as parity: the action, state, result, and platform behavior must work.

Status key: `[x]` implemented and validated, `[~]` partial or materially different, `[ ]` outstanding.

## Auth and Onboarding

- [~] Native login, MFA, registration, email verification, and onboarding. Android routes are registered in `app/src/main/java/chat/stoat/activities/MainActivity.kt`.
  - [x] Login response parsing in `Login.kt` hardened against HTTP 4xx/5xx and JSON error payloads; resolved `MfaCheck` serialization crash (`Field 'result' is required`); added support for `Disabled` account payload; updated session name to "DawnChat for Android".
- [~] Native password reset (request + confirm with token), resend verification, and deep links for `/login/reset/{token}`, `/login/verify/{token}` and `/delete/{token}`. Debug APK builds; validate on device. Token-login links and Android App Links verification (`assetlinks.json` on the web host) are still outstanding.
- [ ] Compare account deletion, token login, and all MFA recovery/error states against web auth flows.

## Home and Conversations

- [~] Real cached DM/group list now replaces fake rows in Android Polar Conversations. The list shows channel identity, unread/muted state, opens the native conversation, uses actual Saved Notes content, and links to group creation. Release Kotlin compilation passes; still validate on a signed-in device with DMs and unread state.
- [~] Channel/conversation context sheet now matches web `ChannelContextMenu.tsx`: mark as read (only when unread), mute/unmute, copy link, copy ID, and close DM / leave group / delete channel (needs ManageChannel) with confirmation. Long-press on Conversations rows opens it. Release Kotlin compilation passes; validate on device.
- [~] Conversations tab now has a Friends row with a pending-request badge that opens a standalone `friends` route (back arrow). Overview (Home) now also has Create or join a server, Discover, and Donate entries matching web `Home.tsx`. Communities tab now lists servers in the synced sidebar order with folders (tap to collapse), unread badges, long-press server actions (copy ID, folder, report, leave, settings), and a `main/server/{serverId}` channel list grouped by category with long-press channel actions. Still to verify: feedback entry and server header/banner. Tapping a voice channel in the server list opens the voice overlay. Web reference: `packages/client/src/interface/navigation/channels/HomeSidebar.tsx` and `packages/client/src/interface/Home.tsx`.
- [ ] Verify conversation ordering and latest-message preview parity with web; current Android list ordering uses `lastMessageID`.
- [ ] Remove remaining differences in group creation, invite handling, saved notes, context actions, and empty/loading/error states.

## Servers and Channels

- [~] Native server/channel navigation, chat, channel settings, and server administration routes exist. Verify each workflow and permission-denied state against web before marking complete.
- [ ] Compare channel create/edit/delete, categories/reordering, slowmode, permissions, role management, invites, bans, emojis, identity, and server moderation action-by-action.
- [~] Channel webhooks (list/create/edit/avatar/copy/delete, needs ManageWebhooks) at `settings/channel/{id}/webhooks`; owned bots (list/create/rename/public/token reset/delete) at `settings/bots`. Endpoints checked against the Rust backend routes. Bot avatar/banner/bio, invite-bot, and discoverable section are not ported.
- [ ] Port missing server/channel webhook workflows. Web reference: `packages/client/components/app/interface/settings/ChannelSettings.tsx`.
- [ ] Port owned-bot management/settings. Web reference: `packages/client/components/app/interface/settings/user/UserSettings.tsx`.

## Messages, Search, and Pins

- [~] Android has native message rendering, reactions, replies, editing/deletion, search, pins, attachments, and reporting surfaces. Route evidence: `app/src/main/java/chat/stoat/activities/MainActivity.kt`; web channel actions: `packages/client/src/interface/channels/`.
- [~] Message sheet now supports mark unread, pin/unpin, and permission gating; moderator reaction removal is not ported.
- [ ] Compare each message action and state (permissions, optimistic updates, failure recovery, paging, filters, embeds, and attachment handling); record concrete gaps as individual tasks.

## Friends and Profiles

- [~] Android has friends, incoming/outgoing requests, blocked users, QR/tag add, profile editing, and user cards.
- [~] Friends screen has Online/All/Pending/Blocked filters with counts and empty states; profile sheets show mutual friends/servers (`/users/{id}/mutual`, matches backend).
- [ ] Compare web online/all/pending/blocked filters, profile fields, relationships, status, badges, and interaction errors. Web reference: `packages/client/src/interface/Friends.tsx`.

## Voice, Video, and Notifications

- [~] Native LiveKit voice, camera, screen share, MediaProjection consent, ongoing call service, permission rationale, FCM registration, and message notifications exist. Preserve these native integrations. Evidence: `app/src/main/java/chat/stoat/composables/voice/VoiceSheet.kt`, `app/src/main/java/chat/stoat/voice/VoiceCallManager.kt`, `app/src/main/java/chat/stoat/c2dm/HandlerService.kt`, and `app/src/main/java/chat/stoat/screens/settings/NotificationsSettingsScreen.kt`.
- [~] New native Voice settings screen (`settings/voice`): noise suppression, echo cancellation, auto gain, output volume, output route, default camera, screen-share quality, applied in `VoiceCallManager`. Input/per-user volume and device pickers are not ported. Unverified on device.
- [ ] Compare web voice settings and actions: input/output device selection, camera selection, screen-share quality/audio, participant controls, reconnect/error behavior, and call notifications. Web references: `packages/client/components/rtc/` and `packages/client/components/app/interface/settings/user/voice/`.
- [ ] Test camera and screen sharing on physical Android devices across permission-denial, cancellation, app-background, and process-recreation cases.

## Discovery and Invites

- [~] Discovery uses the hosted web page in a WebView; invite links open through native `InviteActivity`.
- [ ] Match web discovery navigation events for invite and bot-invite IDs, deep links, and return/back behavior. Compare `app/src/main/java/chat/stoat/composables/screens/services/DiscoverView.kt` with web `packages/client/src/interface/Discover.tsx`.

## Branding and Assets

- [x] Android Compose default palette uses the web seed `#5470ec`, Tonal Spot variant, system light/dark mode, and Inter typography.
- [x] Adaptive launcher foreground, splash icon, monochrome launcher icon, notification icon, and notification accent now use the current web sunset mark/palette from `packages/client/assets/web/`.
- [ ] Use the current web wordmark in Android home/about surfaces and review onboarding, empty states, and store graphics.
- [ ] Verify adaptive icon foreground/background and monochrome icon rendering on physical Android launchers.

## API Contracts and Drift Prevention

- [~] Web `packages/stoat.js` and Android `core/model` define separate models; backend `Backend/api-1.json` is an OpenAPI reference but still contains legacy branding/hosts. Treat server implementation as the contract authority.
- [ ] Choose canonical contract source: update/validate the OpenAPI document against the running DawnChat API, or introduce language-neutral JSON fixtures for high-risk payloads.
- [ ] Add cross-client fixtures for root config, channels, users, messages, invites, and realtime frames; assert Kotlin and TypeScript serialize/deserialize the same fixture shapes.
- [ ] Add contract checks to both client CI workflows so schema drift fails before release.

## Acceptance Criteria

- Every web screen/action available to an authenticated user has a mapped Android workflow or a documented platform-specific reason and equivalent action.
- No placeholder, fabricated row, hard-coded preview, or web-only escape remains in a workflow marked complete.
- Permission, denied-permission, offline, loading, empty, success, and error states are verified for each ported workflow.
- Android camera, screen capture, LiveKit calls, FCM, notification actions, and deep links continue to use the native integrations.
- Current DawnChat brand assets render in adaptive launcher icons, notification surfaces, splash, and app identity screens.
- Shared contract fixtures are exercised by Android and web tests in CI.
