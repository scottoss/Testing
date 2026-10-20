package chat.stoat.shared.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.core.model.data.DAWNCHAT_DISCOVERY
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.Message
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.User
import chat.stoat.shared.gifs.SharedGifCategory
import chat.stoat.shared.gifs.SharedGifItem
import chat.stoat.shared.gifs.SharedGifProvider
import chat.stoat.shared.gifs.SharedGifService
import chat.stoat.shared.gifs.SharedResolvedGifMedia
import chat.stoat.shared.network.SharedJson
import chat.stoat.shared.network.SharedRealtimeState
import chat.stoat.shared.network.SharedSendMessageReply
import chat.stoat.shared.network.SharedStoatClient
import chat.stoat.shared.network.SharedStoatRealtime
import chat.stoat.shared.platform.FriendQrParser
import chat.stoat.shared.platform.PlatformAudioPlayer
import chat.stoat.shared.platform.PlatformInfo
import chat.stoat.shared.platform.PlatformLoopingVideoView
import chat.stoat.shared.platform.PlatformQrScannerView
import chat.stoat.shared.platform.VoiceCallController
import chat.stoat.shared.platform.VoiceConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.random.Random

enum class ClassicHomeTab {
    OVERVIEW,
    FRIENDS,
    CHANNEL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedChatRouterScreen(
    navigator: SharedNavigator,
    voiceController: VoiceCallController,
    onOpenVoiceFullscreen: () -> Unit,
    onOpenCreateJoinServer: () -> Unit,
    onOpenInviteDialog: (String) -> Unit,
    onOpenServerContextSheet: (Server) -> Unit,
    onOpenUserProfileSheet: (User, String?) -> Unit,
    onOpenMessageContextSheet: (Message) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val servers = SharedStoatClient.sortedServers()
    val dmChannels = SharedStoatClient.dmAndGroupChannels()

    // null = Home / DMs rail item selected; non-null = Server ID selected
    var selectedServerId by remember { mutableStateOf<String?>(null) }
    var selectedChannelId by remember { mutableStateOf<String?>(null) }
    var homeTab by remember { mutableStateOf(ClassicHomeTab.OVERVIEW) }
    var previewedVoiceChannelId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        SharedStoatRealtime.ensureConnected()
    }

    // Fetch server members when a server is selected
    LaunchedEffect(selectedServerId) {
        val sid = selectedServerId ?: return@LaunchedEffect
        if (SharedStoatClient.memberCache[sid].isNullOrEmpty()) {
            runCatching { SharedStoatClient.fetchServerMembers(sid) }
        }
    }

