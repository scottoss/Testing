package chat.stoat.shared.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
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

actual object SharedKVStorage {
    private const val PREFIX = "dawnchat_kv_"
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, defaultValue: String): String {
        return defaults.stringForKey("$PREFIX$key") ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = "$PREFIX$key")
        defaults.synchronize()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val fullKey = "$PREFIX$key"
        if (defaults.objectForKey(fullKey) == null) return defaultValue
        return defaults.boolForKey(fullKey)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = "$PREFIX$key")
        defaults.synchronize()
    }

    actual fun remove(key: String) {
        defaults.removeObjectForKey("$PREFIX$key")
        defaults.synchronize()
    }

    actual fun clearAll() {
        val dict = defaults.dictionaryRepresentation()
        for (rawKey in dict.keys) {
            val k = rawKey as? String ?: continue
            if (k.startsWith(PREFIX)) {
                defaults.removeObjectForKey(k)
            }
        }
        defaults.synchronize()
    }
}
