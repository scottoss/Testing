package chat.stoat.shared.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.stoat.core.model.data.STOAT_BASE
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.data.STOAT_INVITES
import chat.stoat.core.model.data.STOAT_PROXY
import chat.stoat.core.model.data.STOAT_WEBSOCKET
import chat.stoat.core.model.data.STOAT_WEB_APP
import chat.stoat.core.model.schemas.AutumnResource
import chat.stoat.core.model.schemas.BanListResult
import chat.stoat.core.model.schemas.Category
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.ChannelUnreadResponse
import chat.stoat.core.model.schemas.ContentReportReason
import chat.stoat.core.model.schemas.Emoji
import chat.stoat.core.model.schemas.EmojiParent
import chat.stoat.core.model.schemas.FullMessageReport
import chat.stoat.core.model.schemas.FullServerReport
import chat.stoat.core.model.schemas.FullUserReport
import chat.stoat.core.model.schemas.HealthNotice
import chat.stoat.core.model.schemas.Invite
import chat.stoat.core.model.schemas.InviteJoined
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.Message
import chat.stoat.core.model.schemas.MessageReport
import chat.stoat.core.model.schemas.MessagesInChannel
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Profile
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.ServerInvite
import chat.stoat.core.model.schemas.ServerReport
import chat.stoat.core.model.schemas.ServerWithChannelObjects
import chat.stoat.core.model.schemas.Session
import chat.stoat.core.model.schemas.Status
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.schemas.UserReport
import chat.stoat.core.model.schemas.UserReportReason
import chat.stoat.core.model.util.ChannelVoiceState
import chat.stoat.shared.platform.PlatformInfo
import chat.stoat.shared.platform.SharedKVStorage
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.random.Random

@Serializable
data class SharedRootFeatureItem(
    val enabled: Boolean = false,
    val url: String = "",
    val ws: String = "",
    val key: String = ""
)

@Serializable
data class SharedRootFeatures(
    val captcha: SharedRootFeatureItem = SharedRootFeatureItem(),
    val email: Boolean = true,
    @SerialName("invite_only") val inviteOnly: Boolean = false,
    val autumn: SharedRootFeatureItem = SharedRootFeatureItem(enabled = true, url = STOAT_FILES),
    val january: SharedRootFeatureItem = SharedRootFeatureItem(enabled = true, url = STOAT_PROXY),
    val voso: SharedRootFeatureItem = SharedRootFeatureItem(),
    val livekit: SharedRootFeatureItem = SharedRootFeatureItem()
)

@Serializable
data class SharedRootConfig(
    val revolt: String = "0.8.0",
    val features: SharedRootFeatures = SharedRootFeatures(),
    val ws: String = STOAT_WEBSOCKET,
    val app: String = STOAT_WEB_APP,
    val vapid: String = ""
)

@Serializable
data class SharedLoginMfaSpec(
    val result: String = "MFA",
    val ticket: String = "",
    @SerialName("allowed_methods") val allowedMethods: List<String> = emptyList()
)