    val activeServer = selectedServerId?.let { SharedStoatClient.serverCache[it] }
    val activeChannel = selectedChannelId?.let { SharedStoatClient.channelCache[it] }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(328.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // 1. Server Rail (72.dp) — Tapping a server updates selectedServerId WITHOUT closing the drawer!
                        LazyColumn(
                            modifier = Modifier
                                .width(72.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(if (selectedServerId == null) 16.dp else 24.dp),
                                    color = if (selectedServerId == null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable {
                                            selectedServerId = null
                                            previewedVoiceChannelId = null
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = "Home & Direct Messages",
                                            tint = if (selectedServerId == null) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }

                            item {
                                HorizontalDivider(modifier = Modifier.width(32.dp))
                            }

                            items(servers, key = { it.id ?: it.name.orEmpty() }) { server ->
                                val sid = server.id ?: return@items
                                val isSelected = selectedServerId == sid
                                val isUnread = SharedStoatClient.isServerUnread(sid)
                                val mentions = SharedStoatClient.serverMentionCount(sid)

                                Box(contentAlignment = Alignment.Center) {
                                    if (isUnread || isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .width(4.dp)
                                                .height(if (isSelected) 32.dp else 12.dp)
                                                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    SharedAvatarCircle(
                                        label = server.name ?: "S",
                                        seed = sid,
                                        size = 48.dp,
                                        isSelected = isSelected,
                                        modifier = Modifier.clickable {
                                            // Switch channel list in drawer WITHOUT closing drawer
                                            selectedServerId = sid
                                            previewedVoiceChannelId = null
                                        }
                                    )
                                    if (mentions > 0) {
                                        Badge(
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Text(mentions.toString())
                                        }
                                    }
                                }
                            }

                            item {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { onOpenCreateJoinServer() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Create or Join Server",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            item {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            navigator.navigate(SharedRoute.Discover)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Explore,
                                            contentDescription = "Discover Communities",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Channel / DM List Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(12.dp)
                        ) {
                            if (activeServer == null) {
                                Text(
                                    text = "Direct Messages",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                DrawerNavRow(
                                    icon = Icons.Default.Home,
                                    label = "Overview",
                                    selected = homeTab == ClassicHomeTab.OVERVIEW && selectedChannelId == null,
                                    onClick = {
                                        homeTab = ClassicHomeTab.OVERVIEW
                                        selectedChannelId = null
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                DrawerNavRow(
                                    icon = Icons.Default.People,
                                    label = "Friends",
                                    selected = homeTab == ClassicHomeTab.FRIENDS && selectedChannelId == null,
                                    onClick = {
                                        homeTab = ClassicHomeTab.FRIENDS
                                        selectedChannelId = null
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                DrawerNavRow(
                                    icon = Icons.Default.NotificationsActive,
                                    label = "Catch Up",
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navigator.navigate(SharedRoute.CatchUp)
                                    }
                                )
                                DrawerNavRow(
                                    icon = Icons.Default.Bookmark,
                                    label = "Saved Notes",
                                    selected = activeChannel?.channelType == ChannelType.SavedMessages,
                                    onClick = {
                                        val saved = dmChannels.firstOrNull { it.channelType == ChannelType.SavedMessages }
                                        if (saved?.id != null) {
                                            selectedChannelId = saved.id
                                            homeTab = ClassicHomeTab.CHANNEL
                                            scope.launch { drawerState.close() }
                                        } else {
                                            onStatusMessage("No Saved Notes channel opened yet")
                                        }
                                    }
                                )
                                DrawerNavRow(
                                    icon = Icons.Default.GroupAdd,
                                    label = "New Group DM",
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navigator.navigate(SharedRoute.CreateGroup)
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(dmChannels, key = { it.id ?: "" }) { dm ->
                                        val cid = dm.id ?: return@items
                                        val title = SharedStoatClient.resolveChannelTitle(dm)
                                        val unread = SharedStoatClient.isChannelUnread(cid)
                                        val mentions = SharedStoatClient.channelMentionCount(cid)

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (selectedChannelId == cid) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                Color.Transparent
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedChannelId = cid
                                                    homeTab = ClassicHomeTab.CHANNEL
                                                    scope.launch { drawerState.close() }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                SharedAvatarCircle(
                                                    label = title,
                                                    seed = cid,
                                                    size = 32.dp
                                                )
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (mentions > 0) {
                                                    Badge { Text(mentions.toString()) }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Server Header
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenServerContextSheet(activeServer) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = activeServer.name ?: "Server",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.MoreVert, contentDescription = "Server Menu")
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                val serverChannelObjs = remember(activeServer, SharedStoatClient.channelCache.size) {
                                    activeServer.channels.orEmpty().mapNotNull { SharedStoatClient.channelCache[it] }
                                }
                                val categorizedIds = remember(activeServer) {
                                    activeServer.categories.orEmpty().flatMap { it.channels.orEmpty() }.toSet()
                                }
                                val uncategorized = remember(serverChannelObjs, categorizedIds) {
                                    serverChannelObjs.filter { it.id !in categorizedIds }
                                }

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uncategorized, key = { it.id ?: "" }) { ch ->
                                        ServerChannelDrawerItem(
                                            channel = ch,
                                            isSelected = selectedChannelId == ch.id,
                                            isVoicePreviewed = previewedVoiceChannelId == ch.id,
                                            voiceController = voiceController,
                                            onSelectTextChannel = {
                                                selectedChannelId = ch.id
                                                homeTab = ClassicHomeTab.CHANNEL
                                                scope.launch { drawerState.close() }
                                            },
                                            onToggleVoicePreview = {
                                                previewedVoiceChannelId = if (previewedVoiceChannelId == ch.id) null else ch.id
                                            },
                                            onJoinVoice = {
                                                val cid = ch.id ?: return@ServerChannelDrawerItem
                                                val cname = ch.name ?: "Voice"
                                                scope.launch {
                                                    drawerState.close()
                                                    val joinResp = runCatching { SharedStoatClient.joinVoiceCall(cid) }.getOrNull()
                                                    voiceController.connect(
                                                        channelId = cid,
                                                        channelName = cname,
                                                        livekitUrl = joinResp?.url.orEmpty(),
                                                        token = joinResp?.token.orEmpty()
                                                    )
                                                    onOpenVoiceFullscreen()
                                                }
                                            }
                                        )
                                    }

                                    activeServer.categories.orEmpty().forEach { category ->
                                        item(key = "cat_${category.id ?: category.title}") {
                                            Text(
                                                text = (category.title ?: "CATEGORY").uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 6.dp)
                                            )
                                        }
                                        val catChannels = category.channels.orEmpty().mapNotNull { SharedStoatClient.channelCache[it] }
                                        items(catChannels, key = { it.id ?: "" }) { ch ->
                                            ServerChannelDrawerItem(
                                                channel = ch,
                                                isSelected = selectedChannelId == ch.id,
                                                isVoicePreviewed = previewedVoiceChannelId == ch.id,
                                                voiceController = voiceController,
                                                onSelectTextChannel = {
                                                    selectedChannelId = ch.id
                                                    homeTab = ClassicHomeTab.CHANNEL
                                                    scope.launch { drawerState.close() }
                                                },
                                                onToggleVoicePreview = {
                                                    previewedVoiceChannelId = if (previewedVoiceChannelId == ch.id) null else ch.id
                                                },
                                                onJoinVoice = {
                                                    val cid = ch.id ?: return@ServerChannelDrawerItem
                                                    val cname = ch.name ?: "Voice"
                                                    scope.launch {
                                                        drawerState.close()
                                                        val joinResp = runCatching { SharedStoatClient.joinVoiceCall(cid) }.getOrNull()
                                                        voiceController.connect(
                                                            channelId = cid,
                                                            channelName = cname,
                                                            livekitUrl = joinResp?.url.orEmpty(),
                                                            token = joinResp?.token.orEmpty()
                                                        )
                                                        onOpenVoiceFullscreen()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Self User Bar
                    val selfUser = SharedStoatClient.currentUser
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SharedAvatarCircle(
                                label = selfUser?.let { User.resolveDefaultName(it) } ?: "Me",
                                seed = selfUser?.id ?: "self",
                                size = 38.dp,
                                online = true,
                                presence = selfUser?.status?.presence,
                                modifier = Modifier.clickable {
                                    selfUser?.let { onOpenUserProfileSheet(it, selectedServerId) }
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selfUser?.let { User.resolveDefaultName(it) } ?: "Signed In",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when (SharedStoatRealtime.state) {
                                        SharedRealtimeState.CONNECTED -> selfUser?.status?.text ?: "Connected (Live)"
                                        SharedRealtimeState.CONNECTING -> "Connecting WebSocket..."
                                        SharedRealtimeState.DISCONNECTED -> "Reconnecting..."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navigator.navigate(SharedRoute.SettingsRoot)
                                }
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    }
                }
            }
        }
    ) {
        // Main Content Area
        when {
            selectedChannelId != null -> {
                SharedChannelScreen(
                    channelId = selectedChannelId!!,
                    navigator = navigator,
                    voiceController = voiceController,
                    onOpenVoiceFullscreen = onOpenVoiceFullscreen,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenInvite = { onOpenInviteDialog(selectedChannelId!!) },
                    onOpenUserProfile = { u -> onOpenUserProfileSheet(u, activeChannel?.server) },
                    onOpenMessageContext = onOpenMessageContextSheet,
                    onStatusMessage = onStatusMessage
                )
            }
            homeTab == ClassicHomeTab.FRIENDS -> {
                SharedFriendsScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenDm = { dm ->
                        selectedChannelId = dm.id
                        homeTab = ClassicHomeTab.CHANNEL
                    },
                    onOpenUserProfile = { u -> onOpenUserProfileSheet(u, null) },
                    onStatusMessage = onStatusMessage
                )
            }
            else -> {
                SharedOverviewScreen(
                    navigator = navigator,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onSelectServer = { sid ->
                        selectedServerId = sid
                        val firstText = SharedStoatClient.serverCache[sid]?.channels.orEmpty()
                            .mapNotNull { SharedStoatClient.channelCache[it] }
                            .firstOrNull { it.channelType != ChannelType.VoiceChannel }
                        if (firstText?.id != null) {
                            selectedChannelId = firstText.id
                            homeTab = ClassicHomeTab.CHANNEL
                        } else {
                            scope.launch { drawerState.open() }
                        }
                    },
                    onSelectChannel = { cid ->
                        selectedChannelId = cid
                        homeTab = ClassicHomeTab.CHANNEL
                    },
                    onOpenFriends = {
                        selectedChannelId = null
                        homeTab = ClassicHomeTab.FRIENDS
                    },
                    onOpenCreateJoinServer = onOpenCreateJoinServer
                )
            }
        }
    }
}

@Composable
private fun DrawerNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ServerChannelDrawerItem(
    channel: Channel,
    isSelected: Boolean,
    isVoicePreviewed: Boolean,
    voiceController: VoiceCallController,
    onSelectTextChannel: () -> Unit,
    onToggleVoicePreview: () -> Unit,
    onJoinVoice: () -> Unit
) {
    val cid = channel.id ?: return
    val isVoice = channel.channelType == ChannelType.VoiceChannel || channel.voice != null
    val unread = SharedStoatClient.isChannelUnread(cid)
    val mentions = SharedStoatClient.channelMentionCount(cid)
    val activeVoiceId by voiceController.activeChannelId.collectAsState()
    val voiceConnState by voiceController.connectionState.collectAsState()
    val isConnectedToThisVoice = isVoice && activeVoiceId == cid && voiceConnState == VoiceConnectionState.CONNECTED

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = when {
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                isConnectedToThisVoice -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else -> Color.Transparent
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSelectTextChannel()
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isVoice) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Tag,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = channel.name ?: "channel",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (unread || isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (channel.nsfw == true) {
                    Text(
                        text = "18+",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (mentions > 0) {
                    Badge { Text(mentions.toString()) }
                }
            }
        }
    }
}

// ============================================================================
// Channel Screen (Messages, Age Gate, Geo Gate, GIFs, Emoji Picker, Members)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SharedChannelScreen(
    channelId: String,
    navigator: SharedNavigator,
    voiceController: VoiceCallController = remember { VoiceCallController() },
    onOpenVoiceFullscreen: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onOpenInvite: () -> Unit,
    onOpenUserProfile: (User) -> Unit,
    onOpenMessageContext: (Message) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val channel = SharedStoatClient.channelCache[channelId]
    val server = channel?.server?.let { SharedStoatClient.serverCache[it] }
    val messages = SharedStoatClient.channelMessages[channelId].orEmpty()
    val typingUsers = SharedStoatClient.typingUsersByChannel[channelId].orEmpty()

    var isLoadingMessages by remember(channelId) { mutableStateOf(false) }
    var loadError by remember(channelId) { mutableStateOf<String?>(null) }
    var composerText by remember(channelId) { mutableStateOf("") }
    var replyingToMessage by remember(channelId) { mutableStateOf<Message?>(null) }
    var editingMessage by remember(channelId) { mutableStateOf<Message?>(null) }
    var isSending by remember { mutableStateOf(false) }

    var showMembersSheet by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showGifPicker by remember { mutableStateOf(false) }

    LaunchedEffect(channelId) {
        isLoadingMessages = true
        loadError = null
        runCatching {
            SharedStoatClient.fetchChannelMessages(channelId)
            SharedStoatClient.ackChannel(channelId)
        }.onSuccess {
            isLoadingMessages = false
        }.onFailure { err ->
            isLoadingMessages = false
            loadError = err.message ?: "Failed to load messages"
        }
    }

    if (SharedStoatClient.isGeoRestricted) {
        SharedChannelGeoGateScreen(onBack = onBack ?: onOpenDrawer ?: {})
        return
    }

    if (channel?.nsfw == true && channelId !in SharedStoatClient.acknowledgedNsfwChannels) {
        SharedChannelAgeGateScreen(
            channelName = SharedStoatClient.resolveChannelTitle(channel),
            onConfirm = { SharedStoatClient.acknowledgeNsfwChannel(channelId) },
            onCancel = onBack ?: onOpenDrawer ?: {}
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (channel?.channelType == ChannelType.DirectMessage || channel?.channelType == ChannelType.Group) {
                                    Icons.Default.Forum
                                } else {
                                    Icons.Default.Tag
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = SharedStoatClient.resolveChannelTitle(channel),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        channel?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    val isDmOrGroup =
                        channel?.channelType == ChannelType.DirectMessage ||
                                channel?.channelType == ChannelType.Group
                    if (isDmOrGroup) {
                        val activeVoiceId by voiceController.activeChannelId.collectAsState()
                        val voiceConnState by voiceController.connectionState.collectAsState()
                        val isConnectedHere =
                            activeVoiceId == channelId && voiceConnState == VoiceConnectionState.CONNECTED
                        IconButton(
                            onClick = {
                                if (isConnectedHere) {
                                    onOpenVoiceFullscreen()
                                } else {
                                    scope.launch {
                                        val joinResp = runCatching { SharedStoatClient.joinVoiceCall(channelId) }
                                            .onFailure { err ->
                                                onStatusMessage(err.message ?: "Connecting to call...")
                                            }
                                            .getOrNull()
                                        voiceController.connect(
                                            channelId = channelId,
                                            channelName = SharedStoatClient.resolveChannelTitle(channel),
                                            livekitUrl = joinResp?.url.orEmpty(),
                                            token = joinResp?.token.orEmpty()
                                        )
                                        onOpenVoiceFullscreen()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("dm_header_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = if (isConnectedHere) "Open Active Call" else "Start Call",
                                tint = if (isConnectedHere) {
                                    Color(0xFF22C55E)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    IconButton(onClick = { navigator.navigate(SharedRoute.ChannelSearch(channelId)) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Channel")
                    }
                    IconButton(onClick = { navigator.navigate(SharedRoute.ChannelPins(channelId)) }) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pinned Messages")
                    }
                    if (server != null) {
                        IconButton(onClick = { showMembersSheet = true }) {
                            Icon(Icons.Default.People, contentDescription = "Server Members")
                        }
                    }
                    IconButton(onClick = { navigator.navigate(SharedRoute.ChannelSettingsHome(channelId)) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Channel Settings")
                    }
                }
            )
        }
    ) { padding ->
        val isVoiceChannel = channel?.channelType == ChannelType.VoiceChannel
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isVoiceChannel) {
                SharedJoinVoiceChannelBanner(
                    channelId = channelId,
                    channel = channel,
                    voiceController = voiceController,
                    onOpenVoiceFullscreen = onOpenVoiceFullscreen,
                    onStatusMessage = onStatusMessage,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            // Message Feed
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoadingMessages && messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (loadError != null && messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(loadError!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoadingMessages = true
                                    loadError = null
                                    runCatching { SharedStoatClient.fetchChannelMessages(channelId) }
                                        .onFailure { loadError = it.message }
                                    isLoadingMessages = false
                                }
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                } else if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Welcome to #${SharedStoatClient.resolveChannelTitle(channel)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "This is the start of the conversation. Send a message or GIF below!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(if (SharedStoatClient.compactChatMode) 4.dp else 10.dp)
                    ) {
                        if (messages.size >= 25) {
                            item(key = "load_older_btn") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextButton(
                                        onClick = {
                                            val oldestId = messages.firstOrNull()?.id ?: return@TextButton
                                            scope.launch {
                                                runCatching {
                                                    SharedStoatClient.fetchChannelMessages(channelId, before = oldestId)
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Load Earlier Messages")
                                    }
                                }
                            }
                        }

                        items(messages, key = { it.id ?: it.nonce ?: it.hashCode().toString() }) { msg ->
                            SharedMessageRowItem(
                                message = msg,
                                server = server,
                                allChannelMessages = messages,
                                onUserClick = onOpenUserProfile,
                                onLongPress = { onOpenMessageContext(msg) },
                                onReplyClick = { replyingToMessage = msg; editingMessage = null },
                                onToggleReaction = { emoji ->
                                    val mid = msg.id ?: return@SharedMessageRowItem
                                    val myId = SharedStoatClient.selfId
                                    val hasReacted = myId != null && msg.reactions.orEmpty()[emoji].orEmpty().contains(myId)
                                    scope.launch {
                                        runCatching {
                                            if (hasReacted) {
                                                SharedStoatClient.unreactToMessage(channelId, mid, emoji)
                                            } else {
                                                SharedStoatClient.reactToMessage(channelId, mid, emoji)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Typing Indicator
            if (typingUsers.isNotEmpty()) {
                val names = typingUsers.map { uid ->
                    SharedStoatClient.userCache[uid]?.let { User.resolveDefaultName(it) } ?: "Someone"
                }
                Text(
                    text = "${names.joinToString(", ")} ${if (names.size == 1) "is" else "are"} typing...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Reply or Edit Bar
            if (replyingToMessage != null || editingMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (editingMessage != null) {
                                "Editing message"
                            } else {
                                val rAuthor = replyingToMessage?.author?.let { SharedStoatClient.userCache[it] }
                                "Replying to ${rAuthor?.let { User.resolveDefaultName(it) } ?: "message"}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                replyingToMessage = null
                                editingMessage = null
                                composerText = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Emoji Picker Drawer
            if (showEmojiPicker) {
                SharedQuickEmojiPickerPanel(
                    serverId = server?.id,
                    onSelectEmoji = { emojiText ->
                        composerText += emojiText
                        showEmojiPicker = false
                    }
                )
            }

            // GIF Picker Drawer
            if (showGifPicker) {
                SharedGifPickerPanel(
                    onSendGifUrl = { gifUrl ->
                        showGifPicker = false
                        scope.launch {
                            runCatching {
                                SharedStoatClient.sendMessage(channelId = channelId, content = gifUrl)
                            }.onFailure {
                                onStatusMessage(it.message ?: "Failed to send GIF")
                            }
                        }
                    },
                    onClose = { showGifPicker = false }
                )
            }

            // Composer Bar (with default-enabled Voice Message recording)
            var isRecordingVoiceMessage by remember(channelId) { mutableStateOf(false) }
            var voiceRecordElapsedMs by remember(channelId) { mutableStateOf(0L) }
            val voiceRecordSamples = remember(channelId) { mutableStateListOf<Int>() }

            LaunchedEffect(isRecordingVoiceMessage) {
                if (isRecordingVoiceMessage) {
                    voiceRecordElapsedMs = 0L
                    voiceRecordSamples.clear()
                    while (isRecordingVoiceMessage) {
                        delay(120L)
                        voiceRecordElapsedMs += 120L
                        val nextAmp = Random.nextInt(48, 235)
                        if (voiceRecordSamples.size >= 64) {
                            voiceRecordSamples.removeAt(0)
                        }
                        voiceRecordSamples.add(nextAmp)
                    }
                }
            }

            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRecordingVoiceMessage) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isRecordingVoiceMessage = false
                                voiceRecordElapsedMs = 0L
                                voiceRecordSamples.clear()
                            },
                            modifier = Modifier.testTag("cancel_voice_message_button")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Cancel Voice Message",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )

                        Text(
                            text = formatVoiceDurationMs(voiceRecordElapsedMs),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val previewBars = if (voiceRecordSamples.isEmpty()) List(24) { 64 } else voiceRecordSamples.takeLast(24)
                            previewBars.forEach { sample ->
                                val fraction = (sample.coerceIn(24, 255) / 255f).coerceIn(0.15f, 1f)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(fraction)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        Button(
                            enabled = !isSending,
                            onClick = {
                                val recordedDuration = voiceRecordElapsedMs.coerceAtLeast(1000L)
                                val recordedWave = if (voiceRecordSamples.isEmpty()) {
                                    List(32) { Random.nextInt(60, 210) }
                                } else {
                                    voiceRecordSamples.toList()
                                }
                                val replyTarget = replyingToMessage
                                isRecordingVoiceMessage = false
                                voiceRecordElapsedMs = 0L
                                voiceRecordSamples.clear()
                                replyingToMessage = null

                                scope.launch {
                                    isSending = true
                                    runCatching {
                                        val replies = replyTarget?.id?.let {
                                            listOf(SharedSendMessageReply(id = it, mention = true))
                                        }.orEmpty()
                                        SharedStoatClient.sendVoiceMessage(
                                            channelId = channelId,
                                            durationMillis = recordedDuration,
                                            waveform = recordedWave,
                                            replies = replies
                                        )
                                    }.onFailure { err ->
                                        onStatusMessage(err.message ?: "Failed to send voice message")
                                    }
                                    isSending = false
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("send_voice_message_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Voice Message",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Send", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                showEmojiPicker = !showEmojiPicker
                                if (showEmojiPicker) showGifPicker = false
                            }
                        ) {
                            Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji Picker")
                        }
                        IconButton(
                            onClick = {
                                showGifPicker = !showGifPicker
                                if (showGifPicker) showEmojiPicker = false
                            }
                        ) {
                            Icon(Icons.Default.GifBox, contentDescription = "GIF Picker")
                        }
                        OutlinedTextField(
                            value = composerText,
                            onValueChange = {
                                composerText = it
                                if (it.isNotEmpty()) {
                                    SharedStoatRealtime.sendBeginTyping(channelId)
                                }
                            },
                            placeholder = { Text("Message #${SharedStoatClient.resolveChannelTitle(channel)}") },
                            maxLines = 4,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        )
                        if (composerText.isBlank() && editingMessage == null) {
                            IconButton(
                                enabled = !isSending,
                                onClick = {
                                    showEmojiPicker = false
                                    showGifPicker = false
                                    isRecordingVoiceMessage = true
                                },
                                modifier = Modifier.testTag("record_voice_message_button")
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Record Voice Message",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            IconButton(
                                enabled = !isSending && composerText.isNotBlank(),
                                onClick = {
                                    val textToSend = composerText.trim()
                                    if (textToSend.isBlank()) return@IconButton
                                    val editTarget = editingMessage
                                    val replyTarget = replyingToMessage
                                    composerText = ""
                                    editingMessage = null
                                    replyingToMessage = null
                                    SharedStoatRealtime.sendEndTyping(channelId)

                                    scope.launch {
                                        isSending = true
                                        runCatching {
                                            if (editTarget?.id != null) {
                                                SharedStoatClient.editMessage(channelId, editTarget.id!!, textToSend)
                                            } else {
                                                val replies = replyTarget?.id?.let {
                                                    listOf(SharedSendMessageReply(id = it, mention = true))
                                                }.orEmpty()
                                                SharedStoatClient.sendMessage(
                                                    channelId = channelId,
                                                    content = textToSend,
                                                    replies = replies
                                                )
                                            }
                                        }.onFailure { err ->
                                            onStatusMessage(err.message ?: "Failed to send message")
                                        }
                                        isSending = false
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (composerText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMembersSheet && server?.id != null) {
        val sid = server.id!!
        val memberMap = SharedStoatClient.memberCache[sid].orEmpty()
        ModalBottomSheet(onDismissRequest = { showMembersSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Members (${memberMap.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        showMembersSheet = false
                        onOpenInvite()
                    }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Invite")
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(memberMap.entries.toList(), key = { it.key }) { (uid, member) ->
                        val user = SharedStoatClient.userCache[uid] ?: User.getPlaceholder(uid)
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = member.nickname ?: User.resolveDefaultName(user),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            supportingContent = {
                                Text(user.status?.text ?: if (user.online == true) "Online" else "Offline")
                            },
                            leadingContent = {
                                SharedAvatarCircle(
                                    label = member.nickname ?: User.resolveDefaultName(user),
                                    seed = uid,
                                    size = 38.dp,
                                    online = user.online,
                                    presence = user.status?.presence
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showMembersSheet = false
                                    onOpenUserProfile(user)
                                }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun SharedMessageRowItem(
    message: Message,
    server: Server?,
    allChannelMessages: List<Message>,
    onUserClick: (User) -> Unit,
    onLongPress: () -> Unit,
    onReplyClick: () -> Unit,
    onToggleReaction: (String) -> Unit
) {
    val authorId = message.author ?: "system"
    val user = SharedStoatClient.userCache[authorId] ?: User(id = authorId, username = message.masquerade?.name ?: "User")
    val member = server?.id?.let { SharedStoatClient.memberCache[it]?.get(authorId) }
    val displayName = message.masquerade?.name ?: member?.nickname ?: User.resolveDefaultName(user)

    val topRoleColor = remember(server, member) {
        val roleId = member?.roles?.firstOrNull()
        val hex = roleId?.let { server?.roles?.get(it)?.colour }
        parseHexColorOrDefault(hex, Color.Unspecified)
    }

    val standaloneGifUrl = remember(message.content) {
        SharedGifService.extractStandaloneGifUrl(message.content)
    }
    val gifEmbedMedia = remember(message.embeds, standaloneGifUrl) {
        message.embeds.orEmpty().firstNotNullOfOrNull { SharedGifService.resolveGifMediaFromEmbed(it) }
            ?: standaloneGifUrl?.let { url ->
                SharedResolvedGifMedia(
                    mediaUrl = url,
                    fallbackImageUrl = url,
                    originalUrl = url,
                    isVideo = SharedGifService.isVideoMediaUrl(url)
                )
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress,
                onDoubleClick = {
                    if (SharedStoatClient.messageReplyStyle == "DoubleTap") {
                        onReplyClick()
                    }
                }
            )
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        // Reply reference preview
        if (!message.replies.isNullOrEmpty()) {
            val repliedId = message.replies!!.first()
            val repliedMsg = allChannelMessages.firstOrNull { it.id == repliedId }
            val repliedUser = repliedMsg?.author?.let { SharedStoatClient.userCache[it] }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 44.dp, bottom = 4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${repliedUser?.let { User.resolveDefaultName(it) } ?: "Reply"}: ${repliedMsg?.content ?: "..."}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SharedAvatarCircle(
                label = displayName,
                seed = authorId,
                size = if (SharedStoatClient.compactChatMode) 28.dp else 38.dp,
                online = user.online,
                presence = user.status?.presence,
                modifier = Modifier.clickable { onUserClick(user) }
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (topRoleColor != Color.Unspecified) topRoleColor else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { onUserClick(user) }
                    )
                    if (message.edited != null) {
                        Text(
                            text = "(edited)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (message.pinned == true) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val parsedVoiceMessage = remember(message.content, message.attachments) {
                    parseSharedVoiceMessageMetadata(message)
                }

                // Hide standalone GIF URL text or raw VoiceMessage metadata when rendered as rich media
                val shouldShowText = !message.content.isNullOrBlank() &&
                        standaloneGifUrl == null &&
                        parsedVoiceMessage == null
                if (shouldShowText) {
                    Text(
                        text = message.content!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Voice Message Player Card (always enabled by default)
                if (parsedVoiceMessage != null) {
                    SharedVoiceMessagePlayerCard(
                        voiceMessage = parsedVoiceMessage,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Borderless looping GIF/video embed
                if (gifEmbedMedia != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 320.dp)
                            .aspectRatio(gifEmbedMedia.aspectRatio)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (gifEmbedMedia.isVideo) {
                            PlatformLoopingVideoView(
                                url = gifEmbedMedia.mediaUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        PlatformInfo.openExternalUrl(gifEmbedMedia.originalUrl ?: gifEmbedMedia.mediaUrl)
                                    }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.GifBox,
                                    contentDescription = "GIF",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Animated GIF",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Non-voice Attachments
                message.attachments.orEmpty()
                    .filterNot { parsedVoiceMessage != null && it.id == parsedVoiceMessage.attachmentId }
                    .forEach { att ->
                        val attUrl = SharedStoatClient.resolveAutumnUrl(att, "attachments")
                        OutlinedCard(
                            onClick = { attUrl?.let { PlatformInfo.openExternalUrl(it) } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = att.filename ?: "Attachment",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = att.contentType ?: "File",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                // Non-GIF Embeds
                message.embeds.orEmpty()
                    .filterNot { SharedGifService.isGifEmbed(it) }
                    .forEach { embed ->
                        OutlinedCard(
                            onClick = { embed.url?.let { PlatformInfo.openExternalUrl(it) } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                embed.siteName?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                embed.title?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                embed.description?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                // Reactions
                if (!message.reactions.isNullOrEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        message.reactions!!.forEach { (emoji, userIds) ->
                            val myId = SharedStoatClient.selfId
                            val isSelected = myId != null && myId in userIds
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleReaction(emoji) },
                                label = { Text("$emoji ${userIds.size}") }
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onLongPress,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Message Actions",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SharedQuickEmojiPickerPanel(
    serverId: String?,
    onSelectEmoji: (String) -> Unit
) {
    val standardEmojis = listOf(
        "😀", "😂", "🥹", "😍", "🥳", "😎", "🤔", "😭",
        "🔥", "❤️", "✨", "🎉", "👍", "🙏", "💯", "🚀"
    )
    val customEmojis = remember(serverId, SharedStoatClient.emojiCache.size) {
        SharedStoatClient.emojiCache.values.filter {
            serverId == null || it.parent?.id == serverId
        }
    }

    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Emojis", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                standardEmojis.forEach { e ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onSelectEmoji(e) }
                    ) {
                        Text(e, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                customEmojis.forEach { ce ->
                    val code = ":${ce.id}:"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { onSelectEmoji(code) }
                    ) {
                        Text(
                            text = ":${ce.name ?: "emoji"}:",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedGifPickerPanel(
    onSendGifUrl: (String) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var provider by remember {
        mutableStateOf(if (SharedStoatClient.enableKlipyGifs) SharedGifProvider.GIFBOX else SharedGifProvider.GIFBOX)
    }
    var searchQuery by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<SharedGifCategory>>(emptyList()) }
    var gifs by remember { mutableStateOf<List<SharedGifItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadData(q: String) {
        scope.launch {
            isLoading = true
            runCatching {
                if (q.isBlank()) {
                    categories = SharedGifService.fetchCategories(provider)
                }
                gifs = SharedGifService.fetchGifs(provider, q.ifBlank { "trending" })
            }
            isLoading = false
        }
    }

    LaunchedEffect(provider) {
        loadData(searchQuery)
    }

    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = provider == SharedGifProvider.GIFBOX,
                    onClick = { provider = SharedGifProvider.GIFBOX },
                    label = { Text("Gifbox") }
                )
                if (SharedStoatClient.enableKlipyGifs) {
                    FilterChip(
                        selected = provider == SharedGifProvider.KLIPY,
                        onClick = { provider = SharedGifProvider.KLIPY },
                        label = { Text("KLIPY") }
                    )
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        loadData(it)
                    },
                    placeholder = { Text("Search GIFs...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close GIF Picker")
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(gifs, key = { it.id }) { gif ->
                        Card(
                            onClick = {
                                if (gif.provider == SharedGifProvider.KLIPY) {
                                    scope.launch { SharedGifService.registerKlipyShare(gif.slug, searchQuery) }
                                }
                                onSendGifUrl(gif.sendUrl)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = gif.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SharedChannelAgeGateScreen(
    channelName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Age-Restricted Channel (18+)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "#$channelName is marked as age-restricted (NSFW). Please confirm you are at least 18 years old to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Go Back")
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("I am 18+")
                    }
                }
            }
        }
    }
}

@Composable
fun SharedChannelGeoGateScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Unavailable in Your Region",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "This content is restricted in your current region due to local regulations.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
    }
}

// ============================================================================
// Friends Screen (with QR Code Generator & Live Camera Scanner)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedFriendsScreen(
    onOpenDrawer: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onOpenDm: (Channel) -> Unit,
    onOpenUserProfile: (User) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var addFriendInput by remember { mutableStateOf("") }
    var showMyQrDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    val allUsers = SharedStoatClient.userCache.values.toList()
    val onlineFriends = allUsers.filter { it.relationship == "Friend" && it.online == true }
    val allFriends = allUsers.filter { it.relationship == "Friend" }
    val pendingIncoming = allUsers.filter { it.relationship == "Incoming" }
    val pendingOutgoing = allUsers.filter { it.relationship == "Outgoing" }
    val blockedUsers = allUsers.filter { it.relationship == "Blocked" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showMyQrDialog = true }) {
                        Icon(Icons.Default.QrCode, contentDescription = "My QR Code")
                    }
                    IconButton(onClick = { showQrScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Friend QR")
                    }
                }
            )
        }
    ) { padding ->
        if (showQrScanner) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                PlatformQrScannerView(
                    modifier = Modifier.fillMaxSize(),
                    onQrCodeScanned = { rawCode ->
                        showQrScanner = false
                        val parsed = FriendQrParser.parse(rawCode)
                        if (parsed != null) {
                            selectedTab = 4
                            addFriendInput = parsed.usernameTag ?: parsed.identifier
                            onStatusMessage("Scanned QR: ${parsed.identifier}")
                        }
                    },
                    onClose = { showQrScanner = false }
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val tabs = listOf(
                "Online (${onlineFriends.size})",
                "All (${allFriends.size})",
                "Pending (${pendingIncoming.size + pendingOutgoing.size})",
                "Blocked (${blockedUsers.size})",
                "Add Friend"
            )
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title) }
                    )
                }
            }

            if (selectedTab == 4) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Friend by Username",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter their username#discriminator (e.g. Alice#0123) or scan their QR code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = addFriendInput,
                        onValueChange = { addFriendInput = it },
                        label = { Text("Username#0000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = addFriendInput.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    runCatching {
                                        SharedStoatClient.sendFriendRequest(addFriendInput)
                                    }.onSuccess {
                                        onStatusMessage("Friend request sent to $addFriendInput")
                                        addFriendInput = ""
                                    }.onFailure { err ->
                                        onStatusMessage(err.message ?: "Failed to send friend request")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send Request")
                        }
                        OutlinedButton(onClick = { showQrScanner = true }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scan QR")
                        }
                    }
                }
            } else {
                val displayedUsers = when (selectedTab) {
                    0 -> onlineFriends
                    1 -> allFriends
                    2 -> pendingIncoming + pendingOutgoing
                    3 -> blockedUsers
                    else -> emptyList()
                }

                if (displayedUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No users in this list yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(displayedUsers, key = { it.id ?: it.username.orEmpty() }) { u ->
                            val uid = u.id ?: return@items
                            Card(
                                onClick = { onOpenUserProfile(u) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SharedAvatarCircle(
                                        label = User.resolveDefaultName(u),
                                        seed = uid,
                                        size = 42.dp,
                                        online = u.online,
                                        presence = u.status?.presence
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = User.resolveDefaultName(u, withDiscriminator = true),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = u.status?.text ?: u.relationship ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    when (u.relationship) {
                                        "Incoming" -> {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        runCatching { SharedStoatClient.acceptFriendRequest(uid) }
                                                            .onSuccess { onStatusMessage("Accepted friend request") }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        runCatching { SharedStoatClient.removeOrDeclineFriend(uid) }
                                                            .onSuccess { onStatusMessage("Declined friend request") }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        "Friend" -> {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        runCatching { SharedStoatClient.openDirectMessage(uid) }
                                                            .onSuccess(onOpenDm)
                                                            .onFailure { onStatusMessage(it.message ?: "Failed to open DM") }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Forum, contentDescription = "Message")
                                            }
                                        }
                                        "Blocked" -> {
                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        runCatching { SharedStoatClient.unblockUser(uid) }
                                                            .onSuccess { onStatusMessage("Unblocked user") }
                                                    }
                                                }
                                            ) {
                                                Text("Unblock")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMyQrDialog) {
        val me = SharedStoatClient.currentUser
        val tag = if (me != null) "${me.username}#${me.discriminator ?: "0000"}" else ""
        val qrPayload = "{\"id\":\"${me?.id.orEmpty()}\",\"username\":\"${me?.username.orEmpty()}\",\"discriminator\":\"${me?.discriminator ?: "0000"}\"}"

        AlertDialog(
            onDismissRequest = { showMyQrDialog = false },
            title = { Text("My Friend QR Code") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SharedQrCodeCanvas(payload = qrPayload)
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PlatformInfo.copyToClipboard("Username", tag)
                        onStatusMessage("Copied $tag to clipboard")
                        showMyQrDialog = false
                    }
                ) {
                    Text("Copy Tag")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMyQrDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// ============================================================================
// Overview, CatchUp, CreateGroup, Discover, ChannelPins & ChannelSearch Screens
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedOverviewScreen(
    navigator: SharedNavigator,
    onOpenDrawer: (() -> Unit)? = null,
    onSelectServer: (String) -> Unit,
    onSelectChannel: (String) -> Unit,
    onOpenFriends: () -> Unit,
    onOpenCreateJoinServer: () -> Unit
) {
    val servers = SharedStoatClient.sortedServers()
    val dms = SharedStoatClient.dmAndGroupChannels()
    val selfUser = SharedStoatClient.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overview") },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navigator.navigate(SharedRoute.CatchUp) }) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Catch Up")
                    }
                    IconButton(onClick = { navigator.navigate(SharedRoute.SettingsRoot) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SharedAvatarCircle(
                            label = selfUser?.let { User.resolveDefaultName(it) } ?: "Me",
                            seed = selfUser?.id ?: "self",
                            size = 52.dp,
                            online = true
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome, ${selfUser?.let { User.resolveDefaultName(it) } ?: "there"}!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${servers.size} Servers • ${dms.size} Conversations",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onOpenFriends, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Friends")
                    }
                    OutlinedButton(onClick = { navigator.navigate(SharedRoute.Discover) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Discover")
                    }
                    OutlinedButton(onClick = onOpenCreateJoinServer, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Server")
                    }
                }
            }

            if (dms.isNotEmpty()) {
                item {
                    Text("Recent Direct Messages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(dms.take(6), key = { "dm_${it.id}" }) { dm ->
                    val cid = dm.id ?: return@items
                    Card(
                        onClick = { onSelectChannel(cid) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SharedAvatarCircle(
                                label = SharedStoatClient.resolveChannelTitle(dm),
                                seed = cid,
                                size = 38.dp
                            )
                            Text(
                                text = SharedStoatClient.resolveChannelTitle(dm),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Text("Your Servers (${servers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (servers.isEmpty()) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("You haven't joined any servers yet.")
                            Button(onClick = onOpenCreateJoinServer) {
                                Text("Create or Join a Server")
                            }
                        }
                    }
                }
            } else {
                items(servers, key = { "srv_${it.id}" }) { srv ->
                    val sid = srv.id ?: return@items
                    Card(
                        onClick = { onSelectServer(sid) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SharedAvatarCircle(
                                label = srv.name ?: "Server",
                                seed = sid,
                                size = 42.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = srv.name ?: "Server",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                srv.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCatchUpScreen(
    navigator: SharedNavigator,
    onOpenChannel: (String) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val unreadChannels = SharedStoatClient.channelCache.values.filter { ch ->
        val cid = ch.id ?: return@filter false
        SharedStoatClient.isChannelUnread(cid) || SharedStoatClient.channelMentionCount(cid) > 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catch Up") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadChannels.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    unreadChannels.forEach { ch ->
                                        ch.id?.let { SharedStoatClient.ackChannel(it, ch.lastMessageID ?: "") }
                                    }
                                    onStatusMessage("Marked all channels as read")
                                }
                            }
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mark All Read")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (unreadChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("You're all caught up! Zero unread channels or mentions.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(unreadChannels, key = { it.id ?: "" }) { ch ->
                    val cid = ch.id ?: return@items
                    val srvName = ch.server?.let { SharedStoatClient.serverCache[it]?.name }
                    val mentions = SharedStoatClient.channelMentionCount(cid)
                    Card(
                        onClick = { onOpenChannel(cid) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = SharedStoatClient.resolveChannelTitle(ch),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                srvName?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (mentions > 0) {
                                Badge { Text("$mentions mentions") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCreateGroupScreen(
    navigator: SharedNavigator,
    onGroupCreated: (Channel) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var groupName by remember { mutableStateOf("") }
    val selectedUserIds = remember { mutableStateListOf<String>() }
    var isCreating by remember { mutableStateOf(false) }

    val friends = SharedStoatClient.userCache.values.filter { it.relationship == "Friend" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Group DM") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Select Friends (${selectedUserIds.size}/9)",
                style = MaterialTheme.typography.labelLarge
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(friends, key = { it.id ?: "" }) { friend ->
                    val uid = friend.id ?: return@items
                    val checked = uid in selectedUserIds
                    ListItem(
                        headlineContent = { Text(User.resolveDefaultName(friend)) },
                        leadingContent = {
                            SharedAvatarCircle(label = User.resolveDefaultName(friend), seed = uid, size = 36.dp)
                        },
                        trailingContent = {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (checked) selectedUserIds.remove(uid)
                                    else if (selectedUserIds.size < 9) selectedUserIds.add(uid)
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checked) selectedUserIds.remove(uid)
                                else if (selectedUserIds.size < 9) selectedUserIds.add(uid)
                            }
                    )
                }
            }
            Button(
                enabled = !isCreating && groupName.isNotBlank() && selectedUserIds.isNotEmpty(),
                onClick = {
                    scope.launch {
                        isCreating = true
                        runCatching {
                            SharedStoatClient.createGroupDm(groupName, selectedUserIds.toList())
                        }.onSuccess { created ->
                            isCreating = false
                            onStatusMessage("Created group ${created.name}")
                            onGroupCreated(created)
                        }.onFailure { err ->
                            isCreating = false
                            onStatusMessage(err.message ?: "Failed to create group")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isCreating) "Creating..." else "Create Group")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedDiscoverScreen(
    navigator: SharedNavigator,
    onServerJoined: (String) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var inviteCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover Communities") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "DawnChat Community Discovery",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Join a server directly with an invite code or browse public communities on the DawnChat Discovery portal.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { PlatformInfo.openExternalUrl(DAWNCHAT_DISCOVERY) }
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Discovery Portal ($DAWNCHAT_DISCOVERY)")
                        }
                    }
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Join with Invite Code or Link",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = { inviteCode = it },
                            label = { Text("Invite Code or URL") },
                            placeholder = { Text("https://csinvite.net/dc/...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            enabled = !isJoining && inviteCode.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    isJoining = true
                                    runCatching {
                                        SharedStoatClient.joinInviteByCode(inviteCode)
                                    }.onSuccess { joined ->
                                        isJoining = false
                                        onStatusMessage("Joined ${joined.server?.name ?: "server"}")
                                        joined.server?.id?.let(onServerJoined)
                                    }.onFailure { err ->
                                        isJoining = false
                                        onStatusMessage(err.message ?: "Failed to join invite")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isJoining) "Joining..." else "Join Server")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedChannelPinsScreen(
    channelId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val pins = SharedStoatClient.channelPinnedMessages[channelId].orEmpty()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(channelId) {
        isLoading = true
        runCatching { SharedStoatClient.fetchPinnedMessages(channelId) }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pinned Messages") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && pins.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (pins.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No pinned messages in this channel.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pins, key = { it.id ?: "" }) { msg ->
                    val author = msg.author?.let { SharedStoatClient.userCache[it] }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = author?.let { User.resolveDefaultName(it) } ?: "User",
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        val mid = msg.id ?: return@TextButton
                                        scope.launch {
                                            runCatching { SharedStoatClient.unpinMessage(channelId, mid) }
                                                .onSuccess { onStatusMessage("Unpinned message") }
                                        }
                                    }
                                ) {
                                    Text("Unpin")
                                }
                            }
                            Text(msg.content ?: "(Attachment / Embed)")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedChannelSearchScreen(
    channelId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf("Latest") }
    var isSearching by remember { mutableStateOf(false) }
    val results = SharedStoatClient.channelSearchResults[channelId].orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Channel") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search messages...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = !isSearching && query.isNotBlank(),
                    onClick = {
                        scope.launch {
                            isSearching = true
                            runCatching {
                                SharedStoatClient.searchChannelMessages(channelId, query, sortOrder)
                            }.onFailure {
                                onStatusMessage(it.message ?: "Search failed")
                            }
                            isSearching = false
                        }
                    }
                ) {
                    Text(if (isSearching) "..." else "Search")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Latest", "Oldest", "Relevance").forEach { s ->
                    FilterChip(
                        selected = sortOrder == s,
                        onClick = { sortOrder = s },
                        label = { Text(s) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id ?: "" }) { msg ->
                    val author = msg.author?.let { SharedStoatClient.userCache[it] }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = author?.let { User.resolveDefaultName(it) } ?: "User",
                                fontWeight = FontWeight.Bold
                            )
                            Text(msg.content ?: "")
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Join Voice Channel Banner Card & Voice Message Player (Android & iOS Shared)
// ============================================================================

@Composable
fun SharedJoinVoiceChannelBanner(
    channelId: String,
    channel: Channel?,
    voiceController: VoiceCallController,
    onOpenVoiceFullscreen: () -> Unit,
    onStatusMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val activeVoiceId by voiceController.activeChannelId.collectAsState()
    val voiceConnState by voiceController.connectionState.collectAsState()
    val controllerParticipants by voiceController.participants.collectAsState()

    val isConnectedToThisVoice =
        activeVoiceId == channelId && voiceConnState == VoiceConnectionState.CONNECTED
    val voiceStateParticipants = SharedStoatClient.voiceStateCache[channelId]?.participants.orEmpty()
    val participantCount = if (isConnectedToThisVoice) {
        controllerParticipants.size.coerceAtLeast(1)
    } else {
        voiceStateParticipants.size
    }
    val hasParticipants = participantCount > 0

    val joinOrOpenCall = {
        val cname = SharedStoatClient.resolveChannelTitle(channel)
        if (isConnectedToThisVoice) {
            onOpenVoiceFullscreen()
        } else {
            scope.launch {
                val joinResp = runCatching { SharedStoatClient.joinVoiceCall(channelId) }
                    .onFailure { err ->
                        onStatusMessage(err.message ?: "Connecting to voice channel...")
                    }
                    .getOrNull()
                voiceController.connect(
                    channelId = channelId,
                    channelName = cname,
                    livekitUrl = joinResp?.url.orEmpty(),
                    token = joinResp?.token.orEmpty()
                )
                onOpenVoiceFullscreen()
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasParticipants || isConnectedToThisVoice) {
                Color(0xFF22C55E).copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { joinOrOpenCall() }
            .testTag("join_voice_channel_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (hasParticipants || isConnectedToThisVoice) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF22C55E)
                                )
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isConnectedToThisVoice) "Connected to Voice" else "Join Voice Call",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )
                Text(
                    text = when {
                        participantCount == 0 -> "No one is currently in voice"
                        participantCount == 1 -> "1 participant in voice"
                        else -> "$participantCount participants in voice"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { joinOrOpenCall() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("join_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Join Call",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isConnectedToThisVoice) "Open Voice Call" else "Join Call",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

internal data class SharedParsedVoiceMessage(
    val durationMillis: Long,
    val waveform: List<Int>,
    val audioUrl: String?,
    val attachmentId: String?
)

internal fun parseSharedVoiceMessageMetadata(message: Message): SharedParsedVoiceMessage? {
    val content = message.content?.trim()
    val audioAttachment = message.attachments.orEmpty().firstOrNull { att ->
        val ct = att.contentType?.lowercase().orEmpty()
        val fn = att.filename?.lowercase().orEmpty()
        ct.startsWith("audio/") ||
                fn.endsWith(".wav") ||
                fn.endsWith(".ogg") ||
                fn.endsWith(".mp3") ||
                fn.endsWith(".m4a") ||
                fn.endsWith(".opus") ||
                fn.endsWith(".aac")
    } ?: message.attachments?.firstOrNull()

    val resolvedUrl = audioAttachment?.let { SharedStoatClient.resolveAutumnUrl(it, "attachments") }

    if (!content.isNullOrBlank()) {
        // 1. Stoat JSON format: {"x":"chat.stoat.VoiceMessage","y":1,"z":durationMillis,"w":[...]}
        if (content.startsWith("{") && content.contains("chat.stoat.VoiceMessage")) {
            val jsonObj = runCatching {
                SharedJson.parseToJsonElement(content) as? JsonObject
            }.getOrNull()
            if (jsonObj != null) {
                val id = (jsonObj["x"] as? JsonPrimitive)?.content
                if (id == "chat.stoat.VoiceMessage") {
                    val durationMs = (jsonObj["z"] as? JsonPrimitive)?.longOrNull?.coerceAtLeast(1000L) ?: 3000L
                    val waveArr = jsonObj["w"] as? JsonArray
                    val samples = waveArr?.mapNotNull { (it as? JsonPrimitive)?.intOrNull?.coerceIn(0, 255) }
                        ?.takeIf { it.isNotEmpty() }
                        ?: List(32) { 120 }
                    return SharedParsedVoiceMessage(
                        durationMillis = durationMs,
                        waveform = samples,
                        audioUrl = resolvedUrl,
                        attachmentId = audioAttachment?.id
                    )
                }
            }
        }

        // 2. Prefix format: ^:2:<durationSeconds>:<hexWaveform>
        if (content.startsWith("^:2:")) {
            val parts = content.removePrefix("^:2:").split(":")
            val durationSec = parts.getOrNull(0)?.toDoubleOrNull() ?: 3.0
            val hexWave = parts.getOrNull(1).orEmpty()
            val samples = if (hexWave.length >= 2) {
                hexWave.chunked(2).mapNotNull { it.toIntOrNull(16)?.coerceIn(0, 255) }
            } else {
                emptyList()
            }
            return SharedParsedVoiceMessage(
                durationMillis = (durationSec * 1000.0).toLong().coerceAtLeast(1000L),
                waveform = if (samples.isEmpty()) List(32) { 120 } else samples,
                audioUrl = resolvedUrl,
                attachmentId = audioAttachment?.id
            )
        }
    }

    // 3. Standalone voice-message audio attachment
    if (audioAttachment != null &&
        (audioAttachment.filename?.contains("voice", ignoreCase = true) == true ||
                audioAttachment.contentType?.startsWith("audio/", ignoreCase = true) == true) &&
        content.isNullOrBlank()
    ) {
        return SharedParsedVoiceMessage(
            durationMillis = 4000L,
            waveform = List(32) { idx -> 70 + ((idx * 37) % 140) },
            audioUrl = resolvedUrl,
            attachmentId = audioAttachment.id
        )
    }

    return null
}

internal fun formatVoiceDurationMs(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
internal fun SharedVoiceMessagePlayerCard(
    voiceMessage: SharedParsedVoiceMessage,
    modifier: Modifier = Modifier
) {
    val mediaPlayer = remember { PlatformAudioPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var progressFraction by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { mediaPlayer.stop() }
        }
    }

    LaunchedEffect(isPlaying, voiceMessage.durationMillis) {
        if (isPlaying) {
            val totalMs = voiceMessage.durationMillis.coerceAtLeast(1000L)
            val stepMs = 80L
            var elapsed = (progressFraction * totalMs).toLong()
            while (isPlaying && elapsed < totalMs) {
                delay(stepMs)
                elapsed += stepMs
                progressFraction = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
            }
            if (elapsed >= totalMs) {
                isPlaying = false
                progressFraction = 0f
                runCatching { mediaPlayer.stop() }
            }
        }
    }

    val displayBars = remember(voiceMessage.waveform) {
        val src = if (voiceMessage.waveform.isEmpty()) List(32) { 120 } else voiceMessage.waveform
        List(32) { idx ->
            val mappedIdx = ((idx.toFloat() / 32f) * src.size).toInt().coerceIn(0, src.lastIndex)
            src[mappedIdx].coerceIn(28, 255)
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
            .widthIn(max = 320.dp)
            .testTag("voice_message_player")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        isPlaying = false
                        runCatching { mediaPlayer.pause() }
                    } else {
                        if (progressFraction >= 0.98f) progressFraction = 0f
                        isPlaying = true
                        voiceMessage.audioUrl?.let { url ->
                            runCatching { mediaPlayer.play(url) }
                        }
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Voice Message" else "Play Voice Message",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                displayBars.forEachIndexed { index, amp ->
                    val barProgress = index.toFloat() / displayBars.size.toFloat()
                    val isPlayed = barProgress <= progressFraction
                    val heightFraction = (amp / 255f).coerceIn(0.18f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(heightFraction)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isPlayed) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                }
                            )
                    )
                }
            }

            val displayedMs = if (isPlaying || progressFraction > 0f) {
                (progressFraction * voiceMessage.durationMillis).toLong()
            } else {
                voiceMessage.durationMillis
            }
            Text(
                text = formatVoiceDurationMs(displayedMs),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

