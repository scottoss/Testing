package chat.stoat.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.channel.leaveDeleteOrCloseChannel
import chat.stoat.api.settings.NotificationSettingsProvider
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.composables.generic.SheetButton
import chat.stoat.core.model.data.STOAT_WEB_APP
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.internals.Platform
import chat.stoat.internals.extensions.rememberChannelPermissions
import kotlinx.coroutines.launch

@Composable
fun ChannelContextSheet(channelId: String, onHideSheet: suspend () -> Unit) {
    val channel = StoatAPI.channelCache[channelId]
    if (channel == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val permissions by rememberChannelPermissions(channelId)
    var showConfirm by remember { mutableStateOf(false) }

    val channelName = ChannelUtils.resolveName(channel) ?: channelId
    val isMuted = NotificationSettingsProvider.isChannelMuted(channelId, channel.server)
    val hasUnread = channel.lastMessageID?.let {
        StoatAPI.unreads.hasUnread(channelId, it, channel.server)
    } == true

    val destructive = when (channel.channelType) {
        ChannelType.DirectMessage -> Triple(
            R.string.channel_context_sheet_actions_close_dm,
            R.string.channel_context_sheet_actions_close_dm_confirm,
            R.string.channel_context_sheet_actions_close_dm_body
        )
        ChannelType.Group -> Triple(
            R.string.channel_context_sheet_actions_leave_group,
            R.string.channel_context_sheet_actions_leave_group_confirm,
            R.string.channel_context_sheet_actions_leave_group_body
        )
        ChannelType.TextChannel, ChannelType.VoiceChannel ->
            if (permissions.hasPermission(PermissionBit.ManageChannel)) Triple(
                R.string.channel_context_sheet_actions_delete_channel,
                R.string.channel_context_sheet_actions_delete_channel_confirm,
                R.string.channel_context_sheet_actions_delete_channel_body
            ) else null
        else -> null
    }
    val confirmBtn = when (channel.channelType) {
        ChannelType.DirectMessage -> R.string.channel_context_sheet_actions_close_dm_confirm_btn
        ChannelType.Group -> R.string.channel_context_sheet_actions_leave_group_confirm_btn
        else -> R.string.channel_context_sheet_actions_delete_channel_confirm_btn
    }

    if (showConfirm && destructive != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(destructive.second, channelName)) },
            text = { Text(stringResource(destructive.third)) },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.channel_context_sheet_actions_dialog_cancel))
                }
            },
            confirmButton = {
                Button(onClick = {
                    showConfirm = false
                    coroutineScope.launch {
                        leaveDeleteOrCloseChannel(channelId)
                        onHideSheet()
                    }
                }) {
                    Text(stringResource(confirmBtn))
                }
            }
        )
    }

    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.channel_context_sheet_actions_copy_id),
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.ic_identifier_copy_24dp),
                contentDescription = null
            )
        },
        onClick = {
            if (channel.id == null) return@SheetButton

            clipboardManager.setText(AnnotatedString(channel.id!!))

            if (Platform.needsShowClipboardNotification()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.channel_context_sheet_actions_copy_id_copied),
                    Toast.LENGTH_SHORT
                ).show()
            }

            coroutineScope.launch {
                onHideSheet()
            }
        }
    )

    if (hasUnread) {
        SheetButton(
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.channel_context_sheet_actions_mark_read),
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mark_chat_read_24dp),
                    contentDescription = null
                )
            },
            onClick = {
                coroutineScope.launch {
                    channel.lastMessageID?.let {
                        StoatAPI.unreads.markAsRead(channelId, it, sync = true)
                    }
                    onHideSheet()
                }
            }
        )
    }

    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(
                    if (isMuted) R.string.channel_context_sheet_actions_unmute
                    else R.string.channel_context_sheet_actions_mute
                ),
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(
                    id = if (isMuted) R.drawable.ic_notifications_24dp
                    else R.drawable.ic_notifications_off_24dp
                ),
                contentDescription = null
            )
        },
        onClick = {
            coroutineScope.launch {
                val current = SyncedSettings.notifications
                val updated = if (isMuted) current.channel - channelId
                else current.channel + (channelId to "muted")
                SyncedSettings.updateNotifications(current.copy(channel = updated))
                onHideSheet()
            }
        }
    )

    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.channel_context_sheet_actions_copy_link),
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.ic_link_24dp),
                contentDescription = null
            )
        },
        onClick = {
            val path = if (channel.server != null) {
                "/server/${channel.server}/channel/$channelId"
            } else {
                "/channel/$channelId"
            }
            clipboardManager.setText(AnnotatedString("$STOAT_WEB_APP$path"))

            if (Platform.needsShowClipboardNotification()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.channel_context_sheet_actions_copy_link_copied),
                    Toast.LENGTH_SHORT
                ).show()
            }

            coroutineScope.launch {
                onHideSheet()
            }
        }
    )

    if (destructive != null) {
        SheetButton(
            headlineContent = {
                Text(text = stringResource(destructive.first))
            },
            leadingContent = {
                Icon(
                    painter = painterResource(
                        id = if (channel.channelType == ChannelType.DirectMessage)
                            R.drawable.ic_close_24dp
                        else if (channel.channelType == ChannelType.Group)
                            R.drawable.ic_door_open_24dp
                        else R.drawable.ic_delete_24dp
                    ),
                    contentDescription = null
                )
            },
            dangerous = true,
            onClick = { showConfirm = true }
        )
    }
}
