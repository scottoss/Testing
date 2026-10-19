package chat.stoat.shared

import androidx.compose.ui.window.ComposeUIViewController
import chat.stoat.shared.ui.DawnChatMultiplatformApp
import platform.UIKit.UIViewController

/**
 * Entry point for hosting the shared Compose Multiplatform UI inside SwiftUI on iOS.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    DawnChatMultiplatformApp()
}
