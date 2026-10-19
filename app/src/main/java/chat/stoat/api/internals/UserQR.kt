package chat.stoat.api.internals

import android.net.Uri
import androidx.core.net.toUri
import chat.stoat.api.StoatCbor
import chat.stoat.core.model.data.STOAT_MARKETING
import chat.stoat.core.model.schemas.User
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class UserQRContents(
    val format: String,
    val avatar: String,
    val displayName: String,
    val username: String,
    val discriminator: String,
    val id: String,
) {
    val resolvedAvatar: String?
        get() = avatar.takeIf { it.isNotBlank() && it != UserQR.MISSING_AVATAR_SENTINEL }

    val resolvedDisplayName: String?
        get() = displayName.takeIf { it.isNotBlank() && it != UserQR.MISSING_DISPLAY_NAME_SENTINEL }
}

object UserQR {
    const val MISSING_AVATAR_SENTINEL = "01JDZRBY95P8AY4CFVX16FFVWS"
    const val MISSING_DISPLAY_NAME_SENTINEL = "01JDZRDD1YZ84HA8EST2E5GVXT"
    const val MISSING_USERNAME_SENTINEL = "01JDZRBAG7AN9PGKKC5GSR7Z72"
    const val MISSING_ID_SENTINEL = "01JDZRDSJXH77K36XAFG1GX9JY"

    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    fun contents(user: User): String {
        return "$STOAT_MARKETING/qr?" + Base64.encode(
            StoatCbor.encodeToByteArray(
                UserQRContents.serializer(),
                UserQRContents(
                    format = $$"rqr$user$0",
                    avatar = user.avatar?.id ?: MISSING_AVATAR_SENTINEL,
                    displayName = user.displayName ?: MISSING_DISPLAY_NAME_SENTINEL,
                    username = user.username ?: MISSING_USERNAME_SENTINEL,
                    discriminator = user.discriminator ?: "0000",
                    id = user.id ?: MISSING_ID_SENTINEL
                )
            )
        )
    }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalSerializationApi::class)
    fun fromUri(uriString: String): UserQRContents? {
        val trimmed = uriString.trim()
        if (trimmed.isEmpty()) return null

        // Try decoding CBOR payload from query string or raw Base64
        val candidates = buildList {
            runCatching {
                val uri = trimmed.toUri()
                uri.encodedQuery?.takeIf { it.isNotBlank() }?.let { add(it) }
                uri.query?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if ('?' in trimmed) {
                add(trimmed.substringAfter('?').substringBefore('#'))
            }
            add(trimmed)
        }.distinct()

        for (candidate in candidates) {
            val cleaned = candidate
                .removePrefix("qr=")
                .removePrefix("data=")
                .let { raw ->
                    if ('%' in raw) Uri.decode(raw) else raw
                }
                .trim()
                .replace(' ', '+')
                .replace('-', '+')
                .replace('_', '/')

            if (cleaned.isEmpty()) continue

            val padded = when (cleaned.length % 4) {
                2 -> "$cleaned=="
                3 -> "$cleaned="
                else -> cleaned
            }

            val parsed = runCatching {
                val decodedBytes = Base64.decode(padded)
                StoatCbor.decodeFromByteArray(UserQRContents.serializer(), decodedBytes)
            }.getOrNull()

            if (parsed != null && parsed.username.isNotBlank() && parsed.username != MISSING_USERNAME_SENTINEL) {
                return parsed
            }
        }

        // Fallback: support direct username#discriminator QR strings
        if ('#' in trimmed && !trimmed.contains("://")) {
            val parts = trimmed.split('#')
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].all { it.isDigit() }) {
                return UserQRContents(
                    format = $$"rqr$user$0",
                    avatar = MISSING_AVATAR_SENTINEL,
                    displayName = parts[0].trim(),
                    username = parts[0].trim(),
                    discriminator = parts[1].trim(),
                    id = ""
                )
            }
        }

        return null
    }
}