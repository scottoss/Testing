# DawnChat iOS Full Parity Implementation Plan (Zero Demo Data)

## Goal
Replace the simplified demo prototype in the shared/iOS application (`DawnChatMultiplatformApp.kt`) with a complete, 1:1 multiplatform port of every screen, navigation route, bottom sheet, dialog, and live Stoat REST + WebSocket API integration from the Android app—with **zero hardcoded or demo data**.

---

## 1. Multiplatform Persistence, REST API & Real-Time WebSocket Engine
- **Persistent Key-Value & Cache Storage (`SharedKVStorage`)**:
  - Implement platform-backed persistent key-value storage (`NSUserDefaults` on iOS, `SharedPreferences` on Android) for `sessionToken`, `sessionId`, `selfId`, `selfName`, `selfAvatarUrl`, synced user settings, theme/appearance overrides, and feature/experiment flags (`usePolar`, etc.).
- **Complete Stoat REST API Client (`SharedStoatClient`)**:
  - Node discovery & root config (`/`), health checks (`/health`), and geo-restriction checks.
  - Authentication & Account routes: Login (`/auth/session/login`), MFA ticket verification (`/auth/session/login` with MFA response), Registration (`/auth/account/create`), Email verification (`/auth/account/verify/{token}`), Account deletion confirmation (`/auth/account/delete`), Password reset request & confirmation (`/auth/account/reset_password`), Onboarding status & completion (`/onboard/hello`, `/onboard/complete`), Sessions list & revocation (`/auth/session/all`, `/auth/session/{id}`).
  - User & Profile routes: Fetch/edit self user (`/users/@me`), profile bio/background, username/email/password updates, MFA status & TOTP/recovery codes (`/auth/mfa/...`), Friend list & requests (`/users/@me/friend`, `/users/{id}/friend`), Block/Unblock, DMS & Group creation (`/users/dms`, `/channels/create`).
  - Server, Channel & Message routes: Fetch/send/edit/delete messages (`/channels/{id}/messages`), message search (`/channels/{id}/search`), pinned messages (`/channels/{id}/pins`), reactions, ack/unreads (`/channels/{id}/ack/{message}`), typing indicators, channel settings/permissions/webhooks, server overview/roles/channels/emojis/invites/bans/member identity, and bots management (`/bots/...`).
  - Media Upload (`Autumn` `/attachments`, `/avatars`, `/banners`, `/backgrounds`, `/icons`) & `January` proxy integration.
- **Real-Time WebSocket Sync (`SharedStoatRealtime`)**:
  - Connect to the live Stoat Bonfire WebSocket (`wss://...`) with token authentication (`Authenticate`).
  - Hydrate initial state from the `Ready` payload (users, servers, channels, members, emojis, unread states, user settings) and process live events (`Message`, `MessageUpdate`, `MessageDelete`, `MessageReact`, `ChannelCreate`, `ChannelUpdate`, `ChannelDelete`, `ServerUpdate`, `ServerMemberUpdate`, `UserUpdate`, `UserRelationship`, `ChannelStartTyping`, `ChannelStopTyping`).
- **Network Inspector (Chucker Equivalent)**:
  - Record live HTTP & WebSocket traffic in an in-memory ring buffer so the iOS Debug / Network Inspector screen can inspect real requests, headers, status codes, and payloads just like Chucker on Android.

---

## 2. Complete Screen & Navigation Parity (All Android Routes)
All hardcoded mock servers (`DawnChat Lounge`, `Multiplatform Hub`) and mock messages will be removed. The app will use a full navigation router supporting every route in `MainActivity.kt`:

### A. Startup, Connectivity & Authentication
1. `default` (`DefaultDestinationScreen` & `DisconnectedScreen`): Live connectivity check, root config hydration, session validation, health alert banner (`HealthAlert`), and login error recovery dialog.
2. `login/greeting` (`LoginGreetingScreen`) & `login2/init` (`InitScreen`): Welcome screens with Terms, Privacy Policy, Community Guidelines, and Network Inspector link in debug mode.
3. `login/login` (`LoginScreen`): Email & password login with custom instance URL support and automatic MFA redirect.
4. `login/mfa/{mfaTicket}/{allowedAuthTypes}` (`MfaScreen`): TOTP, Recovery Code, and Password MFA verification.
5. `login/recover/{mode}` (`AccountRecoveryScreen`) & `login/reset/confirm?token={token}` (`ConfirmPasswordResetScreen`): Password reset and verification email resend flows.
6. `auth/verify/{token}` & `auth/delete/{token}` (`TokenActionScreen`): Deep-link and manual token verification/deletion screens.
7. `register/greeting` (`RegisterGreetingScreen`), `register/details` (`RegisterDetailsScreen`), `register/verify/{email}` (`RegisterVerifyScreen`), and `register/onboarding` (`OnboardingScreen`): Full registration and username onboarding flow.

### B. Main Chat Layouts (Classic Drawer & Polar Mode)
1. `chat` (`ChatRouterScreen` - Classic Mode):
   - Sliding drawer with real Server Rail, DM/Home/Friends/Saved Notes buttons, Server Header menu, and categorized Text & Voice Channels.
   - Drawer behavior preserved: tapping a server switches the channel list without auto-closing the drawer; tapping a voice channel reveals `Join Voice`; tapping `Join Voice` connects and opens the fullscreen voice call screen.
   - `OverviewScreen`, `ChannelScreen`, `FriendsScreen` (with QR Code generation & live camera QR scanner), `NoCurrentChannelScreen`, `ChannelScreenAgeGate` (NSFW confirmation), and `ChannelScreenGeoGate`.
