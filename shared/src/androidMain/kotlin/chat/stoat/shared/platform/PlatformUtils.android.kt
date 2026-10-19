package chat.stoat.shared.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object AndroidPlatformContext {
    @Volatile
    var applicationContext: Context? = null
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}

actual object PlatformInfo {
    actual val platformName: String = "Android"
    actual val osVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    actual val appVersion: String = "v0.0.8"
    actual val isIos: Boolean = false
    actual val isAndroid: Boolean = true

    actual fun openExternalUrl(url: String) {
        val ctx = AndroidPlatformContext.applicationContext ?: return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        }
    }

    actual fun copyToClipboard(label: String, text: String) {
        val ctx = AndroidPlatformContext.applicationContext ?: return
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    actual fun shareText(text: String) {
        val ctx = AndroidPlatformContext.applicationContext ?: return
        runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(chooser)
        }
    }
}
