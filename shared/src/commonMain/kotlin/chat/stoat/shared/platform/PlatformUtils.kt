package chat.stoat.shared.platform

expect object PlatformInfo {
    val platformName: String
    val osVersion: String
    val appVersion: String
    val isIos: Boolean
    val isAndroid: Boolean

    fun openExternalUrl(url: String)
    fun copyToClipboard(label: String, text: String)
    fun shareText(text: String)
}

/**
 * Pure Kotlin RFC 3986 percent-encoder for URL query parameters across Android and iOS.
 */
fun encodeUrlQueryComponent(value: String): String {
    val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
    val bytes = value.encodeToByteArray()
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
        val intVal = b.toInt() and 0xFF
        val ch = intVal.toChar()
        if (ch in unreserved) {
            sb.append(ch)
        } else if (ch == ' ') {
            sb.append("%20")
        } else {
            sb.append('%')
            sb.append(intVal.toString(16).uppercase().padStart(2, '0'))
        }
    }
    return sb.toString()
}
