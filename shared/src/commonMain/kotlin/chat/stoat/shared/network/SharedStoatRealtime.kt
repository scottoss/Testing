package chat.stoat.shared.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.Embed
import chat.stoat.core.model.schemas.Emoji
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.Message
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.ServerUserChoice
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.util.ChannelVoiceState
import chat.stoat.core.model.util.UserVoiceState
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

enum class SharedRealtimeState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

@Serializable
data class SharedReadyFrame(
    val type: String = "Ready",
    val users: List<User> = emptyList(),
    val servers: List<Server> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val emojis: List<Emoji> = emptyList(),
    @SerialName("voice_states") val voiceStates: List<ChannelVoiceState> = emptyList()
)

object SharedStoatRealtime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var connectionJob: Job? = null
    private var activeSession: WebSocketSession? = null

    var state by mutableStateOf(SharedRealtimeState.DISCONNECTED)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    fun ensureConnected(onReady: (() -> Unit)? = null) {
        val token = SharedStoatClient.sessionToken
        if (token.isBlank()) return
        if (state == SharedRealtimeState.CONNECTED || state == SharedRealtimeState.CONNECTING) {
            onReady?.invoke()
            return
        }
        connect(token, onReady)
    }

    fun connect(token: String = SharedStoatClient.sessionToken, onReady: (() -> Unit)? = null) {
        if (token.isBlank()) return
        connectionJob?.cancel()
        connectionJob = scope.launch {
            var retryDelayMs = 1500L
            var invokedReady = false
            while (isActive && SharedStoatClient.sessionToken.isNotBlank()) {
                state = SharedRealtimeState.CONNECTING
                val wsUrl = SharedStoatClient.wsUrl
                try {
                    SharedHttpClient.ws(wsUrl) {
                        activeSession = this
                        val authPayload = buildJsonObject {
                            put("type", "Authenticate")
                            put("token", token)
                        }.toString()
                        SharedNetworkInspector.recordWebSocketFrame("SEND", wsUrl, "Authenticate", authPayload)
                        send(authPayload)

                        val pingJob = launch {
                            while (isActive) {
                                delay(30_000L)
                                val pingPayload = buildJsonObject {
                                    put("type", "Ping")
                                    put("data", Clock.System.now().toEpochMilliseconds())
                                }.toString()
                                runCatching { send(pingPayload) }
                            }
                        }

                        try {
                            incoming.consumeEach { frame ->
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    handleRawFrame(wsUrl, text) {
                                        if (!invokedReady) {
                                            invokedReady = true
                                            onReady?.invoke()
                                        }
                                    }
                                }
                            }
                        } finally {
                            pingJob.cancel()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e.message
                } finally {
                    activeSession = null
                    state = SharedRealtimeState.DISCONNECTED
                }
                delay(retryDelayMs)
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(20_000L)
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        val session = activeSession
        activeSession = null
        state = SharedRealtimeState.DISCONNECTED
        if (session != null) {
            scope.launch {
                runCatching {
                    session.close(CloseReason(CloseReason.Codes.NORMAL, "User disconnected"))
                }
            }
        }
    }

    fun sendBeginTyping(channelId: String) {
        val session = activeSession ?: return
        scope.launch {
            runCatching {
                val payload = buildJsonObject {
                    put("type", "BeginTyping")
                    put("channel", channelId)
                }.toString()
                session.send(payload)
            }
        }
    }

    fun sendEndTyping(channelId: String) {
        val session = activeSession ?: return
        scope.launch {
            runCatching {
                val payload = buildJsonObject {
                    put("type", "EndTyping")
                    put("channel", channelId)
                }.toString()
                session.send(payload)
            }
        }
    }

    private fun handleRawFrame(wsUrl: String, rawText: String, onReadyCallback: () -> Unit) {
        val obj = runCatching { SharedJson.parseToJsonElement(rawText).jsonObject }.getOrNull() ?: return
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return

        if (type != "Pong") {
            SharedNetworkInspector.recordWebSocketFrame("RECV", wsUrl, type, rawText)
        }

        when (type) {
            "Authenticated" -> {
                state = SharedRealtimeState.CONNECTED
                lastError = null
            }

            "Bulk" -> {
                val frames = obj["v"] as? JsonArray ?: return
                frames.forEach { elem ->
                    handleRawFrame(wsUrl, elem.toString(), onReadyCallback)
                }
            }

            "Ready" -> {
                state = SharedRealtimeState.CONNECTED
                lastError = null
                val ready = runCatching {
                    SharedJson.decodeFromJsonElement(SharedReadyFrame.serializer(), obj)
                }.getOrNull() ?: return

                ready.users.forEach { u ->
                    u.id?.let { id ->
                        SharedStoatClient.userCache[id] = u
                        if (u.relationship == "User") {
                            SharedStoatClient.saveSession(
                                token = SharedStoatClient.sessionToken,
                                sessionIdValue = SharedStoatClient.sessionId,
                                userIdValue = id
                            )
                        }
                    }
                }
                ready.servers.forEach { s ->
                    s.id?.let { SharedStoatClient.serverCache[it] = s }
                }
                ready.channels.forEach { c ->
                    c.id?.let { SharedStoatClient.channelCache[it] = c }
                }
                ready.emojis.forEach { e ->
                    e.id?.let { SharedStoatClient.emojiCache[it] = e }
                }
                ready.voiceStates.forEach { vs ->
                    SharedStoatClient.voiceStateCache[vs.id] = vs
                }
                scope.launch {
                    SharedStoatClient.syncUnreads()
                    SharedStoatClient.syncRemoteSettings()
                }
                onReadyCallback()
            }

            "Message" -> {
                val msg = runCatching { SharedJson.decodeFromJsonElement(Message.serializer(), obj) }.getOrNull() ?: return
                val cid = msg.channel ?: return
                SharedStoatClient.appendMessageToChannel(cid, msg)
                val myId = SharedStoatClient.selfId
                if (myId != null && msg.mentions.orEmpty().contains(myId) && msg.id != null) {
                    val existing = SharedStoatClient.unreadMentionsByChannel[cid].orEmpty()
                    if (msg.id !in existing) {
                        SharedStoatClient.unreadMentionsByChannel[cid] = existing + msg.id!!
                    }
                }
            }

            "MessageUpdate" -> {
                val msgId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val cid = obj["channel"]?.jsonPrimitive?.contentOrNull ?: return
                val dataObj = obj["data"] as? JsonObject ?: return
                val partial = runCatching { SharedJson.decodeFromJsonElement(Message.serializer(), dataObj) }.getOrNull() ?: return
                SharedStoatClient.channelMessages[cid]?.let { list ->
                    SharedStoatClient.channelMessages[cid] = list.map {
                        if (it.id == msgId) it.mergeWithPartial(partial) else it
                    }
                }
            }

            "MessageAppend" -> {
                val msgId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val cid = obj["channel"]?.jsonPrimitive?.contentOrNull ?: return
                val appendObj = obj["append"] as? JsonObject ?: return
                val embedsArr = appendObj["embeds"] as? JsonArray ?: return
                val newEmbeds = runCatching {
                    SharedJson.decodeFromJsonElement(ListSerializer(Embed.serializer()), embedsArr)
                }.getOrNull().orEmpty()
                if (newEmbeds.isNotEmpty()) {
                    SharedStoatClient.channelMessages[cid]?.let { list ->
                        SharedStoatClient.channelMessages[cid] = list.map {
                            if (it.id == msgId) it.copy(embeds = it.embeds.orEmpty() + newEmbeds) else it
                        }
                    }
                }
            }

            "MessageDelete" -> {
                val msgId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val cid = obj["channel"]?.jsonPrimitive?.contentOrNull ?: return
                SharedStoatClient.channelMessages[cid]?.let { list ->
                    SharedStoatClient.channelMessages[cid] = list.filterNot { it.id == msgId }
                }
            }

            "MessageReact" -> {
                val msgId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val cid = obj["channel_id"]?.jsonPrimitive?.contentOrNull ?: return
                val uid = obj["user_id"]?.jsonPrimitive?.contentOrNull ?: return
                val emojiId = obj["emoji_id"]?.jsonPrimitive?.contentOrNull ?: return
                SharedStoatClient.channelMessages[cid]?.let { list ->
                    SharedStoatClient.channelMessages[cid] = list.map { msg ->
                        if (msg.id == msgId) {
                            val map = msg.reactions.orEmpty().toMutableMap()
                            val users = (map[emojiId].orEmpty() + uid).distinct()
                            map[emojiId] = users
                            msg.copy(reactions = map)
                        } else msg
                    }
                }
            }

            "MessageUnreact" -> {
                val msgId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val cid = obj["channel_id"]?.jsonPrimitive?.contentOrNull ?: return
                val uid = obj["user_id"]?.jsonPrimitive?.contentOrNull ?: return
                val emojiId = obj["emoji_id"]?.jsonPrimitive?.contentOrNull ?: return
                SharedStoatClient.channelMessages[cid]?.let { list ->
                    SharedStoatClient.channelMessages[cid] = list.map { msg ->
                        if (msg.id == msgId) {
                            val map = msg.reactions.orEmpty().toMutableMap()
                            val users = map[emojiId].orEmpty() - uid
                            if (users.isEmpty()) map.remove(emojiId) else map[emojiId] = users
                            msg.copy(reactions = map)
                        } else msg
                    }
                }
            }

            "ChannelCreate" -> {
                val ch = runCatching { SharedJson.decodeFromJsonElement(Channel.serializer(), obj) }.getOrNull() ?: return
                val cid = ch.id ?: return
                SharedStoatClient.channelCache[cid] = ch
                ch.server?.let { sid ->
                    SharedStoatClient.serverCache[sid]?.let { srv ->
                        if (cid !in srv.channels.orEmpty()) {
                            SharedStoatClient.serverCache[sid] = srv.copy(channels = srv.channels.orEmpty() + cid)
                        }
                    }
                }
            }

            "ChannelUpdate" -> {
                val cid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val dataObj = obj["data"] as? JsonObject ?: return
                val partial = runCatching { SharedJson.decodeFromJsonElement(Channel.serializer(), dataObj) }.getOrNull() ?: return
                val current = SharedStoatClient.channelCache[cid]
                SharedStoatClient.channelCache[cid] = current?.mergeWithPartial(partial) ?: partial.copy(id = cid)
            }

            "ChannelDelete" -> {
                val cid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val removed = SharedStoatClient.channelCache.remove(cid)
                removed?.server?.let { sid ->
                    SharedStoatClient.serverCache[sid]?.let { srv ->
                        SharedStoatClient.serverCache[sid] = srv.copy(channels = srv.channels.orEmpty() - cid)
                    }
                }
            }

            "ChannelStartTyping" -> {
                val cid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val uid = obj["user"]?.jsonPrimitive?.contentOrNull ?: return
                if (uid != SharedStoatClient.selfId) {
                    val set = SharedStoatClient.typingUsersByChannel[cid].orEmpty()
                    SharedStoatClient.typingUsersByChannel[cid] = set + uid
                }
            }

            "ChannelStopTyping" -> {
                val cid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val uid = obj["user"]?.jsonPrimitive?.contentOrNull ?: return
                val set = SharedStoatClient.typingUsersByChannel[cid].orEmpty()
                SharedStoatClient.typingUsersByChannel[cid] = set - uid
            }

            "ChannelAck" -> {
                val cid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val msgId = obj["message_id"]?.jsonPrimitive?.contentOrNull ?: return
                SharedStoatClient.unreadLastReadByChannel[cid] = msgId
                SharedStoatClient.unreadMentionsByChannel.remove(cid)
            }

            "ServerCreate" -> {
                val srvObj = obj["server"] as? JsonObject ?: return
                val srv = runCatching { SharedJson.decodeFromJsonElement(Server.serializer(), srvObj) }.getOrNull() ?: return
                val sid = srv.id ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                SharedStoatClient.serverCache[sid] = srv
                (obj["channels"] as? JsonArray)?.forEach { chElem ->
                    runCatching { SharedJson.decodeFromJsonElement(Channel.serializer(), chElem) }.getOrNull()?.let { ch ->
                        ch.id?.let { SharedStoatClient.channelCache[it] = ch }
                    }
                }
            }

            "ServerUpdate" -> {
                val sid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val dataObj = obj["data"] as? JsonObject ?: return
                val partial = runCatching { SharedJson.decodeFromJsonElement(Server.serializer(), dataObj) }.getOrNull() ?: return
                val current = SharedStoatClient.serverCache[sid]
                SharedStoatClient.serverCache[sid] = current?.mergeWithPartial(partial) ?: partial.copy(id = sid)
            }

            "ServerDelete" -> {
                val sid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val removed = SharedStoatClient.serverCache.remove(sid)
                removed?.channels.orEmpty().forEach { SharedStoatClient.channelCache.remove(it) }
            }

            "ServerMemberUpdate" -> {
                val idObj = obj["id"] as? JsonObject ?: return
                val choice = runCatching { SharedJson.decodeFromJsonElement(ServerUserChoice.serializer(), idObj) }.getOrNull() ?: return
                val dataObj = obj["data"] as? JsonObject ?: return
                val partial = runCatching { SharedJson.decodeFromJsonElement(Member.serializer(), dataObj) }.getOrNull() ?: return
                val currentMap = SharedStoatClient.memberCache[choice.server].orEmpty()
                val existing = currentMap[choice.user]
                val merged = existing?.mergeWithPartial(partial) ?: partial.copy(id = choice)
                SharedStoatClient.memberCache[choice.server] = currentMap + (choice.user to merged)
            }

            "ServerRoleUpdate" -> {
                val sid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val rid = obj["role_id"]?.jsonPrimitive?.contentOrNull ?: return
                val dataObj = obj["data"] as? JsonObject ?: return
                val partial = runCatching { SharedJson.decodeFromJsonElement(Role.serializer(), dataObj) }.getOrNull() ?: return
                SharedStoatClient.serverCache[sid]?.let { srv ->
                    val existing = srv.roles.orEmpty()[rid]
                    val merged = existing?.mergeWithPartial(partial) ?: partial
                    SharedStoatClient.serverCache[sid] = srv.copy(roles = srv.roles.orEmpty() + (rid to merged))
                }
            }

            "ServerRoleDelete" -> {
                val sid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val rid = obj["role_id"]?.jsonPrimitive?.contentOrNull ?: return
                SharedStoatClient.serverCache[sid]?.let { srv ->
                    SharedStoatClient.serverCache[sid] = srv.copy(roles = srv.roles.orEmpty() - rid)
                }
            }

            "UserUpdate" -> {
                val uid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val dataObj = obj["data"] as? JsonObject ?: return
                val partial = runCatching { SharedJson.decodeFromJsonElement(User.serializer(), dataObj) }.getOrNull() ?: return
                val existing = SharedStoatClient.userCache[uid]
                SharedStoatClient.userCache[uid] = existing?.mergeWithPartial(partial) ?: partial.copy(id = uid)
            }

            "UserRelationship" -> {
                val userObj = obj["user"] as? JsonObject ?: return
                val updatedUser = runCatching { SharedJson.decodeFromJsonElement(User.serializer(), userObj) }.getOrNull() ?: return
                val uid = updatedUser.id ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val status = obj["status"]?.jsonPrimitive?.contentOrNull
                SharedStoatClient.userCache[uid] = updatedUser.copy(relationship = status ?: updatedUser.relationship)
            }

            "EmojiCreate" -> {
                val emoji = runCatching { SharedJson.decodeFromJsonElement(Emoji.serializer(), obj) }.getOrNull() ?: return
                emoji.id?.let { SharedStoatClient.emojiCache[it] = emoji }
            }

            "EmojiDelete" -> {
                val eid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                SharedStoatClient.emojiCache.remove(eid)
            }

            "VoiceChannelJoin" -> {
                val cid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val stateObj = obj["state"] as? JsonObject ?: return
                val userVoice = runCatching { SharedJson.decodeFromJsonElement(UserVoiceState.serializer(), stateObj) }.getOrNull() ?: return
                val current = SharedStoatClient.voiceStateCache[cid]
                val updatedList = (current?.participants.orEmpty().filterNot { it.id == userVoice.id }) + userVoice
                SharedStoatClient.voiceStateCache[cid] = ChannelVoiceState(id = cid, participants = updatedList)
            }

            "VoiceChannelLeave" -> {
                val cid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return
                val uid = obj["user"]?.jsonPrimitive?.contentOrNull ?: return
                val current = SharedStoatClient.voiceStateCache[cid] ?: return
                SharedStoatClient.voiceStateCache[cid] = current.copy(
                    participants = current.participants.filterNot { it.id == uid }
                )
            }
        }
    }
}
