package chat.stoat.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import chat.stoat.sheets.ChannelContextSheet
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.api.internals.FriendRequests
import chat.stoat.api.settings.NotificationSettingsProvider
import chat.stoat.composables.screens.chat.drawer.DMOrGroupItem
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.User
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.screens.chat.ChatRouterDestination
import chat.stoat.internals.extensions.zero
import androidx.compose.foundation.lazy.items

// Note - this is not a traditional screen per se, as it is a part of the main screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(navController: NavController) {
    var channelContextSheetTarget by remember { mutableStateOf<String?>(null) }

    channelContextSheetTarget?.let { targetId ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { channelContextSheetTarget = null }
        ) {
            ChannelContextSheet(
                channelId = targetId,
                onHideSheet = {
                    sheetState.hide()
                    channelContextSheetTarget = null
                }
            )
        }
    }

    val dmAbleChannels =
        StoatAPI.channelCache.values
            .filter { it.channelType == ChannelType.DirectMessage || it.channelType == ChannelType.Group }
            .filter { if (it.channelType == ChannelType.DirectMessage) it.active == true else true }
            .sortedBy { it.lastMessageID ?: it.id }
            .reversed()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(NavigationBarDefaults.windowInsets),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.main_tab_conversations)) },
                windowInsets = WindowInsets.zero,
                actions = {
                    IconButton(onClick = { navController.navigate("create/group") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24dp),
                            contentDescription = stringResource(R.string.create_group)
                        )
                    }
                }
            )
        },
    ) { pv ->
        LazyColumn(
            modifier = Modifier.padding(pv),
        ) {
            item(key = "friends") {
                val pending = FriendRequests.getIncoming().size
                ListItem(
                    headlineContent = { Text(stringResource(R.string.friends)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_account_circle_24dp),
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        if (pending > 0) {
                            Badge { Text(pending.toString()) }
                        }
                    },
                    modifier = Modifier.clickable { navController.navigate("friends") }
                )
                HorizontalDivider()
            }

            item(key = "saved_messages") {
                val notesChannel =
                    StoatAPI.channelCache.values.firstOrNull { it.channelType == ChannelType.SavedMessages }
                val lastMessage = notesChannel?.lastMessageID?.let { StoatAPI.messageCache[it] }
                val preview = lastMessage?.content.orEmpty()

                if (notesChannel != null) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.channel_notes))
                        },
                        supportingContent = {
                            if (preview.isNotBlank()) {
                                Text(
                                    preview,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        leadingContent = {
                            Box(contentAlignment = Alignment.TopEnd) {
                                StoatAPI.userCache[StoatAPI.selfId]?.let {
                                    UserAvatar(
                                        username = it.username.toString(),
                                        avatar = it.avatar,
                                        userId = it.id.toString(),
                                        shape = RoundedCornerShape(LoadedSettings.avatarRadius)
                                    )
                                }
                                Badge {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_keep_24dp),
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate("main/conversation/${notesChannel.id}")
                        }
                    )
                    HorizontalDivider()
                }
            }

            items(
                items = dmAbleChannels,
                key = { it.id ?: it.hashCode() }
            ) { channel ->
                val channelId = channel.id ?: return@items
                val partner = if (channel.channelType == ChannelType.DirectMessage) {
                    StoatAPI.userCache[ChannelUtils.resolveDMPartner(channel)]
                } else {
                    null
                }

                DMOrGroupItem(
                    channel = channel,
                    partner = partner,
                    isCurrent = false,
                    hasUnread = channel.lastMessageID?.let { lastMessageId ->
                        StoatAPI.unreads.hasUnread(channelId, lastMessageId, serverId = null)
                    } ?: false,
                    isMuted = NotificationSettingsProvider.isChannelMuted(channelId, null),
                    onDestinationChanged = { destination ->
                        if (destination is ChatRouterDestination.Channel) {
                            navController.navigate("main/conversation/${destination.channelId}")
                        }
                    },
                    onOpenChannelContextSheet = { channelContextSheetTarget = channelId }
                )
            }
        }
    }
}