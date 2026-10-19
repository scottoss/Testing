package chat.stoat.core.model.schemas

import chat.stoat.core.model.data.DAWNCHAT_INVITE_SHORT
import chat.stoat.core.model.data.STOAT_BETA_WEB_APP
import chat.stoat.core.model.data.STOAT_WEB_APP
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Invite(
    val type: String? = null,
    val code: String? = null,

    @SerialName("server_id")
    val serverId: String? = null,

    @SerialName("server_name")
    val serverName: String? = null,

    @SerialName("server_icon")
    val serverIcon: AutumnResource? = null,

    @SerialName("server_banner")
    val serverBanner: AutumnResource? = null,

    @SerialName("server_flags")
    val serverFlags: Long? = null,

    @SerialName("channel_id")
    val channelId: String? = null,

    @SerialName("channel_name")
    val channelName: String? = null,

    @SerialName("user_name")
    val userName: String? = null,

    @SerialName("user_avatar")
    val userAvatar: AutumnResource? = null,

    @SerialName("member_count")
    val memberCount: Long? = null
)

@Serializable
data class InviteJoined(
    val type: String? = null,
    val channels: List<Channel>? = null,
    val server: Server? = null
)

@Serializable
data class ServerInvite(
    val type: String? = "Server",
    @SerialName("_id")
    val id: String = "",
    val code: String? = null,
    val server: String? = null,
    val creator: String? = null,
    val channel: String? = null,
) {
    val effectiveCode: String
        get() = if (id.isNotBlank()) id else (code ?: "")
}

private fun extractHostFromUrl(rawUrl: String): String? {
    val withoutScheme = rawUrl.trim()
        .substringAfter("://", "")
        .takeIf { it.isNotEmpty() } ?: return null
    return withoutScheme
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringBefore(':')
        .lowercase()
        .takeIf { it.isNotBlank() }
}

private fun extractPathSegmentsFromUrl(rawUrl: String): List<String> {
    val withoutScheme = rawUrl.trim()
        .substringAfter("://", "")
        .takeIf { it.isNotEmpty() } ?: return emptyList()
    val pathPart = withoutScheme
        .substringAfter('/', "")
        .substringBefore('?')
        .substringBefore('#')
    if (pathPart.isBlank()) return emptyList()
    return pathPart.split('/').filter { it.isNotBlank() }
}

fun isInviteUrl(rawUrl: String): Boolean {
    val host = extractHostFromUrl(rawUrl) ?: return false
    val pathSegments = extractPathSegmentsFromUrl(rawUrl)

    val isDawnChat = host == "dawn-chat.com" || host == "www.dawn-chat.com" ||
        host == "chat.dawn-chat.com" || host == "app.dawn-chat.com" ||
        host == extractHostFromUrl(STOAT_WEB_APP) || host == extractHostFromUrl(STOAT_BETA_WEB_APP)
    val isStoatChat = host == "stoat.chat" || host == "www.stoat.chat"
    val isRevoltChat = host == "revolt.chat" || host == "app.revolt.chat" || host == "www.revolt.chat"
    val isCsInvite = host == "csinvite.net" || host == "www.csinvite.net" ||
        host == extractHostFromUrl(DAWNCHAT_INVITE_SHORT)
    val isShortDomain = host == "rvlt.gg" || host == "stt.gg"

    if (isDawnChat || isStoatChat || isRevoltChat) {
        return pathSegments.firstOrNull() == "invite" && pathSegments.size >= 2
    }
    if (isCsInvite) {
        if (pathSegments.firstOrNull() == "dc" && pathSegments.size >= 2) return true
        return pathSegments.size == 1
    }
    if (isShortDomain) {
        return pathSegments.size == 1
    }

    return false
}