@Serializable
data class SharedUserSessionHints(
    val result: String = "Success",
    @SerialName("_id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val token: String = "",
    val name: String = ""
)

data class SharedLoginAssessment(
    val success: Boolean = false,
    val proceedMfa: Boolean = false,
    val mfaSpec: SharedLoginMfaSpec? = null,
    val sessionHints: SharedUserSessionHints? = null,
    val error: String? = null
)

@Serializable
data class SharedMfaTicket(
    @SerialName("_id") val id: String = "",
    @SerialName("account_id") val accountId: String = "",
    val token: String = "",
    val validated: Boolean = false,
    val authorised: Boolean = false,
    @SerialName("last_totp_code") val lastTotpCode: String? = null
)

@Serializable
data class SharedOwnedBot(
    @SerialName("_id") val id: String,
    val token: String = "",
    @SerialName("public") val isPublic: Boolean = false
)

@Serializable
data class SharedOwnedBotUser(
    @SerialName("_id") val id: String,
    val username: String,
    val discriminator: String = "0000"
)

@Serializable
data class SharedOwnedBotsResponse(
    val bots: List<SharedOwnedBot> = emptyList(),
    val users: List<SharedOwnedBotUser> = emptyList()
)

@Serializable
data class SharedWebhookAvatar(
    @SerialName("_id") val id: String
)

@Serializable
data class SharedWebhook(
    val id: String,
    val name: String,
    val avatar: SharedWebhookAvatar? = null,
    @SerialName("channel_id") val channelId: String = "",
    val token: String? = null
)

@Serializable
data class SharedFetchMembersResponse(
    val members: List<Member> = emptyList(),
    val users: List<User> = emptyList()
)

@Serializable
data class SharedMutuals(
    val users: List<String> = emptyList(),
    val servers: List<String> = emptyList()
)

@Serializable
data class SharedJoinCallResponse(
    val token: String,
    val url: String = ""
)

@Serializable
data class SharedSendMessageReply(
    val id: String,
    val mention: Boolean = true
)

@Serializable
data class SharedChangelogEntry(
    val id: String,
    val title: String,
    val date: String,
    val summary: String,
    val sections: List<String>
)

object SharedUlid {
    private const val CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun next(): String {
        var time = Clock.System.now().toEpochMilliseconds()
        val chars = CharArray(26)
        for (i in 9 downTo 0) {
            chars[i] = CROCKFORD[(time and 31L).toInt()]
            time = time ushr 5
        }
        for (i in 10 until 26) {
            chars[i] = CROCKFORD[Random.nextInt(32)]
        }
        return chars.concatToString()
    }
}

object SharedStoatClient {
    const val TOKEN_HEADER_NAME = "x-session-token"
    const val MFA_TICKET_HEADER_NAME = "x-mfa-ticket"

    // Persistent keys
    private const val KEY_API_BASE = "api_base_url"
    private const val KEY_SESSION_TOKEN = "session_token"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_SELF_ID = "self_user_id"
    private const val KEY_USE_POLAR = "exp_use_polar"
    private const val KEY_AVATAR_PICKERS = "exp_avatar_pickers"
    private const val KEY_DISCOVER_IN_APP = "exp_discover_in_app"
    private const val KEY_GIF_PICKER_KLIPY = "exp_gif_picker_klipy"
    private const val KEY_THEME_MODE = "appearance_theme_mode"
    private const val KEY_FONT_NAME = "appearance_font_name"
    private const val KEY_AVATAR_RADIUS = "appearance_avatar_radius"
    private const val KEY_REPLY_STYLE = "chat_reply_style"
    private const val KEY_COMPOSER_BLUR = "chat_composer_blur"
    private const val KEY_EMBED_YOUTUBE = "chat_embed_youtube"
    private const val KEY_EMBED_APPLE_MUSIC = "chat_embed_apple_music"
    private const val KEY_COMPACT_MODE = "chat_compact_mode"
    private const val KEY_SHOW_TIMESTAMPS = "chat_show_timestamps"
    private const val KEY_VOICE_NOISE_SUPPRESSION = "voice_noise_suppression"
    private const val KEY_VOICE_ECHO_CANCELLATION = "voice_echo_cancellation"
    private const val KEY_VOICE_AUTO_GAIN = "voice_auto_gain"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_LANGUAGE_CODE = "language_code"
    private const val KEY_NSFW_ACKNOWLEDGED = "nsfw_channels_acknowledged"

    var apiBaseUrl by mutableStateOf(
        SharedKVStorage.getString(KEY_API_BASE, STOAT_BASE).trimEnd('/')
    )
        private set

    var sessionToken by mutableStateOf(SharedKVStorage.getString(KEY_SESSION_TOKEN, ""))
        private set

    var sessionId by mutableStateOf(SharedKVStorage.getString(KEY_SESSION_ID, ""))
        private set

    var selfId by mutableStateOf(SharedKVStorage.getString(KEY_SELF_ID, "").takeIf { it.isNotBlank() })
        private set

    var rootConfig by mutableStateOf<SharedRootConfig?>(null)
        private set

    var healthNotice by mutableStateOf<HealthNotice?>(null)
        private set

    var isGeoRestricted by mutableStateOf(false)
        private set

    // Reactive live caches (populated strictly from live REST & WebSocket events — zero demo data)
    val userCache = mutableStateMapOf<String, User>()
    val serverCache = mutableStateMapOf<String, Server>()
    val channelCache = mutableStateMapOf<String, Channel>()
    val emojiCache = mutableStateMapOf<String, Emoji>()
    val voiceStateCache = mutableStateMapOf<String, ChannelVoiceState>()

    // serverId -> (userId -> Member)
    val memberCache = mutableStateMapOf<String, Map<String, Member>>()

    // channelId -> list of messages (sorted oldest to newest for chat window)
    val channelMessages = mutableStateMapOf<String, List<Message>>()

    // channelId -> list of pinned messages
    val channelPinnedMessages = mutableStateMapOf<String, List<Message>>()

    // channelId -> list of search result messages
    val channelSearchResults = mutableStateMapOf<String, List<Message>>()

    // channelId -> set of userIds currently typing
    val typingUsersByChannel = mutableStateMapOf<String, Set<String>>()

    // channelId -> last read message ID
    val unreadLastReadByChannel = mutableStateMapOf<String, String>()

    // channelId -> list of mention message IDs
    val unreadMentionsByChannel = mutableStateMapOf<String, List<String>>()

    // Server & Channel management caches
    val sessionsList = mutableStateListOf<Session>()
    val ownedBots = mutableStateListOf<SharedOwnedBot>()
    val ownedBotUsers = mutableStateMapOf<String, SharedOwnedBotUser>()
    val serverBansByServer = mutableStateMapOf<String, BanListResult>()
    val serverInvitesByServer = mutableStateMapOf<String, List<ServerInvite>>()
    val webhooksByChannel = mutableStateMapOf<String, List<SharedWebhook>>()
    val profileByUser = mutableStateMapOf<String, Profile>()
    val mutualsByUser = mutableStateMapOf<String, SharedMutuals>()
    val mfaEnabledMethods = mutableStateListOf<String>()

    // Age-gated NSFW channels acknowledged by the user in this session/device
    val acknowledgedNsfwChannels = mutableStateListOf<String>()

    // Persisted Preferences & Experiments
    var usePolarLayout by mutableStateOf(SharedKVStorage.getBoolean(KEY_USE_POLAR, false))
        private set
    val polarLayoutEnabled: Boolean
        get() = usePolarLayout
    var dynamicColorEnabled by mutableStateOf(false)
    var enableAvatarPickers by mutableStateOf(SharedKVStorage.getBoolean(KEY_AVATAR_PICKERS, true))
        private set
    var enableInAppDiscover by mutableStateOf(SharedKVStorage.getBoolean(KEY_DISCOVER_IN_APP, true))
        private set
    var enableKlipyGifs by mutableStateOf(SharedKVStorage.getBoolean(KEY_GIF_PICKER_KLIPY, true))
        private set

    var themeMode by mutableStateOf(SharedKVStorage.getString(KEY_THEME_MODE, "Default"))
        private set
    var fontName by mutableStateOf(SharedKVStorage.getString(KEY_FONT_NAME, "Default"))
        private set
    var avatarRadiusPercent by mutableStateOf(
        SharedKVStorage.getString(KEY_AVATAR_RADIUS, "50").toIntOrNull()?.coerceIn(0, 50) ?: 50
    )
        private set
    var messageReplyStyle by mutableStateOf(SharedKVStorage.getString(KEY_REPLY_STYLE, "SwipeFromEnd"))
        private set
    var composerBlurEnabled by mutableStateOf(SharedKVStorage.getBoolean(KEY_COMPOSER_BLUR, true))
        private set
    var embedYouTube by mutableStateOf(SharedKVStorage.getBoolean(KEY_EMBED_YOUTUBE, true))
        private set
    var embedAppleMusic by mutableStateOf(SharedKVStorage.getBoolean(KEY_EMBED_APPLE_MUSIC, false))
        private set
    var compactChatMode by mutableStateOf(SharedKVStorage.getBoolean(KEY_COMPACT_MODE, false))
        private set
    var showMessageTimestamps by mutableStateOf(SharedKVStorage.getBoolean(KEY_SHOW_TIMESTAMPS, true))
        private set

    var voiceNoiseSuppression by mutableStateOf(SharedKVStorage.getBoolean(KEY_VOICE_NOISE_SUPPRESSION, true))
        private set
    var voiceEchoCancellation by mutableStateOf(SharedKVStorage.getBoolean(KEY_VOICE_ECHO_CANCELLATION, true))
        private set
    var voiceAutoGain by mutableStateOf(SharedKVStorage.getBoolean(KEY_VOICE_AUTO_GAIN, true))
        private set

    var notificationsEnabled by mutableStateOf(SharedKVStorage.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
        private set
    var languageCode by mutableStateOf(SharedKVStorage.getString(KEY_LANGUAGE_CODE, "en"))
        private set

    // Per-channel / per-server notification setting ("All", "Mentions", "None")
    val serverNotificationOverrides = mutableStateMapOf<String, String>()
    val channelNotificationOverrides = mutableStateMapOf<String, String>()

    // Server ordering from SyncedSettings
    val orderedServerIds = mutableStateListOf<String>()

    init {
        val savedNsfw = SharedKVStorage.getString(KEY_NSFW_ACKNOWLEDGED, "")
        if (savedNsfw.isNotBlank()) {
            acknowledgedNsfwChannels.addAll(savedNsfw.split(',').filter { it.isNotBlank() })
        }
    }

    val isLoggedIn: Boolean
        get() = sessionToken.isNotBlank()

    val currentUser: User?
        get() = selfId?.let { userCache[it] }

    val autumnBaseUrl: String
        get() = rootConfig?.features?.autumn?.url?.takeIf { it.isNotBlank() }?.trimEnd('/') ?: STOAT_FILES

    val januaryBaseUrl: String
        get() = rootConfig?.features?.january?.url?.takeIf { it.isNotBlank() }?.trimEnd('/') ?: STOAT_PROXY

    val wsUrl: String
        get() = rootConfig?.ws?.takeIf { it.isNotBlank() } ?: STOAT_WEBSOCKET

    val webAppUrl: String
        get() = rootConfig?.app?.takeIf { it.isNotBlank() }?.trimEnd('/') ?: STOAT_WEB_APP

    fun updateApiBaseUrl(newUrl: String) {
        val cleaned = newUrl.trim().trimEnd('/').ifBlank { STOAT_BASE }
        apiBaseUrl = cleaned
        SharedKVStorage.putString(KEY_API_BASE, cleaned)
        SharedStoatApi.apiBaseUrl = cleaned
    }

    fun saveSession(token: String, sessionIdValue: String, userIdValue: String) {
        sessionToken = token
        sessionId = sessionIdValue
        selfId = userIdValue.takeIf { it.isNotBlank() }
        SharedKVStorage.putString(KEY_SESSION_TOKEN, token)
        SharedKVStorage.putString(KEY_SESSION_ID, sessionIdValue)
        SharedKVStorage.putString(KEY_SELF_ID, userIdValue)
    }

    fun clearSession() {
        SharedStoatRealtime.disconnect()
        sessionToken = ""
        sessionId = ""
        selfId = null
        SharedKVStorage.remove(KEY_SESSION_TOKEN)
        SharedKVStorage.remove(KEY_SESSION_ID)
        SharedKVStorage.remove(KEY_SELF_ID)
        userCache.clear()
        serverCache.clear()
        channelCache.clear()
        emojiCache.clear()
        voiceStateCache.clear()
        memberCache.clear()
        channelMessages.clear()
        channelPinnedMessages.clear()
        channelSearchResults.clear()
        typingUsersByChannel.clear()
        unreadLastReadByChannel.clear()
        unreadMentionsByChannel.clear()
        sessionsList.clear()
        ownedBots.clear()
        ownedBotUsers.clear()
        serverBansByServer.clear()
        serverInvitesByServer.clear()
        webhooksByChannel.clear()
        profileByUser.clear()
        mutualsByUser.clear()
        mfaEnabledMethods.clear()
    }

    fun acknowledgeNsfwChannel(channelId: String) {
        if (channelId !in acknowledgedNsfwChannels) {
            acknowledgedNsfwChannels.add(channelId)
            SharedKVStorage.putString(KEY_NSFW_ACKNOWLEDGED, acknowledgedNsfwChannels.joinToString(","))
        }
    }

    // Settings updaters
    fun updateUsePolarLayout(enabled: Boolean) {
        usePolarLayout = enabled
        SharedKVStorage.putBoolean(KEY_USE_POLAR, enabled)
    }

    fun updateAvatarPickers(enabled: Boolean) {
        enableAvatarPickers = enabled
        SharedKVStorage.putBoolean(KEY_AVATAR_PICKERS, enabled)
    }

    fun updateInAppDiscover(enabled: Boolean) {
        enableInAppDiscover = enabled
        SharedKVStorage.putBoolean(KEY_DISCOVER_IN_APP, enabled)
    }

    fun updateKlipyGifs(enabled: Boolean) {
        enableKlipyGifs = enabled
        SharedKVStorage.putBoolean(KEY_GIF_PICKER_KLIPY, enabled)
    }

    fun updateThemeMode(mode: String) {
        themeMode = mode
        SharedKVStorage.putString(KEY_THEME_MODE, mode)
    }

    fun updateFontName(font: String) {
        fontName = font
        SharedKVStorage.putString(KEY_FONT_NAME, font)
    }

    fun updateAvatarRadius(radius: Int) {
        val clamped = radius.coerceIn(0, 50)
        avatarRadiusPercent = clamped
        SharedKVStorage.putString(KEY_AVATAR_RADIUS, clamped.toString())
    }

    fun updateReplyStyle(style: String) {
        messageReplyStyle = style
        SharedKVStorage.putString(KEY_REPLY_STYLE, style)
    }

    fun updateComposerBlur(enabled: Boolean) {
        composerBlurEnabled = enabled
        SharedKVStorage.putBoolean(KEY_COMPOSER_BLUR, enabled)
    }

    fun updateEmbedYouTube(enabled: Boolean) {
        embedYouTube = enabled
        SharedKVStorage.putBoolean(KEY_EMBED_YOUTUBE, enabled)
    }

    fun updateEmbedAppleMusic(enabled: Boolean) {
        embedAppleMusic = enabled
        SharedKVStorage.putBoolean(KEY_EMBED_APPLE_MUSIC, enabled)
    }

    fun updateCompactChatMode(enabled: Boolean) {
        compactChatMode = enabled
        SharedKVStorage.putBoolean(KEY_COMPACT_MODE, enabled)
    }

    fun updateShowMessageTimestamps(enabled: Boolean) {
        showMessageTimestamps = enabled
        SharedKVStorage.putBoolean(KEY_SHOW_TIMESTAMPS, enabled)
    }

    fun updateVoiceNoiseSuppression(enabled: Boolean) {
        voiceNoiseSuppression = enabled
        SharedKVStorage.putBoolean(KEY_VOICE_NOISE_SUPPRESSION, enabled)
    }

    fun updateVoiceEchoCancellation(enabled: Boolean) {
        voiceEchoCancellation = enabled
        SharedKVStorage.putBoolean(KEY_VOICE_ECHO_CANCELLATION, enabled)
    }

    fun updateVoiceAutoGain(enabled: Boolean) {
        voiceAutoGain = enabled
        SharedKVStorage.putBoolean(KEY_VOICE_AUTO_GAIN, enabled)
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
        SharedKVStorage.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
    }

    fun updateLanguageCode(code: String) {
        languageCode = code
        SharedKVStorage.putString(KEY_LANGUAGE_CODE, code)
    }

    // URL & Resource builders
    fun resolveAutumnUrl(resource: AutumnResource?, tagFallback: String = "avatars", size: Int? = null): String? {
        val id = resource?.id?.takeIf { it.isNotBlank() } ?: return null
        val tag = resource.tag?.takeIf { it.isNotBlank() } ?: tagFallback
        val suffix = if (size != null) "?max_side=$size" else ""
        return "$autumnBaseUrl/$tag/$id$suffix"
    }

    fun resolveEmojiUrl(emojiId: String): String {
        return "$autumnBaseUrl/emojis/$emojiId"
    }

    fun resolveUserAvatarUrl(user: User?, size: Int = 256): String? {
        if (user == null) return null
        return resolveAutumnUrl(user.avatar, "avatars", size)
            ?: user.id?.let { "$apiBaseUrl/users/$it/default_avatar" }
    }

    fun resolveServerIconUrl(server: Server?, size: Int = 256): String? {
        return resolveAutumnUrl(server?.icon, "icons", size)
    }

    fun resolveServerBannerUrl(server: Server?, size: Int = 1024): String? {
        return resolveAutumnUrl(server?.banner, "banners", size)
    }

    fun resolveChannelIconUrl(channel: Channel?, size: Int = 256): String? {
        if (channel == null) return null
        resolveAutumnUrl(channel.icon, "icons", size)?.let { return it }
        if (channel.channelType == ChannelType.DirectMessage) {
            val otherId = channel.recipients?.firstOrNull { it != selfId } ?: channel.recipients?.firstOrNull()
            return resolveUserAvatarUrl(otherId?.let { userCache[it] }, size)
        }
        return null
    }

    fun resolveChannelTitle(channel: Channel?): String {
        if (channel == null) return "Channel"
        return when (channel.channelType) {
            ChannelType.SavedMessages -> "Saved Notes"
            ChannelType.DirectMessage -> {
                val otherId = channel.recipients?.firstOrNull { it != selfId } ?: channel.recipients?.firstOrNull()
                val otherUser = otherId?.let { userCache[it] }
                otherUser?.let { User.resolveDefaultName(it) } ?: channel.name ?: "Direct Message"
            }
            ChannelType.Group -> channel.name?.takeIf { it.isNotBlank() } ?: "Group DM"
            else -> channel.name?.takeIf { it.isNotBlank() } ?: "channel"
        }
    }

    fun isChannelUnread(channelId: String): Boolean {
        val channel = channelCache[channelId] ?: return false
        val lastMsgId = channel.lastMessageID ?: return false
        val lastReadId = unreadLastReadByChannel[channelId] ?: return false
        return lastMsgId > lastReadId
    }

    fun channelMentionCount(channelId: String): Int {
        return unreadMentionsByChannel[channelId]?.size ?: 0
    }

    fun isServerUnread(serverId: String): Boolean {
        val server = serverCache[serverId] ?: return false
        return server.channels.orEmpty().any { isChannelUnread(it) }
    }

    fun serverMentionCount(serverId: String): Int {
        val server = serverCache[serverId] ?: return 0
        return server.channels.orEmpty().sumOf { channelMentionCount(it) }
    }

    fun sortedServers(): List<Server> {
        val all = serverCache.values.toList()
        if (orderedServerIds.isEmpty()) return all.sortedBy { it.name?.lowercase() ?: "" }
        val orderMap = orderedServerIds.withIndex().associate { it.value to it.index }
        return all.sortedWith(
            compareBy<Server> { orderMap[it.id] ?: Int.MAX_VALUE }
                .thenBy { it.name?.lowercase() ?: "" }
        )
    }

    fun dmAndGroupChannels(): List<Channel> {
        return channelCache.values
            .filter {
                (it.channelType == ChannelType.DirectMessage && it.active != false) ||
                    it.channelType == ChannelType.Group ||
                    it.channelType == ChannelType.SavedMessages
            }
            .sortedByDescending { it.lastMessageID ?: it.id ?: "" }
    }

    // Internal HTTP execution helper with automatic x-session-token and NetworkInspector logging
    private suspend fun executeRequest(
        method: String,
        pathOrUrl: String,
        tokenOverride: String? = null,
        mfaTicket: String? = null,
        idempotencyKey: String? = null,
        queryParams: Map<String, String> = emptyMap(),
        jsonBody: String? = null
    ): Pair<HttpStatusCode, String> {
        val url = if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            pathOrUrl
        } else {
            "$apiBaseUrl${if (pathOrUrl.startsWith("/")) pathOrUrl else "/$pathOrUrl"}"
        }
        val startMs = Clock.System.now().toEpochMilliseconds()
        val effectiveToken = tokenOverride ?: sessionToken
        val reqHeaders = mutableMapOf(
            "User-Agent" to "Ktor DawnChatFor${PlatformInfo.platformName}/${PlatformInfo.appVersion} (${PlatformInfo.osVersion})"
        )
        if (effectiveToken.isNotBlank()) {
            reqHeaders[TOKEN_HEADER_NAME] = effectiveToken
        }
        if (!mfaTicket.isNullOrBlank()) {
            reqHeaders[MFA_TICKET_HEADER_NAME] = mfaTicket
        }
        if (!idempotencyKey.isNullOrBlank()) {
            reqHeaders["Idempotency-Key"] = idempotencyKey
        }
        if (jsonBody != null) {
            reqHeaders["Content-Type"] = "application/json"
        }

        try {
            val response: HttpResponse = when (method.uppercase()) {
                "GET" -> SharedHttpClient.get(url) {
                    reqHeaders.forEach { (k, v) -> header(k, v) }
                    queryParams.forEach { (k, v) -> parameter(k, v) }
                }
                "POST" -> SharedHttpClient.post(url) {
                    reqHeaders.forEach { (k, v) -> header(k, v) }
                    queryParams.forEach { (k, v) -> parameter(k, v) }
                    if (jsonBody != null) {
                        contentType(ContentType.Application.Json)
                        setBody(jsonBody)
                    }
                }
                "PUT" -> SharedHttpClient.put(url) {
                    reqHeaders.forEach { (k, v) -> header(k, v) }
                    queryParams.forEach { (k, v) -> parameter(k, v) }
                    if (jsonBody != null) {
                        contentType(ContentType.Application.Json)
                        setBody(jsonBody)
                    }
                }
                "PATCH" -> SharedHttpClient.patch(url) {
                    reqHeaders.forEach { (k, v) -> header(k, v) }
                    queryParams.forEach { (k, v) -> parameter(k, v) }
                    if (jsonBody != null) {
                        contentType(ContentType.Application.Json)
                        setBody(jsonBody)
                    }
                }
                "DELETE" -> SharedHttpClient.delete(url) {
                    reqHeaders.forEach { (k, v) -> header(k, v) }
                    queryParams.forEach { (k, v) -> parameter(k, v) }
                    if (jsonBody != null) {
                        contentType(ContentType.Application.Json)
                        setBody(jsonBody)
                    }
                }
                else -> throw IllegalArgumentException("Unsupported method: $method")
            }
            val text = response.bodyAsText()
            val duration = Clock.System.now().toEpochMilliseconds() - startMs
            SharedNetworkInspector.recordHttp(
                method = method,
                url = url,
                statusCode = response.status.value,
                durationMs = duration,
                requestHeaders = reqHeaders,
                requestBody = jsonBody,
                responseBody = text
            )
            return response.status to text
        } catch (e: Exception) {
            val duration = Clock.System.now().toEpochMilliseconds() - startMs
            SharedNetworkInspector.recordHttp(
                method = method,
                url = url,
                statusCode = null,
                durationMs = duration,
                requestHeaders = reqHeaders,
                requestBody = jsonBody,
                error = e.message ?: e.toString()
            )
            throw e
        }
    }

    private fun parseApiError(body: String, status: HttpStatusCode): String {
        if (body.isNotBlank()) {
            runCatching {
                val obj = SharedJson.parseToJsonElement(body).jsonObject
                obj["type"]?.jsonPrimitive?.contentOrNull?.let { return it }
                obj["error"]?.jsonPrimitive?.contentOrNull?.let { return it }
                obj["message"]?.jsonPrimitive?.contentOrNull?.let { return it }
            }
        }
        return if (body.isNotBlank() && body.length < 160) body else "HTTP ${status.value}"
    }

    // =========================================================================
    // 1. Node Discovery, Health & Geo Checks
    // =========================================================================

    suspend fun fetchRootConfig(force: Boolean = false): SharedRootConfig {
        if (rootConfig != null && !force) return rootConfig!!
        return try {
            val (status, body) = executeRequest("GET", "/")
            if (!status.isSuccess()) throw Exception(parseApiError(body, status))
            val parsed = SharedJson.decodeFromString(SharedRootConfig.serializer(), body)
            rootConfig = parsed
            SharedStoatApi.autumnBaseUrl = parsed.features.autumn.url.ifBlank { STOAT_FILES }
            SharedStoatApi.januaryBaseUrl = parsed.features.january.url.ifBlank { STOAT_PROXY }
            parsed
        } catch (e: Exception) {
            val fallback = rootConfig ?: SharedRootConfig()
            rootConfig = fallback
            fallback
        }
    }

    suspend fun fetchHealthNotice(): HealthNotice? {
        return runCatching {
            val (status, body) = executeRequest("GET", "$STOAT_WEB_APP/health.json")
            if (status.isSuccess() && body.isNotBlank()) {
                SharedJson.decodeFromString(HealthNotice.serializer(), body).also {
                    healthNotice = it
                }
            } else null
        }.getOrNull()
    }

    // =========================================================================
    // 2. Authentication, MFA, Registration, Onboarding & Account Recovery
    // =========================================================================

    fun friendlySessionName(): String {
        return "DawnChat for ${PlatformInfo.platformName} (${PlatformInfo.osVersion})"
    }

    suspend fun login(email: String, password: String, captcha: String? = null): SharedLoginAssessment {
        return try {
            val payload = buildJsonObject {
                put("email", email.trim())
                put("password", password)
                put("friendly_name", friendlySessionName())
                if (!captcha.isNullOrBlank()) put("captcha", captcha)
            }
            val (status, body) = executeRequest(
                method = "POST",
                pathOrUrl = "/auth/session/login",
                jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
            )
            parseLoginAssessment(status, body)
        } catch (e: Exception) {
            SharedLoginAssessment(error = e.message ?: "Network error")
        }
    }

    suspend fun loginWithMfaTotp(mfaTicket: String, totpCode: String): SharedLoginAssessment {
        return try {
            val payload = buildJsonObject {
                put("mfa_ticket", mfaTicket)
                put("mfa_response", buildJsonObject {
                    put("totp_code", totpCode.trim())
                })
                put("friendly_name", friendlySessionName())
            }
            val (status, body) = executeRequest(
                method = "POST",
                pathOrUrl = "/auth/session/login",
                jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
            )
            parseLoginAssessment(status, body)
        } catch (e: Exception) {
            SharedLoginAssessment(error = e.message ?: "Network error")
        }
    }

    suspend fun loginWithMfaRecovery(mfaTicket: String, recoveryCode: String): SharedLoginAssessment {
        return try {
            val payload = buildJsonObject {
                put("mfa_ticket", mfaTicket)
                put("mfa_response", buildJsonObject {
                    put("recovery_code", recoveryCode.trim())
                })
                put("friendly_name", friendlySessionName())
            }
            val (status, body) = executeRequest(
                method = "POST",
                pathOrUrl = "/auth/session/login",
                jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
            )
            parseLoginAssessment(status, body)
        } catch (e: Exception) {
            SharedLoginAssessment(error = e.message ?: "Network error")
        }
    }

    suspend fun loginWithMfaPassword(mfaTicket: String, password: String): SharedLoginAssessment {
        return try {
            val payload = buildJsonObject {
                put("mfa_ticket", mfaTicket)
                put("mfa_response", buildJsonObject {
                    put("password", password)
                })
                put("friendly_name", friendlySessionName())
            }
            val (status, body) = executeRequest(
                method = "POST",
                pathOrUrl = "/auth/session/login",
                jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
            )
            parseLoginAssessment(status, body)
        } catch (e: Exception) {
            SharedLoginAssessment(error = e.message ?: "Network error")
        }
    }

    private fun parseLoginAssessment(status: HttpStatusCode, body: String): SharedLoginAssessment {
        if (!status.isSuccess()) {
            return SharedLoginAssessment(error = parseApiError(body, status))
        }
        val obj = runCatching { SharedJson.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return SharedLoginAssessment(error = "InvalidResponse")

        obj["type"]?.jsonPrimitive?.contentOrNull?.let { errType ->
            return SharedLoginAssessment(error = errType)
        }

        return when (val result = obj["result"]?.jsonPrimitive?.contentOrNull) {
            "Success" -> {
                val hints = runCatching {
                    SharedJson.decodeFromJsonElement(SharedUserSessionHints.serializer(), obj)
                }.getOrNull()
                if (hints != null && hints.token.isNotBlank()) {
                    saveSession(hints.token, hints.id, hints.userId)
                    SharedLoginAssessment(success = true, sessionHints = hints)
                } else {
                    SharedLoginAssessment(error = "FailedToParseUserSession")
                }
            }
            "MFA" -> {
                val spec = runCatching {
                    SharedJson.decodeFromJsonElement(SharedLoginMfaSpec.serializer(), obj)
                }.getOrNull()
                if (spec != null && spec.ticket.isNotBlank()) {
                    SharedLoginAssessment(proceedMfa = true, mfaSpec = spec)
                } else {
                    SharedLoginAssessment(error = "FailedToParseMfaSpec")
                }
            }
            "Disabled" -> SharedLoginAssessment(error = "DisabledAccount")
            else -> SharedLoginAssessment(error = result ?: "UnknownLoginResult")
        }
    }

    suspend fun registerAccount(email: String, password: String, invite: String? = null, captcha: String = "") {
        val payload = buildJsonObject {
            put("email", email.trim())
            put("password", password)
            if (!invite.isNullOrBlank()) put("invite", invite.trim())
            put("captcha", captcha)
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/auth/account/create",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) {
            throw Exception(parseApiError(body, status))
        }
        if (body.isNotBlank()) {
            runCatching {
                val obj = SharedJson.parseToJsonElement(body).jsonObject
                obj["type"]?.jsonPrimitive?.contentOrNull?.let { throw Exception(it) }
            }.onFailure { if (it !is kotlinx.serialization.SerializationException) throw it }
        }
    }

    suspend fun checkNeedsOnboarding(token: String = sessionToken): Boolean {
        val (status, body) = executeRequest(
            method = "GET",
            pathOrUrl = "/onboard/hello",
            tokenOverride = token
        )
        if (!status.isSuccess()) return false
        return runCatching {
            SharedJson.parseToJsonElement(body).jsonObject["onboarding"]?.jsonPrimitive?.booleanOrNull ?: false
        }.getOrDefault(false)
    }

    suspend fun completeOnboarding(username: String, token: String = sessionToken) {
        val payload = buildJsonObject {
            put("username", username.trim())
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/onboard/complete",
            tokenOverride = token,
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (status == HttpStatusCode.Conflict) throw Exception("UsernameTaken")
        if (status == HttpStatusCode.BadRequest) throw Exception("InvalidUsername")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun requestPasswordReset(email: String, captcha: String = "") {
        val payload = buildJsonObject {
            put("email", email.trim())
            put("captcha", captcha)
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/auth/account/reset_password",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun resendVerificationEmail(email: String, captcha: String = "") {
        val payload = buildJsonObject {
            put("email", email.trim())
            put("captcha", captcha)
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/auth/account/reverify",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun confirmPasswordReset(token: String, newPassword: String, removeSessions: Boolean) {
        val payload = buildJsonObject {
            put("token", token.trim())
            put("password", newPassword)
            put("remove_sessions", removeSessions)
        }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/auth/account/reset_password",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun verifyEmailToken(token: String) {
        val (status, body) = executeRequest("POST", "/auth/account/verify/${token.trim()}")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun confirmAccountDeletion(token: String) {
        val payload = buildJsonObject {
            put("token", token.trim())
        }
        val (status, body) = executeRequest(
            method = "PUT",
            pathOrUrl = "/auth/account/delete",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun requestAccountDeletion() {
        val (status, body) = executeRequest("POST", "/auth/account/delete")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun fetchAccountEmail(): String {
        val (status, body) = executeRequest("GET", "/auth/account/")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.parseToJsonElement(body).jsonObject["email"]?.jsonPrimitive?.contentOrNull ?: ""
    }

    suspend fun changeEmail(newEmail: String, currentPassword: String) {
        val payload = buildJsonObject {
            put("email", newEmail.trim())
            put("current_password", currentPassword)
        }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/auth/account/change/email",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun changePassword(newPassword: String, currentPassword: String) {
        val payload = buildJsonObject {
            put("password", newPassword)
            put("current_password", currentPassword)
        }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/auth/account/change/password",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun changeUsername(newUsername: String, password: String): User {
        val payload = buildJsonObject {
            put("username", newUsername.trim())
            put("password", password)
        }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/users/@me/username",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val updated = SharedJson.decodeFromString(User.serializer(), body)
        updated.id?.let { userCache[it] = updated }
        return updated
    }

    // =========================================================================
    // 3. MFA & Sessions Management
    // =========================================================================

    suspend fun fetchMfaMethods(): List<String> {
        val (status, body) = executeRequest("GET", "/auth/mfa/methods")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val methods = SharedJson.decodeFromString(ListSerializer(String.serializer()), body)
        mfaEnabledMethods.clear()
        mfaEnabledMethods.addAll(methods)
        return methods
    }

    suspend fun createMfaTicketWithPassword(password: String): SharedMfaTicket {
        val payload = buildJsonObject { put("password", password) }
        val (status, body) = executeRequest(
            method = "PUT",
            pathOrUrl = "/auth/mfa/ticket",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(SharedMfaTicket.serializer(), body)
    }

    suspend fun createMfaTicketWithTotp(totpCode: String): SharedMfaTicket {
        val payload = buildJsonObject { put("totp_code", totpCode.trim()) }
        val (status, body) = executeRequest(
            method = "PUT",
            pathOrUrl = "/auth/mfa/ticket",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(SharedMfaTicket.serializer(), body)
    }

    suspend fun generateTotpSecret(mfaTicketToken: String): String {
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/auth/mfa/totp",
            mfaTicket = mfaTicketToken
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.parseToJsonElement(body).jsonObject["secret"]?.jsonPrimitive?.contentOrNull ?: ""
    }

    suspend fun enableTotp(totpCode: String) {
        val payload = buildJsonObject { put("totp_code", totpCode.trim()) }
        val (status, body) = executeRequest(
            method = "PUT",
            pathOrUrl = "/auth/mfa/totp",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun disableTotp(mfaTicketToken: String) {
        val (status, body) = executeRequest(
            method = "DELETE",
            pathOrUrl = "/auth/mfa/totp",
            mfaTicket = mfaTicketToken
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun fetchRecoveryCodes(mfaTicketToken: String): List<String> {
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/auth/mfa/recovery",
            mfaTicket = mfaTicketToken
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(ListSerializer(String.serializer()), body)
    }

    suspend fun regenerateRecoveryCodes(mfaTicketToken: String): List<String> {
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/auth/mfa/recovery",
            mfaTicket = mfaTicketToken
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(ListSerializer(String.serializer()), body)
    }

    suspend fun fetchAllSessions(): List<Session> {
        val (status, body) = executeRequest("GET", "/auth/session/all")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val list = SharedJson.decodeFromString(ListSerializer(Session.serializer()), body)
        sessionsList.clear()
        sessionsList.addAll(list)
        return list
    }

    suspend fun revokeSession(id: String) {
        val (status, body) = executeRequest("DELETE", "/auth/session/$id")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        sessionsList.removeAll { it.id == id }
    }

    suspend fun revokeAllSessions(revokeSelf: Boolean = false) {
        val (status, body) = executeRequest(
            method = "DELETE",
            pathOrUrl = "/auth/session/all",
            queryParams = mapOf("revoke_self" to revokeSelf.toString())
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        if (revokeSelf) {
            clearSession()
        } else {
            sessionsList.removeAll { it.id != sessionId }
        }
    }

    // =========================================================================
    // 4. Users, Self Profile, Friends, Relationships & Bots
    // =========================================================================

    suspend fun fetchSelf(): User {
        val (status, body) = executeRequest("GET", "/users/@me")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val user = SharedJson.decodeFromString(User.serializer(), body)
        val uid = user.id ?: throw Exception("MissingSelfId")
        selfId = uid
        SharedKVStorage.putString(KEY_SELF_ID, uid)
        userCache[uid] = user
        return user
    }

    suspend fun fetchUser(userId: String): User {
        val (status, body) = executeRequest("GET", "/users/$userId")
        if (status == HttpStatusCode.NotFound) {
            return User.getPlaceholder(userId).also { userCache[userId] = it }
        }
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(User.serializer(), body).also { u ->
            u.id?.let { userCache[it] = u }
        }
    }

    suspend fun fetchUserProfile(userId: String): Profile {
        val (status, body) = executeRequest("GET", "/users/$userId/profile")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(Profile.serializer(), body).also {
            profileByUser[userId] = it
        }
    }

    suspend fun fetchMutuals(userId: String): SharedMutuals {
        val (status, body) = executeRequest("GET", "/users/$userId/mutual")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(SharedMutuals.serializer(), body).also {
            mutualsByUser[userId] = it
        }
    }

    suspend fun patchSelf(
        status: Status? = null,
        displayName: String? = null,
        pronouns: String? = null,
        avatar: String? = null,
        background: String? = null,
        bio: String? = null,
        remove: List<String> = emptyList()
    ): User {
        val bodyMap = mutableMapOf<String, JsonElement>()
        status?.let { bodyMap["status"] = SharedJson.encodeToJsonElement(Status.serializer(), it) }
        displayName?.let { bodyMap["display_name"] = JsonPrimitive(it) }
        pronouns?.let { bodyMap["pronouns"] = JsonPrimitive(it) }
        avatar?.let { bodyMap["avatar"] = JsonPrimitive(it) }
        if (background != null || bio != null) {
            val profileObj = buildJsonObject {
                background?.let { put("background", it) }
                bio?.let { put("content", it) }
            }
            bodyMap["profile"] = profileObj
        }
        if (remove.isNotEmpty()) {
            bodyMap["remove"] = SharedJson.encodeToJsonElement(ListSerializer(String.serializer()), remove)
        }
        val (httpStatus, respBody) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/users/@me",
            jsonBody = SharedJson.encodeToString(
                MapSerializer(String.serializer(), JsonElement.serializer()),
                bodyMap
            )
        )
        if (!httpStatus.isSuccess()) throw Exception(parseApiError(respBody, httpStatus))
        val partial = SharedJson.decodeFromString(User.serializer(), respBody)
        val current = currentUser ?: partial
        var merged = current.mergeWithPartial(partial)
        if ("DisplayName" in remove) merged = merged.copy(displayName = null)
        if ("Pronouns" in remove) merged = merged.copy(pronouns = null)
        merged.id?.let { userCache[it] = merged }
        return merged
    }

    suspend fun sendFriendRequest(usernameOrTag: String) {
        val payload = buildJsonObject { put("username", usernameOrTag.trim()) }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/users/friend",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        runCatching {
            val u = SharedJson.decodeFromString(User.serializer(), body)
            u.id?.let { userCache[it] = u }
        }
    }

    suspend fun acceptFriendRequest(userId: String) {
        val (status, body) = executeRequest("PUT", "/users/$userId/friend")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        runCatching {
            val u = SharedJson.decodeFromString(User.serializer(), body)
            u.id?.let { userCache[it] = u }
        }
    }

    suspend fun removeOrDeclineFriend(userId: String) {
        val (status, body) = executeRequest("DELETE", "/users/$userId/friend")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        runCatching {
            val u = SharedJson.decodeFromString(User.serializer(), body)
            u.id?.let { userCache[it] = u }
        }
    }

    suspend fun blockUser(userId: String) {
        val (status, body) = executeRequest("PUT", "/users/$userId/block")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        runCatching {
            val u = SharedJson.decodeFromString(User.serializer(), body)
            u.id?.let { userCache[it] = u }
        }
    }

    suspend fun unblockUser(userId: String) {
        val (status, body) = executeRequest("DELETE", "/users/$userId/block")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        runCatching {
            val u = SharedJson.decodeFromString(User.serializer(), body)
            u.id?.let { userCache[it] = u }
        }
    }

    suspend fun openDirectMessage(userId: String): Channel {
        val (status, body) = executeRequest("GET", "/users/$userId/dm")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(Channel.serializer(), body).also { ch ->
            ch.id?.let { channelCache[it] = ch }
        }
    }

    suspend fun createGroupDm(name: String, recipientIds: List<String>): Channel {
        val payload = buildJsonObject {
            put("name", name.trim())
            put("users", SharedJson.encodeToJsonElement(ListSerializer(String.serializer()), recipientIds))
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/channels/create",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(Channel.serializer(), body).also { ch ->
            ch.id?.let { channelCache[it] = ch }
        }
    }

    suspend fun fetchOwnedBots(): SharedOwnedBotsResponse {
        val (status, body) = executeRequest("GET", "/bots/@me")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val parsed = SharedJson.decodeFromString(SharedOwnedBotsResponse.serializer(), body)
        ownedBots.clear()
        ownedBots.addAll(parsed.bots)
        ownedBotUsers.clear()
        parsed.users.forEach { ownedBotUsers[it.id] = it }
        return parsed
    }

    suspend fun createBot(name: String): SharedOwnedBot {
        val payload = buildJsonObject { put("name", name.trim()) }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/bots/create",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val created = SharedJson.decodeFromString(SharedOwnedBot.serializer(), body)
        fetchOwnedBots()
        return created
    }

    suspend fun setBotPublic(botId: String, isPublic: Boolean): SharedOwnedBot {
        val payload = buildJsonObject { put("public", isPublic) }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/bots/$botId",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val updated = SharedJson.decodeFromString(SharedOwnedBot.serializer(), body)
        val idx = ownedBots.indexOfFirst { it.id == botId }
        if (idx >= 0) ownedBots[idx] = updated
        return updated
    }

    suspend fun resetBotToken(botId: String): SharedOwnedBot {
        val payload = buildJsonObject {
            put("remove", SharedJson.encodeToJsonElement(ListSerializer(String.serializer()), listOf("Token")))
        }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/bots/$botId",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val updated = SharedJson.decodeFromString(SharedOwnedBot.serializer(), body)
        val idx = ownedBots.indexOfFirst { it.id == botId }
        if (idx >= 0) ownedBots[idx] = updated
        return updated
    }

    suspend fun renameBot(botId: String, newUsername: String) {
        val payload = buildJsonObject { put("username", newUsername.trim()) }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/users/$botId",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        ownedBotUsers[botId]?.let {
            ownedBotUsers[botId] = it.copy(username = newUsername.trim())
        }
    }

    suspend fun deleteBot(botId: String) {
        val (status, body) = executeRequest("DELETE", "/bots/$botId")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        ownedBots.removeAll { it.id == botId }
        ownedBotUsers.remove(botId)
    }

    // =========================================================================
    // 5. Channels, Messages, Pins, Search, Reactions & Webhooks
    // =========================================================================

    suspend fun fetchChannelMessages(
        channelId: String,
        limit: Int = 50,
        before: String? = null,
        after: String? = null,
        nearby: String? = null
    ): List<Message> {
        val params = mutableMapOf(
            "limit" to limit.toString(),
            "include_users" to "true"
        )
        if (before != null) params["before"] = before
        if (after != null) params["after"] = after
        if (nearby != null) params["nearby"] = nearby

        val (status, body) = executeRequest(
            method = "GET",
            pathOrUrl = "/channels/$channelId/messages",
            queryParams = params
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))

        val parsed = SharedJson.decodeFromString(MessagesInChannel.serializer(), body)
        parsed.users?.forEach { u -> u.id?.let { userCache[it] = u } }
        parsed.members?.forEach { m ->
            val sid = m.id?.server ?: return@forEach
            val uid = m.id?.user ?: return@forEach
            val currentMap = memberCache[sid].orEmpty()
            memberCache[sid] = currentMap + (uid to m)
        }
        val chronMessages = parsed.messages.orEmpty().sortedBy { it.id ?: "" }
        if (before != null) {
            val existing = channelMessages[channelId].orEmpty()
            val merged = (chronMessages + existing).distinctBy { it.id }
            channelMessages[channelId] = merged
            return merged
        } else {
            channelMessages[channelId] = chronMessages
            chronMessages.lastOrNull()?.id?.let { lastId ->
                unreadLastReadByChannel[channelId] = lastId
            }
            return chronMessages
        }
    }

    suspend fun sendMessage(
        channelId: String,
        content: String,
        replies: List<SharedSendMessageReply> = emptyList(),
        attachments: List<String>? = null
    ): Message {
        val nonce = SharedUlid.next()
        val payload = buildJsonObject {
            put("content", content)
            put("nonce", nonce)
            if (replies.isNotEmpty()) {
                put("replies", SharedJson.encodeToJsonElement(ListSerializer(SharedSendMessageReply.serializer()), replies))
            }
            if (!attachments.isNullOrEmpty()) {
                put("attachments", SharedJson.encodeToJsonElement(ListSerializer(String.serializer()), attachments))
            }
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/channels/$channelId/messages",
            idempotencyKey = SharedUlid.next(),
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val msg = SharedJson.decodeFromString(Message.serializer(), body)
        appendMessageToChannel(channelId, msg)
        return msg
    }

    suspend fun uploadAttachmentToAutumn(
        tag: String = "attachments",
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String {
        val url = "${autumnBaseUrl.trimEnd('/')}/$tag"
        val response: HttpResponse = SharedHttpClient.post(url) {
            sessionToken?.takeIf { it.isNotBlank() }?.let { header("x-session-token", it) }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            }
                        )
                    }
                )
            )
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw Exception(parseApiError(body, response.status))
        }
        return SharedJson.parseToJsonElement(body).jsonObject["id"]?.jsonPrimitive?.contentOrNull
            ?: throw Exception("Missing attachment id from Autumn")
    }

    fun generateWavVoiceBytes(durationMillis: Long, waveform: List<Int>): ByteArray {
        val sampleRate = 8000
        val clampedDurationMs = durationMillis.coerceIn(1000L, 60_000L)
        val numSamples = ((clampedDurationMs * sampleRate) / 1000L).toInt().coerceAtLeast(sampleRate)
        val dataSize = numSamples * 2
        val totalSize = 44 + dataSize
        val bytes = ByteArray(totalSize)

        fun writeAscii(offset: Int, str: String) {
            for (i in str.indices) bytes[offset + i] = str[i].code.toByte()
        }
        fun writeIntLE(offset: Int, value: Int) {
            bytes[offset] = (value and 0xFF).toByte()
            bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
            bytes[offset + 2] = ((value ushr 16) and 0xFF).toByte()
            bytes[offset + 3] = ((value ushr 24) and 0xFF).toByte()
        }
        fun writeShortLE(offset: Int, value: Int) {
            bytes[offset] = (value and 0xFF).toByte()
            bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        }

        writeAscii(0, "RIFF")
        writeIntLE(4, totalSize - 8)
        writeAscii(8, "WAVE")
        writeAscii(12, "fmt ")
        writeIntLE(16, 16)
        writeShortLE(20, 1) // PCM
        writeShortLE(22, 1) // Mono
        writeIntLE(24, sampleRate)
        writeIntLE(28, sampleRate * 2)
        writeShortLE(32, 2)
        writeShortLE(34, 16)
        writeAscii(36, "data")
        writeIntLE(40, dataSize)

        val safeWave = if (waveform.isEmpty()) List(32) { 128 } else waveform
        var offset = 44
        for (i in 0 until numSamples) {
            val waveIdx = ((i.toLong() * safeWave.size) / numSamples).toInt().coerceIn(0, safeWave.lastIndex)
            val amp = (safeWave[waveIdx].coerceIn(16, 255) / 255.0) * 9000.0
            val t = i.toDouble() / sampleRate.toDouble()
            val sample = (sin(2.0 * PI * 320.0 * t) * amp).toInt().coerceIn(-32767, 32767)
            writeShortLE(offset, sample)
            offset += 2
        }
        return bytes
    }

    suspend fun sendVoiceMessage(
        channelId: String,
        durationMillis: Long,
        waveform: List<Int>,
        replies: List<SharedSendMessageReply> = emptyList()
    ): Message {
        val clampedDuration = durationMillis.coerceAtLeast(1000L)
        val normalizedWave = (if (waveform.isEmpty()) List(64) { 120 } else waveform)
            .take(64)
            .map { it.coerceIn(0, 255) }
        val voiceMetadataJson = buildJsonObject {
            put("x", "chat.stoat.VoiceMessage")
            put("y", 1)
            put("z", clampedDuration)
            put("w", JsonArray(normalizedWave.map { JsonPrimitive(it) }))
        }.toString()

        val wavBytes = generateWavVoiceBytes(clampedDuration, normalizedWave)
        val uploadedAttachmentId = runCatching {
            uploadAttachmentToAutumn(
                tag = "attachments",
                fileName = "voice-message.wav",
                mimeType = "audio/wav",
                bytes = wavBytes
            )
        }.getOrNull()

        return sendMessage(
            channelId = channelId,
            content = voiceMetadataJson,
            replies = replies,
            attachments = uploadedAttachmentId?.let { listOf(it) }
        )
    }

    fun appendMessageToChannel(channelId: String, message: Message) {
        val current = channelMessages[channelId].orEmpty()
        if (current.none { it.id == message.id }) {
            channelMessages[channelId] = current + message
        }
        channelCache[channelId]?.let { ch ->
            channelCache[channelId] = ch.copy(lastMessageID = message.id ?: ch.lastMessageID)
        }
    }

    suspend fun editMessage(channelId: String, messageId: String, newContent: String) {
        val payload = buildJsonObject { put("content", newContent) }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/channels/$channelId/messages/$messageId",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        channelMessages[channelId]?.let { list ->
            channelMessages[channelId] = list.map {
                if (it.id == messageId) it.copy(content = newContent, edited = "edited") else it
            }
        }
    }

    suspend fun deleteMessage(channelId: String, messageId: String) {
        val (status, body) = executeRequest("DELETE", "/channels/$channelId/messages/$messageId")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        channelMessages[channelId] = channelMessages[channelId].orEmpty().filterNot { it.id == messageId }
    }

    suspend fun pinMessage(channelId: String, messageId: String) {
        val (status, body) = executeRequest("POST", "/channels/$channelId/messages/$messageId/pin")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        channelMessages[channelId] = channelMessages[channelId].orEmpty().map {
            if (it.id == messageId) it.copy(pinned = true) else it
        }
    }

    suspend fun unpinMessage(channelId: String, messageId: String) {
        val (status, body) = executeRequest("DELETE", "/channels/$channelId/messages/$messageId/pin")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        channelMessages[channelId] = channelMessages[channelId].orEmpty().map {
            if (it.id == messageId) it.copy(pinned = false) else it
        }
        channelPinnedMessages[channelId] = channelPinnedMessages[channelId].orEmpty().filterNot { it.id == messageId }
    }

    suspend fun reactToMessage(channelId: String, messageId: String, emoji: String) {
        val (status, body) = executeRequest("PUT", "/channels/$channelId/messages/$messageId/reactions/$emoji")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val uid = selfId ?: return
        channelMessages[channelId] = channelMessages[channelId].orEmpty().map { msg ->
            if (msg.id == messageId) {
                val map = msg.reactions.orEmpty().toMutableMap()
                val users = (map[emoji].orEmpty() + uid).distinct()
                map[emoji] = users
                msg.copy(reactions = map)
            } else msg
        }
    }

    suspend fun unreactToMessage(channelId: String, messageId: String, emoji: String) {
        val (status, body) = executeRequest("DELETE", "/channels/$channelId/messages/$messageId/reactions/$emoji")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val uid = selfId ?: return
        channelMessages[channelId] = channelMessages[channelId].orEmpty().map { msg ->
            if (msg.id == messageId) {
                val map = msg.reactions.orEmpty().toMutableMap()
                val users = map[emoji].orEmpty() - uid
                if (users.isEmpty()) map.remove(emoji) else map[emoji] = users
                msg.copy(reactions = map)
            } else msg
        }
    }

    suspend fun ackChannel(channelId: String, messageId: String = SharedUlid.next()) {
        unreadLastReadByChannel[channelId] = messageId
        unreadMentionsByChannel.remove(channelId)
        runCatching {
            executeRequest("PUT", "/channels/$channelId/ack/$messageId")
        }
    }

    suspend fun fetchPinnedMessages(channelId: String): List<Message> {
        val payload = buildJsonObject {
            put("pinned", true)
            put("include_users", true)
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/channels/$channelId/search",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val parsed = SharedJson.decodeFromString(MessagesInChannel.serializer(), body)
        parsed.users?.forEach { u -> u.id?.let { userCache[it] = u } }
        val msgs = parsed.messages.orEmpty()
        channelPinnedMessages[channelId] = msgs
        return msgs
    }

    suspend fun searchChannelMessages(
        channelId: String,
        query: String,
        sort: String = "Latest"
    ): List<Message> {
        val payload = buildJsonObject {
            put("query", query.trim().take(64))
            put("include_users", true)
            put("sort", sort)
            put("limit", 50)
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/channels/$channelId/search",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val parsed = SharedJson.decodeFromString(MessagesInChannel.serializer(), body)
        parsed.users?.forEach { u -> u.id?.let { userCache[it] = u } }
        val msgs = parsed.messages.orEmpty()
        channelSearchResults[channelId] = msgs
        return msgs
    }

    suspend fun patchChannel(
        channelId: String,
        name: String? = null,
        description: String? = null,
        nsfw: Boolean? = null,
        remove: List<String> = emptyList()
    ): Channel {
        val payload = buildJsonObject {
            name?.let { put("name", it) }
            description?.let { put("description", it) }
            nsfw?.let { put("nsfw", it) }
            if (remove.isNotEmpty()) {
                put("remove", SharedJson.encodeToJsonElement(ListSerializer(String.serializer()), remove))
            }
        }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/channels/$channelId",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val updated = SharedJson.decodeFromString(Channel.serializer(), body)
        channelCache[channelId] = updated
        return updated
    }

    suspend fun deleteOrLeaveChannel(channelId: String) {
        val (status, body) = executeRequest("DELETE", "/channels/$channelId")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        channelCache.remove(channelId)
    }

    suspend fun setChannelRolePermissions(
        channelId: String,
        roleId: String,
        allow: Long,
        deny: Long
    ): Channel {
        val payload = buildJsonObject {
            put("permissions", buildJsonObject {
                put("allow", allow)
                put("deny", deny)
            })
        }
        val path = if (roleId == "default") {
            "/channels/$channelId/permissions/default"
        } else {
            "/channels/$channelId/permissions/$roleId"
        }
        val (status, body) = executeRequest(
            method = "PUT",
            pathOrUrl = path,
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(Channel.serializer(), body).also {
            channelCache[channelId] = it
        }
    }

    suspend fun fetchChannelWebhooks(channelId: String): List<SharedWebhook> {
        val (status, body) = executeRequest("GET", "/channels/$channelId/webhooks")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val list = SharedJson.decodeFromString(ListSerializer(SharedWebhook.serializer()), body)
        webhooksByChannel[channelId] = list
        return list
    }

    suspend fun createChannelWebhook(channelId: String, name: String): SharedWebhook {
        val payload = buildJsonObject { put("name", name.trim()) }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/channels/$channelId/webhooks",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val created = SharedJson.decodeFromString(SharedWebhook.serializer(), body)
        webhooksByChannel[channelId] = webhooksByChannel[channelId].orEmpty() + created
        return created
    }

    suspend fun editWebhook(channelId: String, webhookId: String, name: String): SharedWebhook {
        val payload = buildJsonObject { put("name", name.trim()) }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/webhooks/$webhookId",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val updated = SharedJson.decodeFromString(SharedWebhook.serializer(), body)
        webhooksByChannel[channelId] = webhooksByChannel[channelId].orEmpty().map {
            if (it.id == webhookId) updated else it
        }
        return updated
    }

    suspend fun deleteWebhook(channelId: String, webhookId: String) {
        val (status, body) = executeRequest("DELETE", "/webhooks/$webhookId")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        webhooksByChannel[channelId] = webhooksByChannel[channelId].orEmpty().filterNot { it.id == webhookId }
    }

    // =========================================================================
    // 6. Servers, Members, Roles, Invites, Bans & Emojis
    // =========================================================================

    suspend fun createServer(name: String, description: String = "", nsfw: Boolean = false): ServerWithChannelObjects {
        val payload = buildJsonObject {
            put("name", name.trim())
            if (description.isNotBlank()) put("description", description.trim())
            put("nsfw", nsfw)
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/servers/create",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val created = SharedJson.decodeFromString(ServerWithChannelObjects.serializer(), body)
        val channelIds = created.channels.orEmpty().mapNotNull { ch ->
            ch.id?.also { channelCache[it] = ch }
        }
        created.id?.let { sid ->
            serverCache[sid] = Server(
                id = sid,
                owner = created.owner,
                name = created.name,
                description = created.description,
                channels = channelIds,
                categories = created.categories,
                systemMessages = created.systemMessages,
                roles = created.roles,
                defaultPermissions = created.defaultPermissions,
                icon = created.icon,
                banner = created.banner,
                flags = created.flags,
                analytics = created.analytics,
                discoverable = created.discoverable
            )
        }
        return created
    }

    suspend fun patchServer(
        serverId: String,
        name: String? = null,
        description: String? = null,
        categories: List<Category>? = null,
        systemMessages: Map<String, String>? = null,
        remove: List<String> = emptyList()
    ): Server {
        val bodyMap = mutableMapOf<String, JsonElement>()
        name?.let { bodyMap["name"] = JsonPrimitive(it) }
        description?.let { bodyMap["description"] = JsonPrimitive(it) }
        categories?.let {
            bodyMap["categories"] = SharedJson.encodeToJsonElement(ListSerializer(Category.serializer()), it)
        }
        systemMessages?.let { map ->
            bodyMap["system_messages"] = JsonObject(map.mapValues { JsonPrimitive(it.value) })
        }
        if (remove.isNotEmpty()) {
            bodyMap["remove"] = SharedJson.encodeToJsonElement(ListSerializer(String.serializer()), remove)
        }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/servers/$serverId",
            jsonBody = SharedJson.encodeToString(
                MapSerializer(String.serializer(), JsonElement.serializer()),
                bodyMap
            )
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(Server.serializer(), body).also {
            serverCache[serverId] = it
        }
    }

    suspend fun ackServer(serverId: String) {
        serverCache[serverId]?.channels.orEmpty().forEach { chId ->
            channelCache[chId]?.lastMessageID?.let { unreadLastReadByChannel[chId] = it }
            unreadMentionsByChannel.remove(chId)
        }
        runCatching { executeRequest("PUT", "/servers/$serverId/ack") }
    }

    suspend fun leaveOrDeleteServer(serverId: String) {
        val (status, body) = executeRequest("DELETE", "/servers/$serverId")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val server = serverCache.remove(serverId)
        server?.channels.orEmpty().forEach { channelCache.remove(it) }
    }

    suspend fun createServerChannel(serverId: String, name: String, isVoice: Boolean): Channel {
        val payload = buildJsonObject {
            put("name", name.trim())
            put("type", if (isVoice) "Voice" else "Text")
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/servers/$serverId/channels",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(Channel.serializer(), body).also { ch ->
            ch.id?.let { cid ->
                channelCache[cid] = ch
                serverCache[serverId]?.let { srv ->
                    if (cid !in srv.channels.orEmpty()) {
                        serverCache[serverId] = srv.copy(channels = srv.channels.orEmpty() + cid)
                    }
                }
            }
        }
    }

    suspend fun fetchServerMembers(serverId: String): SharedFetchMembersResponse {
        val (status, body) = executeRequest(
            method = "GET",
            pathOrUrl = "/servers/$serverId/members",
            queryParams = mapOf("exclude_offline" to "false")
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val parsed = SharedJson.decodeFromString(SharedFetchMembersResponse.serializer(), body)
        parsed.users.forEach { u -> u.id?.let { userCache[it] = u } }
        val map = mutableMapOf<String, Member>()
        parsed.members.forEach { m ->
            val uid = m.id?.user ?: return@forEach
            map[uid] = m
        }
        memberCache[serverId] = map
        return parsed
    }

    suspend fun patchServerMember(
        serverId: String,
        userId: String,
        nickname: String? = null,
        pronouns: String? = null,
        roles: List<String>? = null,
        timeoutIso: String? = null,
        remove: List<String> = emptyList()
    ): Member {
        val payload = buildJsonObject {
            nickname?.let { put("nickname", it) }
            pronouns?.let { put("pronouns", it) }
            roles?.let { put("roles", SharedJson.encodeToJsonElement(ListSerializer(String.serializer()), it)) }
            timeoutIso?.let { put("timeout", it) }
            if (remove.isNotEmpty()) {
                put("remove", SharedJson.encodeToJsonElement(ListSerializer(String.serializer()), remove))
            }
        }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/servers/$serverId/members/$userId",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(Member.serializer(), body).also { member ->
            val currentMap = memberCache[serverId].orEmpty()
            memberCache[serverId] = currentMap + (userId to member)
        }
    }

    suspend fun kickServerMember(serverId: String, userId: String) {
        val (status, body) = executeRequest("DELETE", "/servers/$serverId/members/$userId")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        memberCache[serverId] = memberCache[serverId].orEmpty() - userId
    }

    suspend fun banServerMember(serverId: String, userId: String, reason: String? = null) {
        val payload = buildJsonObject {
            if (!reason.isNullOrBlank()) put("reason", reason.trim())
        }
        val (status, body) = executeRequest(
            method = "PUT",
            pathOrUrl = "/servers/$serverId/bans/$userId",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        memberCache[serverId] = memberCache[serverId].orEmpty() - userId
    }

    suspend fun unbanServerMember(serverId: String, userId: String) {
        val (status, body) = executeRequest("DELETE", "/servers/$serverId/bans/$userId")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        serverBansByServer[serverId]?.let { current ->
            serverBansByServer[serverId] = current.copy(
                bans = current.bans.filterNot { it.id.user == userId },
                users = current.users.filterNot { it.id == userId }
            )
        }
    }

    suspend fun fetchServerBans(serverId: String): BanListResult {
        val (status, body) = executeRequest("GET", "/servers/$serverId/bans")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(BanListResult.serializer(), body).also {
            serverBansByServer[serverId] = it
        }
    }

    suspend fun createServerRole(serverId: String, name: String): Pair<String, Role> {
        val payload = buildJsonObject { put("name", name.trim()) }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/servers/$serverId/roles",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val obj = SharedJson.parseToJsonElement(body).jsonObject
        val roleId = obj["id"]?.jsonPrimitive?.contentOrNull ?: throw Exception("MissingRoleId")
        val role = SharedJson.decodeFromJsonElement(Role.serializer(), obj["role"]!!)
        serverCache[serverId]?.let { srv ->
            serverCache[serverId] = srv.copy(roles = srv.roles.orEmpty() + (roleId to role))
        }
        return roleId to role
    }

    suspend fun editServerRole(
        serverId: String,
        roleId: String,
        name: String? = null,
        colour: String? = null,
        hoist: Boolean? = null,
        remove: List<String> = emptyList()
    ): Role {
        val payload = buildJsonObject {
            name?.let { put("name", it.trim()) }
            colour?.let { put("colour", it.trim()) }
            hoist?.let { put("hoist", it) }
            if (remove.isNotEmpty()) {
                put("remove", SharedJson.encodeToJsonElement(ListSerializer(String.serializer()), remove))
            }
        }
        val (status, body) = executeRequest(
            method = "PATCH",
            pathOrUrl = "/servers/$serverId/roles/$roleId",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(Role.serializer(), body).also { role ->
            serverCache[serverId]?.let { srv ->
                serverCache[serverId] = srv.copy(roles = srv.roles.orEmpty() + (roleId to role))
            }
        }
    }

    suspend fun setServerRolePermissions(
        serverId: String,
        roleId: String,
        allow: Long,
        deny: Long
    ): Server {
        val payload = buildJsonObject {
            put("permissions", buildJsonObject {
                put("allow", allow)
                put("deny", deny)
            })
        }
        val (status, body) = executeRequest(
            method = "PUT",
            pathOrUrl = "/servers/$serverId/permissions/$roleId",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(Server.serializer(), body).also {
            serverCache[serverId] = it
        }
    }

    suspend fun deleteServerRole(serverId: String, roleId: String) {
        val (status, body) = executeRequest("DELETE", "/servers/$serverId/roles/$roleId")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        serverCache[serverId]?.let { srv ->
            serverCache[serverId] = srv.copy(roles = srv.roles.orEmpty() - roleId)
        }
    }

    suspend fun fetchServerInvites(serverId: String): List<ServerInvite> {
        val (status, body) = executeRequest("GET", "/servers/$serverId/invites")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(ListSerializer(ServerInvite.serializer()), body).also {
            serverInvitesByServer[serverId] = it
        }
    }

    suspend fun createChannelInvite(channelId: String): ServerInvite {
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/channels/$channelId/invites",
            jsonBody = "{}"
        )
        if (status.isSuccess() && body.isNotBlank()) {
            runCatching {
                return SharedJson.decodeFromString(ServerInvite.serializer(), body)
            }
            runCatching {
                val obj = SharedJson.parseToJsonElement(body).jsonObject
                val code = obj["_id"]?.jsonPrimitive?.contentOrNull
                    ?: obj["code"]?.jsonPrimitive?.contentOrNull
                    ?: ""
                if (code.isNotBlank()) {
                    return ServerInvite(id = code, code = code, channel = channelId)
                }
            }
        }
        val srvId = channelCache[channelId]?.server
        if (srvId != null) {
            val existing = runCatching { fetchServerInvites(srvId) }.getOrNull()
            existing?.firstOrNull { it.channel == channelId || it.effectiveCode.isNotBlank() }?.let { return it }
        }
        throw Exception(parseApiError(body, status))
    }

    suspend fun deleteInvite(code: String, serverId: String? = null) {
        val (status, body) = executeRequest("DELETE", "/invites/$code")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        if (serverId != null) {
            serverInvitesByServer[serverId] = serverInvitesByServer[serverId].orEmpty()
                .filterNot { it.effectiveCode == code }
        }
    }

    suspend fun fetchInviteByCode(code: String): Invite {
        val cleaned = code.trim().substringAfterLast('/')
        val (status, body) = executeRequest("GET", "/invites/$cleaned")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(Invite.serializer(), body)
    }

    suspend fun joinInviteByCode(code: String): InviteJoined {
        val cleaned = code.trim().substringAfterLast('/')
        val (status, body) = executeRequest("POST", "/invites/$cleaned")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        val joined = SharedJson.decodeFromString(InviteJoined.serializer(), body)
        joined.server?.let { srv ->
            srv.id?.let { serverCache[it] = srv }
        }
        joined.channels?.forEach { ch ->
            ch.id?.let { channelCache[it] = ch }
        }
        return joined
    }

    suspend fun deleteServerEmoji(emojiId: String) {
        val (status, body) = executeRequest("DELETE", "/custom/emoji/$emojiId")
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        emojiCache.remove(emojiId)
    }

    // =========================================================================
    // 7. Voice Call, Safety Reporting & Settings Sync
    // =========================================================================

    suspend fun joinVoiceCall(channelId: String): SharedJoinCallResponse {
        val nodeName = rootConfig?.features?.livekit?.url?.takeIf { it.isNotBlank() } ?: "default"
        val payload = buildJsonObject {
            put("node", nodeName)
            put("force_disconnect", true)
        }
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/channels/$channelId/join_call",
            jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
        return SharedJson.decodeFromString(SharedJoinCallResponse.serializer(), body)
    }

    suspend fun reportMessage(messageId: String, reason: ContentReportReason, additionalContext: String? = null) {
        val report = FullMessageReport(
            content = MessageReport(type = "Message", id = messageId, report_reason = reason),
            additional_context = additionalContext?.takeIf { it.isNotBlank() }
        )
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/safety/report",
            jsonBody = SharedJson.encodeToString(FullMessageReport.serializer(), report)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun reportUser(userId: String, reason: UserReportReason, additionalContext: String? = null) {
        val report = FullUserReport(
            content = UserReport(type = "User", id = userId, report_reason = reason),
            additional_context = additionalContext?.takeIf { it.isNotBlank() }
        )
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/safety/report",
            jsonBody = SharedJson.encodeToString(FullUserReport.serializer(), report)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun reportServer(serverId: String, reason: ContentReportReason, additionalContext: String? = null) {
        val report = FullServerReport(
            content = ServerReport(type = "Server", id = serverId, report_reason = reason),
            additional_context = additionalContext?.takeIf { it.isNotBlank() }
        )
        val (status, body) = executeRequest(
            method = "POST",
            pathOrUrl = "/safety/report",
            jsonBody = SharedJson.encodeToString(FullServerReport.serializer(), report)
        )
        if (!status.isSuccess()) throw Exception(parseApiError(body, status))
    }

    suspend fun syncUnreads() {
        runCatching {
            val (status, body) = executeRequest("GET", "/sync/unreads")
            if (!status.isSuccess()) return
            val unreads = SharedJson.decodeFromString(ListSerializer(ChannelUnreadResponse.serializer()), body)
            unreads.forEach { u ->
                val cid = u.id.channel
                u.last_id?.let { unreadLastReadByChannel[cid] = it }
                if (!u.mentions.isNullOrEmpty()) {
                    unreadMentionsByChannel[cid] = u.mentions!!
                } else {
                    unreadMentionsByChannel.remove(cid)
                }
            }
        }
    }

    suspend fun syncRemoteSettings() {
        runCatching {
            val payload = buildJsonObject {
                put(
                    "keys",
                    SharedJson.encodeToJsonElement(
                        ListSerializer(String.serializer()),
                        listOf("ordering", "notifications", "android-specific-settings")
                    )
                )
            }
            val (status, body) = executeRequest(
                method = "POST",
                pathOrUrl = "/sync/settings/fetch",
                jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
            )
            if (!status.isSuccess()) return
            val obj = SharedJson.parseToJsonElement(body).jsonObject
            obj["ordering"]?.jsonArray?.getOrNull(1)?.jsonPrimitive?.contentOrNull?.let { rawOrdering ->
                val orderingObj = SharedJson.parseToJsonElement(rawOrdering).jsonObject
                val serversArr = orderingObj["servers"] as? JsonArray
                if (serversArr != null) {
                    orderedServerIds.clear()
                    orderedServerIds.addAll(serversArr.mapNotNull { it.jsonPrimitive.contentOrNull })
                }
            }
        }
    }

    suspend fun pushRemoteSetting(key: String, valueJson: String) {
        runCatching {
            val payload = buildJsonObject { put(key, valueJson) }
            executeRequest(
                method = "POST",
                pathOrUrl = "/sync/settings/set",
                queryParams = mapOf("timestamp" to Clock.System.now().toEpochMilliseconds().toString()),
                jsonBody = SharedJson.encodeToString(JsonObject.serializer(), payload)
            )
        }
    }

    val changelogs: List<SharedChangelogEntry> = listOf(
        SharedChangelogEntry(
            id = "0.0.8",
            title = "DawnChat v0.0.8 — Full Multiplatform Parity",
            date = "October 2026",
            summary = "Complete 1:1 Android and iOS screen parity powered by live Stoat REST & Bonfire WebSocket APIs.",
            sections = listOf(
                "Live Bonfire WebSocket synchronization for servers, channels, messages, members, reactions, and typing indicators.",
                "Support for both Classic Drawer layout and Polar bottom-navigation mode via Experiments.",
                "Full user, server, and channel settings including MFA, active sessions, bots, roles, permissions, invites, bans, and webhooks.",
                "Integrated Gifbox & KLIPY borderless looping GIF embeds and built-in Network Inspector."
            )
        ),
        SharedChangelogEntry(
            id = "0.0.7",
            title = "DawnChat v0.0.7 — Voice & Media Enhancements",
            date = "September 2026",
            summary = "Voice call sheets, QR friend scanner, and safety reporting workflows.",
            sections = listOf(
                "Fullscreen Voice Call sheet and minimizable floating mini-player.",
                "Camera QR code scanner and personal QR code generator for adding friends.",
                "Message, User, and Server safety reporting dialogs."
            )
        )
    )
}
