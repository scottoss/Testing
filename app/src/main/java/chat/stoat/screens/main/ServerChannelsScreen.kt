package chat.stoat.screens.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.composables.screens.chat.ChannelIcon
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.ServerFlags
import chat.stoat.core.model.schemas.has
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.screens.chat.dialogs.safety.ReportServerDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import chat.stoat.sheets.ServerContextSheet
import chat.stoat.sheets.ServerFolderPickerSheet
import chat.stoat.sheets.ChannelContextSheet
import chat.stoat.voice.VoiceCallManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ServerChannelsScreen(navController: NavController, serverId: String) {
    val server = StoatAPI.serverCache[serverId]
    var contextTarget by remember { mutableStateOf<String?>(null) }
    var showServerSheet by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var collapsedCategoryIds by rememberSaveable(serverId) { mutableStateOf(listOf<String>()) }

    if (showServerSheet) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(sheetState = state, onDismissRequest = { showServerSheet = false }) {
            ServerContextSheet(
                serverId = serverId,
                onHideSheet = {
                    state.hide()
                    showServerSheet = false
                },
                onReportServer = { showReport = true },
                onPickFolder = {
                    state.hide()
                    showServerSheet = false
                    showFolderPicker = true
                }
            )
        }
    }

    if (showFolderPicker) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(sheetState = state, onDismissRequest = { showFolderPicker = false }) {
            ServerFolderPickerSheet(
                serverId = serverId,
                onHideSheet = {
                    state.hide()
                    showFolderPicker = false
                }
            )
        }
    }

    if (showReport) {
        ReportServerDialog(onDismiss = { showReport = false }, serverId = serverId)
    }

    contextTarget?.let { channelId ->
        val state = rememberModalBottomSheetState()
        ModalBottomSheet(sheetState = state, onDismissRequest = { contextTarget = null }) {
            ChannelContextSheet(
                channelId = channelId,
                onHideSheet = {
                    state.hide()
                    contextTarget = null
                }
            )
        }
    }

    val categories = server?.categories.orEmpty()
    val categorised = categories.flatMap { it.channels.orEmpty() }.toSet()
    val uncategorised = server?.channels.orEmpty().filter { it !in categorised }

    @Composable
    fun ChannelRow(channelId: String) {
        val channel: Channel = StoatAPI.channelCache[channelId] ?: return
        val hasUnread = channel.lastMessageID?.let {
            StoatAPI.unreads.hasUnread(channelId, it, serverId)
        } == true
        ListItem(
            headlineContent = {
                Text(
                    ChannelUtils.resolveName(channel) ?: channelId,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = { ChannelIcon(channel, Modifier.size(24.dp)) },
            trailingContent = { if (hasUnread) Badge() },
            modifier = Modifier.combinedClickable(
                onClick = {
                    if (channel.channelType == ChannelType.VoiceChannel) {
                        VoiceCallManager.openSheet(channelId)
                    } else {
                        navController.navigate("main/conversation/$channelId")
                    }
                },
                onLongClick = { contextTarget = channelId }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (server?.flags has ServerFlags.Official) {
                            Icon(
                                painter = painterResource(R.drawable.ic_workspace_premium_24dp__fill),
                                contentDescription = stringResource(R.string.server_flag_official),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (server?.flags has ServerFlags.Verified) {
                            Icon(
                                painter = painterResource(R.drawable.ic_verified_24dp__fill),
                                contentDescription = stringResource(R.string.server_flag_verified),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            server?.name ?: stringResource(R.string.unknown),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showServerSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert_24dp),
                            contentDescription = stringResource(R.string.menu)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { pv ->
        LazyColumn(modifier = Modifier.padding(pv)) {
            server?.banner?.let { banner ->
                item(key = "banner") {
                    RemoteImage(
                        url = "$STOAT_FILES/banners/${banner.id}/original",
                        description = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            }
            uncategorised.forEach { id -> item(key = "u/$id") { ChannelRow(id) } }
            categories.forEachIndexed { index, category ->
                val catKey = category.id ?: category.title ?: "cat_$index"
                val isCollapsed = catKey in collapsedCategoryIds
                item(key = "c/${category.id ?: catKey}") {
                    val rotation by animateFloatAsState(
                        targetValue = if (isCollapsed) 0f else 90f,
                        animationSpec = tween(durationMillis = 200),
                        label = "category_screen_rotation"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                collapsedCategoryIds = if (isCollapsed) {
                                    collapsedCategoryIds - catKey
                                } else {
                                    collapsedCategoryIds + catKey
                                }
                            }
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard_arrow_right_24dp),
                            contentDescription = if (isCollapsed) "Expand" else "Collapse",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(rotation)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            category.title.orEmpty().uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!isCollapsed) {
                    category.channels.orEmpty().forEach { id ->
                        item(key = "c/${category.id ?: catKey}/$id") { ChannelRow(id) }
                    }
                }
            }
        }
    }
}
