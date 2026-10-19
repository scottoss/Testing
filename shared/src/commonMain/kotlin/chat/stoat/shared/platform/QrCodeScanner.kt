package chat.stoat.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class FriendQrPayload(
    val identifier: String,
    val userId: String? = null,
    val usernameTag: String? = null,
    val rawValue: String
)

object FriendQrParser {
    private val ulidRegex = Regex("^[0-9A-HJKMNP-TV-Z]{26}$", RegexOption.IGNORE_CASE)
    private val discriminatorTagRegex = Regex("^[^#\\s]{1,32}#\\d{4}$")

    fun parse(rawInput: String): FriendQrPayload? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null

        // 1. JSON payload: {"id":"...", "username":"...", "discriminator":"..."}
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            runCatching {
                val obj = Json.parseToJsonElement(trimmed).jsonObject
                val id = obj["userId"]?.jsonPrimitive?.contentOrNull
                    ?: obj["user_id"]?.jsonPrimitive?.contentOrNull
                    ?: obj["id"]?.jsonPrimitive?.contentOrNull
                    ?: obj["_id"]?.jsonPrimitive?.contentOrNull
                val username = obj["username"]?.jsonPrimitive?.contentOrNull
                val discriminator = obj["discriminator"]?.jsonPrimitive?.contentOrNull
                val tag = if (!username.isNullOrBlank() && !discriminator.isNullOrBlank()) {
                    "$username#$discriminator"
                } else {
                    username
                }
                val identifier = tag ?: id
                if (!identifier.isNullOrBlank()) {
                    return FriendQrPayload(
                        identifier = identifier,
                        userId = id,
                        usernameTag = tag,
                        rawValue = trimmed
                    )
                }
            }
        }

        // 2. Deep link / URL format: dawnchat://friend/... or https://dawn-chat.com/friend/...
        if (trimmed.contains("://")) {
            val withoutQuery = trimmed.substringBefore('?').substringBefore('#')
            val segments = withoutQuery.substringAfter("://").split('/').filter { it.isNotBlank() }
            val friendIdx = segments.indexOfFirst {
                it.equals("friend", ignoreCase = true) ||
                    it.equals("friends", ignoreCase = true) ||
                    it.equals("user", ignoreCase = true) ||
                    it.equals("users", ignoreCase = true) ||
                    it.equals("add", ignoreCase = true)
            }
            val candidate = if (friendIdx >= 0 && friendIdx + 1 < segments.size) {
                segments[friendIdx + 1]
            } else {
                segments.lastOrNull()
            }
            if (!candidate.isNullOrBlank()) {
                val decoded = candidate.replace("%23", "#").replace("%20", " ")
                return FriendQrPayload(
                    identifier = decoded,
                    userId = decoded.takeIf { ulidRegex.matches(it) },
                    usernameTag = decoded.takeIf { discriminatorTagRegex.matches(it) },
                    rawValue = trimmed
                )
            }
        }

        // 3. Direct ULID or Username#Discriminator
        if (ulidRegex.matches(trimmed)) {
            return FriendQrPayload(
                identifier = trimmed,
                userId = trimmed,
                usernameTag = null,
                rawValue = trimmed
            )
        }
        if (discriminatorTagRegex.matches(trimmed)) {
            return FriendQrPayload(
                identifier = trimmed,
                userId = null,
                usernameTag = trimmed,
                rawValue = trimmed
            )
        }

        return FriendQrPayload(
            identifier = trimmed,
            rawValue = trimmed
        )
    }
}

@Composable
expect fun PlatformQrScannerView(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit
)