2. `main` (`MainScreen` - Polar Mode via Experiments toggle):
   - Bottom navigation bar with `CommunitiesScreen`, `ConversationsScreen`, and `OverviewScreen`.
   - `main/server/{serverId}` (`ServerChannelsScreen`) and `main/conversation/{channelId}` (`ChannelScreen`).
3. `catchup` (`CatchUpScreen`): Unread mentions and messages catch-up feed.
4. `create/group` (`CreateGroupScreen`): Select real friends to create a Group DM.
5. `discover` (`DiscoverScreen`): Centered responsive Discover layout (`maxWidth = 600.dp`) with in-app/portal community discovery.
6. `channel/{channelId}/pins` (`ChannelPinsScreen`) & `channel/{channelId}/search` (`ChannelSearchScreen`): Live pinned messages and channel message search with jump-to-message support.

### C. Message Window, Bottom Sheets, Media & Safety Dialogs
1. **Rich Message Window**:
   - Real-time message list with pagination, author avatars/roles/badges, replies, attachments, reactions, and borderless looping GIF embeds (hiding standalone GIF URLs for Gifbox, Tenor, and Giphy).
   - Integrated Emoji & GIF picker pane (Gifbox, Tenor, Giphy categories & search).
2. **Bottom Sheets & Dialogs**:
   - Message Context Sheet (Reply, Edit, Copy Text, Pin/Unpin, Add Reaction, Report Message, Delete Message).
   - User Profile & Member Context Sheet (View Profile, Message, Add/Remove Friend, Block, Manage Roles, Kick, Ban, Timeout, Server Identity).
   - Server Context Sheet & `InviteDialog` (Create/share invite links, Mark as Read, Server Settings, Leave Server, Report Server).
   - Safety Reporting Dialogs: `ReportMessageDialog`, `ReportUserDialog`, and `ReportServerDialog` wired to Stoat safety endpoints.

### D. Complete Settings Hierarchy
1. **User Settings**:
   - `settings` (`SettingsScreen` root menu)
   - `settings/account` (`AccountSettingsScreen`) & `settings/account/mfa` (`MfaSettingsScreen`)
   - `settings/profile` (`ProfileSettingsScreen`)
   - `settings/sessions` (`SessionSettingsScreen`)
   - `settings/bots` (`BotsSettingsScreen`) & `settings/bots/{botId}` (`BotSettingsScreen`)
   - `settings/appearance` (`AppearanceSettingsScreen` - theme, dynamic/custom color overrides, font selection)
   - `settings/chat` (`ChatSettingsScreen`)
   - `settings/voice` (`VoiceSettingsScreen`)
   - `settings/notifications` (`NotificationsSettingsScreen`)
   - `settings/language` (`LanguagePickerSettingsScreen`)
   - `settings/experiments` (`ExperimentsSettingsScreen` - including Polar UI toggle)
   - `settings/debug` (`DebugSettingsScreen` + built-in HTTP/WS Network Inspector)
2. **Channel Settings**:
   - `settings/channel/{channelId}` (`ChannelSettingsHome`)
   - `settings/channel/{channelId}/overview` (`ChannelSettingsOverview`)
   - `settings/channel/{channelId}/webhooks` (`ChannelSettingsWebhooks`) & `settings/channel/{channelId}/webhooks/{webhookId}` (`ChannelSettingsWebhook`)
   - `settings/channel/{channelId}/permissions` (`ChannelSettingsPermissions`) & `settings/channel/{channelId}/permissions/{roleId}` (`ChannelSettingsPermissionEditor`)
3. **Server Settings**:
   - `settings/server/{serverId}` (`ServerSettingsHome`)
   - `settings/server/{serverId}/identity` & `settings/server/{serverId}/identity/{userId}` (`ServerIdentitySettingsScreen`)
   - `settings/server/{serverId}/overview` (`ServerSettingsOverview`)
   - `settings/server/{serverId}/emojis` (`ServerSettingsEmojis`)
   - `settings/server/{serverId}/invites` (`ServerSettingsInvites`)
   - `settings/server/{serverId}/bans` (`ServerSettingsBans`)
   - `settings/server/{serverId}/channels` (`ServerSettingsChannels`)
   - `settings/server/{serverId}/roles` (`ServerSettingsRoles`) & `settings/server/{serverId}/roles/{roleId}` (`ServerSettingsRoleEditor`)
4. **About, Changelogs & Labs**:
   - `about` (`AboutScreen`), `about/oss` (`AttributionScreen`), `changelog/{id}` (`ReadChangelogScreen`).
   - `labs` (`LabsRootScreen`, `LabsHomeScreen`, `NewLoginExperienceMockup`, `SettingsDslSandbox`, `GradientEditorSandbox`, `NewCardSandbox`, `TelecomSandbox`).

### E. Fullscreen Voice Call & Floating Mini-Player (`VoiceSheet` & `VoiceFloatingPip`)
- Fullscreen `VoiceSheet` overlay with live participant cards, speaking/mute indicators, mute/deafen/speaker toggles, and disconnect button.
- Minimizable floating Picture-in-Picture style overlay (`VoiceFloatingPip`) while browsing other screens during an active voice call.

---

## 3. Verification
- Verify both `:app` (Android) and `:shared` (Kotlin Multiplatform Android + iOS targets) compile cleanly via `compile_applet` and Gradle checks.
- Confirm zero demo or hardcoded servers/messages remain anywhere in `:shared`.
