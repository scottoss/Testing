package chat.stoat.internals.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.routes.server.fetchMember

@Composable
fun rememberChannelPermissions(channelId: String, key1: Any = Unit): MutableLongState {
    val permissions = rememberSaveable(channelId) { mutableLongStateOf(PermissionBit.GrantAllSafe.value) }
    val selfId = StoatAPI.selfId
    val selfUser = selfId?.let { StoatAPI.userCache[it] }
    val channel = StoatAPI.channelCache[channelId]
    val server = channel?.server?.let { StoatAPI.serverCache[it] }

    LaunchedEffect(channelId, key1, selfId, selfUser, channel, server) {
        val currentChannel = StoatAPI.channelCache[channelId] ?: return@LaunchedEffect
        val currentSelfId = StoatAPI.selfId
        val currentSelfUser = currentSelfId?.let { StoatAPI.userCache[it] }
        val member = currentChannel.server?.let { serverId ->
            currentSelfId?.let { uid ->
                StoatAPI.members.getMember(serverId, uid)
                    ?: runCatching { fetchMember(serverId, uid) }.getOrNull()
            }
        }
        permissions.longValue = Roles.permissionFor(currentChannel, currentSelfUser, member)
    }

    return permissions
}

@Composable
fun rememberServerPermissions(serverId: String, key1: Any = Unit): State<Long?> {
    val permissions = remember(serverId) { mutableStateOf<Long?>(null) }
    val selfId = StoatAPI.selfId
    val server = StoatAPI.serverCache[serverId]
    val selfUser = selfId?.let(StoatAPI.userCache::get)

    LaunchedEffect(serverId, key1, selfId, selfUser, server) {
        permissions.value = null

        if (server == null || selfId == null) {
            return@LaunchedEffect
        }

        if (server.owner == selfId) {
            permissions.value = PermissionBit.GrantAllSafe.value
            return@LaunchedEffect
        }

        if (selfUser == null) return@LaunchedEffect

        if (selfUser.privileged == true) {
            permissions.value = PermissionBit.GrantAllSafe.value
            return@LaunchedEffect
        }

        val member = StoatAPI.members.getMember(serverId, selfId)
            ?: runCatching { fetchMember(serverId, selfId) }.getOrNull()

        permissions.value = member?.let { Roles.permissionFor(server, it) } ?: 0L
    }

    return permissions
}
