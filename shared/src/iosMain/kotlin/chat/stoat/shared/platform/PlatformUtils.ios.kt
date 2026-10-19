package chat.stoat.shared.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIPasteboard

actual object PlatformInfo {
    actual val platformName: String = "iOS"
    actual val osVersion: String =
        "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"
    actual val appVersion: String =
        (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String)
            ?.let { if (it.startsWith("v")) it else "v$it" }
            ?: "v0.0.8"
    actual val isIos: Boolean = true
    actual val isAndroid: Boolean = false

    actual fun openExternalUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(
            url = nsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = null
        )
    }

    actual fun copyToClipboard(label: String, text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    actual fun shareText(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}
