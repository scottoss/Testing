# Kotlin Multiplatform (KMP) & Compose Multiplatform Migration Plan

## Overview
We will convert the project into a **Kotlin Multiplatform (KMP)** architecture using **Compose Multiplatform** so the codebase can compile for both **Android** and **iOS** (`iosArm64`, `iosSimulatorArm64`, `iosX64`), while keeping the Android build (`:app`) fully functional.

---

## 1. Gradle & Multiplatform Module Setup
- **Version Catalog (`gradle/libs.versions.toml`) & Root `build.gradle.kts`**:
  - Register the Kotlin Multiplatform plugin (`org.jetbrains.kotlin.multiplatform`) and JetBrains Compose Multiplatform plugin (`org.jetbrains.compose`).
- **Convert `:core:model` to Kotlin Multiplatform**:
  - Update `core/model/build.gradle.kts` to apply `kotlin("multiplatform")` and configure `androidTarget()`, `iosX64()`, `iosArm64()`, and `iosSimulatorArm64()` targets.
  - Move shared data models into `commonMain` and replace Android-specific `@Parcelize` / `Parcelable` annotations with multiplatform expect/actual definitions (`@CommonParcelize` / `CommonParcelable`) so models compile cleanly on both Android and iOS.
- **Create `:shared` Multiplatform Module**:
  - Add `include(":shared")` to `settings.gradle.kts`.
  - Configure `shared/build.gradle.kts` with Kotlin Multiplatform + Compose Multiplatform (`commonMain`, `androidMain`, `iosMain`) and export a static/dynamic `SharedViewController` framework (`DawnChatShared`) for Xcode.

---

## 2. Shared Logic & Compose Multiplatform UI (`:shared`)
- **Shared Core & Networking (`commonMain`)**:
  - Extract multiplatform-compatible networking (Ktor client, WebSocket event handling, GIF service, serialization, and repository interfaces) and shared state models into `:shared`.
- **Shared Compose UI (`commonMain`)**:
  - Provide the shared Compose Multiplatform theme, navigation entrypoint (`DawnChatMultiplatformApp`), and core shared composables that render across Android and iOS.
  - Connect Android `:app` to `:shared` so the Android application uses the shared module seamlessly.

---

## 3. Platform Abstractions (`expect` / `actual`) with iOS Stubs
- Define clean `expect` declarations in `shared/src/commonMain` with full Android implementations in `shared/src/androidMain` (backed by existing Android services) and safe, compilable stubs in `shared/src/iosMain`:
  - **`VoiceCallController`**: Abstracts LiveKit voice room connection, mute/deafen toggles, and participant audio state (Android delegates to LiveKit/VoiceService; iOS provides a safe no-op stub with status reporting).
  - **`QrCodeScanner`**: Abstracts camera QR code scanning for adding friends (Android delegates to CameraX + ZXing; iOS provides a stub composable/callback prompt).
  - **`MediaPlayerController`**: Abstracts looping GIF/video playback and voice message playback (Android uses Media3 ExoPlayer; iOS provides a fallback view/stub).
  - **`PlatformContext` & System Utilities**: Abstracts clipboard, URL launching, share sheet, and platform version info (`NSBundle` on iOS, `PackageManager` on Android).

---

## 4. iOS Entrypoint & Xcode Project Skeleton (`iosApp/`)
- **`MainViewController.kt` (`shared/src/iosMain`)**:
  - Expose `fun MainViewController(): UIViewController = ComposeUIViewController { DawnChatMultiplatformApp() }` so iOS can host the Compose Multiplatform UI directly inside SwiftUI.
- **`iosApp/` Directory**:
  - Create the SwiftUI host entry files (`iOSApp.swift`, `ContentView.swift`, and `Info.plist` with camera/microphone usage descriptions) configured to embed the `DawnChatShared` KMP framework when opened in Xcode on macOS.
