package chat.stoat.composables.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.invites.fetchInviteByCode
import chat.stoat.api.routes.invites.joinInviteByCode
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.IconPlaceholder
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Invite
import chat.stoat.core.model.util.RsResult
import kotlinx.coroutines.launch

private val INVITE_URL_REGEX = Regex(
    """(?:https?://)?(?:(?:www\.)?csinvite\.net(?:/dc)?|(?:www\.)?dawn-chat\.com/invite|(?:www\.)?stoat\.chat/invite|(?:www\.)?rvlt\.gg|(?:www\.)?stt\.gg|(?:www\.)?revolt\.chat/invite|(?:app\.)?revolt\.chat/invite)/([a-zA-Z0-9_-]+)""",
    RegexOption.IGNORE_CASE
)

fun extractInviteCodes(text: String?): List<String> {
    if (text.isNullOrBlank()) return emptyList()
    return INVITE_URL_REGEX.findAll(text).map { it.groupValues[1] }.distinct().toList()
}

@Composable
fun InviteEmbed(
    inviteCode: String,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var inviteResult by remember(inviteCode) { mutableStateOf<RsResult<Invite, *>?>(null) }
    var isLoading by remember(inviteCode) { mutableStateOf(true) }
    var isJoining by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(inviteCode) {
        isLoading = true
        try {
            inviteResult = fetchInviteByCode(inviteCode)
        } catch (e: Exception) {
            inviteResult = null
        } finally {
            isLoading = false
        }
    }

    val invite = inviteResult?.value
    val serverId = invite?.serverId
    val isAlreadyJoined = serverId != null && StoatAPI.serverCache.containsKey(serverId)

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
                Text(
                    text = "Loading invite preview…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (invite == null || inviteResult?.ok != true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_error_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Invalid or Expired Invite",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "This invite link may have expired or is invalid.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "YOU'VE BEEN INVITED TO JOIN A SERVER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val serverIcon = invite.serverIcon
                    val serverName = invite.serverName ?: "Server"
                    val memberCount = invite.memberCount
                    val channelName = invite.channelName

                    // Server Icon
                    if (serverIcon != null) {
                        RemoteImage(
                            url = "$STOAT_FILES/icons/${serverIcon.id}",
                            width = 52,
                            height = 52,
                            contentScale = ContentScale.Crop,
                            description = serverName,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        IconPlaceholder(
                            name = serverName,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = serverName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (memberCount != null && memberCount > 0L) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Text(
                                    text = "$memberCount Members",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        } else if (!channelName.isNullOrBlank()) {
                            Text(
                                text = "#$channelName",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    // Join / Joined Button
                    Button(
                        onClick = {
                            if (isAlreadyJoined) {
                                val targetChannel = invite.channelId ?: invite.serverId ?: return@Button
                                scope.launch {
                                    ActionChannel.send(Action.SwitchChannel(targetChannel))
                                }
                            } else {
                                isJoining = true
                                joinError = null
                                scope.launch {
                                    try {
                                        val joinRes = joinInviteByCode(inviteCode)
                                        val joined = joinRes.value
                                        if (joinRes.ok && joined != null) {
                                            joined.server?.let { server ->
                                                server.id?.let { sid ->
                                                    StoatAPI.serverCache[sid] = server
                                                }
                                            }
                                            joined.channels?.forEach { ch ->
                                                ch.id?.let { cid ->
                                                    StoatAPI.channelCache[cid] = ch
                                                }
                                            }
                                            val targetChannel = joined.channels?.firstOrNull()?.id ?: invite.channelId
                                            if (targetChannel != null) {
                                                ActionChannel.send(Action.SwitchChannel(targetChannel))
                                            }
                                        } else {
                                            joinError = "Could not join server."
                                        }
                                    } catch (e: Exception) {
                                        joinError = e.message ?: "Failed to join"
                                    } finally {
                                        isJoining = false
                                    }
                                }
                            }
                        },
                        enabled = !isJoining,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAlreadyJoined) MaterialTheme.colorScheme.secondaryContainer else Color(0xFF22C55E),
                            contentColor = if (isAlreadyJoined) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isAlreadyJoined) "Joined" else "Join",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                if (joinError != null) {
                    Text(
                        text = joinError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
