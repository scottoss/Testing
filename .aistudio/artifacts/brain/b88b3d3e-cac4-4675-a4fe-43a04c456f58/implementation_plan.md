# Fix iOS Release Framework Heap OOM & Restore DM / Group DM Header Call Buttons

This plan resolves the `java.lang.OutOfMemoryError: Java heap space` failure during `:shared:linkReleaseFrameworkIosArm64` in the iOS App Store build pipeline and restores the **Call** icon button in the top header bar for **Private DMs** and **Group DMs** across both Android and iOS.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions govern this update based on your requirements and clarification:

- **Confirmed Decision 1 — Private DM & Group DM Call Button Placement**: Display the **Call** action for Private DMs (`ChannelType.DirectMessage`) and Group DMs (`ChannelType.Group`) **as an icon button in the top header bar only** on both Android and iOS, while keeping the **Join Voice Channel** banner card exclusively inside server Voice Channels (`ChannelType.VoiceChannel`).
- **Confirmed Decision 2 — iOS KMP Release Linker Memory Configuration**: Increase JVM heap and metaspace limits for Gradle and the Kotlin Native compiler daemon so whole-program `DevirtualizationAnalysis` during `:shared:linkReleaseFrameworkIosArm64` completes without running out of heap space.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. Fixes the `OutOfMemoryError: Java heap space` crash during `:shared:linkReleaseFrameworkIosArm64` when compiling the release KMP framework for iOS App Store / TestFlight builds.
  2. Restores the **Call** (`ic_call_24dp` / `Icons.Default.Call`) button in the top app bar of Private DMs and Group DMs on both Android and iOS so users can start or join a DM/Group voice call with a single tap from the header.
- **Target Audience / Persona**: DawnChat users calling friends in 1-on-1 Direct Messages or Group DMs on Android and iOS, and CI/CD pipelines archiving signed iOS builds.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Starting or Joining a Call in a Private DM or Group DM (Android & iOS)**:
     - User opens any Private DM (`DirectMessage`) or Group DM (`Group`).
     - The `TopAppBar` header displays a **Call** icon button (`testTag("dm_header_call_button")`) alongside the channel actions.
     - Tapping the **Call** icon button connects to the DM/Group voice room and opens the fullscreen Voice Call sheet (or re-opens the sheet if already connected).
  2. **Server Voice Channels (Android & iOS)**:
     - Server Voice Channels (`ChannelType.VoiceChannel`) continue to display the **Join Voice Channel** banner card at the top of the channel view with no duplicate header call icon.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Header-Only Call Action for DMs & Group DMs vs. Banner Card for Voice Channels**
  - *Chosen Approach*: Show the Call icon button in the `TopAppBar` actions strictly when `channelType == DirectMessage || channelType == Group`, and show the Join Call banner card strictly when `channelType == VoiceChannel`.
  - *Why*: Keeps 1-on-1 and Group DM text conversations uncluttered by a permanent voice banner while providing instant 1-tap voice call access in the top header bar.
- **Decision 2: Tiered JVM Heap Allocation for Local/Container vs. macOS CI Release Linking**
  - *Chosen Approach*: Configure `gradle.properties` with `-Xmx4g -XX:MaxMetaspaceSize=1g` for both `org.gradle.jvmargs` and `kotlin.daemon.jvmargs` (safe for 12 GB container environments), and pass `-Dorg.gradle.jvmargs="-Xmx8g -XX:MaxMetaspaceSize=2g -XX:+UseParallelGC -Dfile.encoding=UTF-8"` and `-Dkotlin.daemon.jvmargs="-Xmx8g -XX:MaxMetaspaceSize=2g"` in `scripts/build_and_upload_ios.sh` for macOS release framework linking.
  - *Why*: Kotlin/Native's release LTO `DevirtualizationAnalysis` phase across Compose Multiplatform + Material 3 + Ktor requires >2 GiB of heap; 8 GiB on the 14 GiB macOS runner prevents OOM during release framework linking while 4 GiB in `gradle.properties` remains safe for standard container builds.

---

## 4. Technical Architecture & Changes *(Technical Reference)*

```
┌──────────────────────────────────────────────────────────────────────────┐
│                     Channel Screen (Android & iOS)                       │
├───────────────────────────────────┬──────────────────────────────────────┤
│ Private DM / Group DM             │ Server Voice Channel                 │
│ (DirectMessage / Group)           │ (VoiceChannel)                       │
│                                   │                                      │
│ TopAppBar Actions:                │ TopAppBar Actions:                   │
│ [ 📞 Call ] [ 🔍 Search ] ...     │ [ 🔍 Search ] ...                    │
│      │                            │                                      │
│      ▼                            │ Top of Channel Body:                 │
│ Connects & Opens Voice Call Sheet │ ┌──────────────────────────────────┐ │
│                                   │ │ Join Voice Channel Banner Card   │ │
│                                   │ │ [ 📞 Join Call ]                 │ │
│                                   │ └──────────────────────────────────┘ │
└───────────────────────────────────┴──────────────────────────────────────┘
```

1. **Gradle & iOS CI Memory Configuration**:
   - `gradle.properties`: Update `org.gradle.jvmargs` from `-Xmx2048m` to `-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8` and add `kotlin.daemon.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m`.
   - `scripts/build_and_upload_ios.sh`: Pass `-Dorg.gradle.jvmargs="-Xmx8g -XX:MaxMetaspaceSize=2g -XX:+UseParallelGC -Dfile.encoding=UTF-8"` and `-Dkotlin.daemon.jvmargs="-Xmx8g -XX:MaxMetaspaceSize=2g"` to `./gradlew :shared:linkReleaseFrameworkIosArm64`.
   - `shared/build.gradle.kts` & `core/model/build.gradle.kts`: Add `-Xexpect-actual-classes` compiler argument so `expect`/`actual` beta warnings do not clutter CI logs.
2. **Android Private DM & Group DM Call Button (`app/src/main/java/chat/stoat/screens/chat/views/channel/ChannelScreen.kt`)**:
   - In `ChannelScreen`'s `TopAppBar` `actions`, render an `IconButton` with `R.drawable.ic_call_24dp` when `viewModel.channel?.channelType == ChannelType.DirectMessage || viewModel.channel?.channelType == ChannelType.Group` and `channelPermissions has PermissionBit.Connect`.
   - Tapping the button calls `VoiceCallManager.openCall(channelId)` and sends `Action.OpenVoiceChannelOverlay(channelId)`.
   - Restrict `JoinVoiceChannelButton(channelId)` in the message list header to `viewModel.channel?.channelType == ChannelType.VoiceChannel`.
3. **iOS / Multiplatform Private DM & Group DM Call Button (`shared/src/commonMain/kotlin/chat/stoat/shared/ui/SharedChatScreens.kt`)**:
   - In `SharedChannelScreen`'s `TopAppBar` `actions`, render an `IconButton` with `Icons.Default.Call` (tinted with `Color(0xFF22C55E)` when a call is active in that DM/Group, or `MaterialTheme.colorScheme.onSurface` otherwise) when `channel?.channelType == ChannelType.DirectMessage || channel?.channelType == ChannelType.Group`.
   - Tapping the button connects via `SharedStoatClient.joinVoiceCall(channelId)` + `voiceController.connect(...)` and invokes `onOpenVoiceFullscreen()`.
   - Restrict `SharedJoinVoiceChannelBanner` to `channel?.channelType == ChannelType.VoiceChannel`.
