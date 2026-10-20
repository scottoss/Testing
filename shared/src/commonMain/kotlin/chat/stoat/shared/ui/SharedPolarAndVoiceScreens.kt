package chat.stoat.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.User
import chat.stoat.shared.network.SharedStoatClient
import chat.stoat.shared.network.SharedStoatRealtime
import chat.stoat.shared.platform.VoiceCallController
import chat.stoat.shared.platform.VoiceConnectionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedPolarMainScreen(
    navigator: SharedNavigator,
    onOpenCreateJoinServer: () -> Unit,
    onOpenUserProfile: (User, String?) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        SharedStoatRealtime.ensureConnected()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Groups, contentDescription = "Communities") },
                    label = { Text("Communities") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Forum, contentDescription = "Conversations") },
                    label = { Text("DMs") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.People, contentDescription = "Friends") },
                    label = { Text("Friends") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Overview") },
                    label = { Text("Overview") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> SharedPolarCommunitiesTab(
                    navigator = navigator,
                    onOpenCreateJoinServer = onOpenCreateJoinServer
                )
                1 -> SharedPolarConversationsTab(navigator = navigator)
                2 -> SharedFriendsScreen(
                    onOpenDm = { dm ->
                        dm.id?.let { navigator.navigate(SharedRoute.PolarConversation(it)) }
                    },
                    onOpenUserProfile = { u -> onOpenUserProfile(u, null) },
                    onStatusMessage = onStatusMessage
                )
                3 -> SharedOverviewScreen(
                    navigator = navigator,
                    onSelectServer = { sid -> navigator.navigate(SharedRoute.PolarServerChannels(sid)) },
                    onSelectChannel = { cid -> navigator.navigate(SharedRoute.PolarConversation(cid)) },
                    onOpenFriends = { selectedTab = 2 },
                    onOpenCreateJoinServer = onOpenCreateJoinServer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharedPolarCommunitiesTab(
    navigator: SharedNavigator,
    onOpenCreateJoinServer: () -> Unit
) {
    val servers = SharedStoatClient.sortedServers()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Communities (Polar)") },
                actions = {
                    IconButton(onClick = { navigator.navigate(SharedRoute.Discover) }) {
                        Icon(Icons.Default.Explore, contentDescription = "Discover")
                    }
                    IconButton(onClick = { navigator.navigate(SharedRoute.SettingsRoot) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenCreateJoinServer) {
                Icon(Icons.Default.Add, contentDescription = "Create or Join Server")
            }
        }
    ) { padding ->
        if (servers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("No communities joined yet.")
                    Button(onClick = onOpenCreateJoinServer) {
                        Text("Create or Join a Server")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(servers, key = { it.id ?: "" }) { server ->
                    val sid = server.id ?: return@items
                    val unread = SharedStoatClient.isServerUnread(sid)
                    val mentions = SharedStoatClient.serverMentionCount(sid)
                    Card(
                        onClick = { navigator.navigate(SharedRoute.PolarServerChannels(sid)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SharedAvatarCircle(
                                label = server.name ?: "Server",
                                seed = sid,
                                size = 48.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = server.name ?: "Server",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (unread) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                                server.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (mentions > 0) {
                                Badge { Text(mentions.toString()) }
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
private fun SharedPolarConversationsTab(
    navigator: SharedNavigator
) {
    val dms = SharedStoatClient.dmAndGroupChannels()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Direct Messages") },
                actions = {
                    IconButton(onClick = { navigator.navigate(SharedRoute.CreateGroup) }) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Create Group DM")
                    }
                    IconButton(onClick = { navigator.navigate(SharedRoute.SettingsRoot) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (dms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No direct message conversations yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dms, key = { it.id ?: "" }) { dm ->
                    val cid = dm.id ?: return@items
                    val title = SharedStoatClient.resolveChannelTitle(dm)
                    val unread = SharedStoatClient.isChannelUnread(cid)
                    val mentions = SharedStoatClient.channelMentionCount(cid)

                    Card(
                        onClick = { navigator.navigate(SharedRoute.PolarConversation(cid)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SharedAvatarCircle(
                                label = title,
                                seed = cid,
                                size = 44.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = dm.channelType?.value ?: "DirectMessage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (mentions > 0) {
                                Badge { Text(mentions.toString()) }
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
fun SharedPolarServerChannelsScreen(
    serverId: String,
    navigator: SharedNavigator,
    voiceController: VoiceCallController,
    onOpenVoiceFullscreen: () -> Unit,
    onOpenServerContextSheet: (Server) -> Unit
) {
    val scope = rememberCoroutineScope()
    val server = SharedStoatClient.serverCache[serverId]

    LaunchedEffect(serverId) {
        if (SharedStoatClient.memberCache[serverId].isNullOrEmpty()) {
            runCatching { SharedStoatClient.fetchServerMembers(serverId) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(server?.name ?: "Server") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (server != null) {
                        IconButton(onClick = { onOpenServerContextSheet(server) }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Server Menu")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (server == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Server not found")
            }
        } else {
            val allChannels = server.channels.orEmpty().mapNotNull { SharedStoatClient.channelCache[it] }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allChannels, key = { it.id ?: "" }) { ch ->
                    val cid = ch.id ?: return@items
                    val isVoice = ch.channelType == ChannelType.VoiceChannel || ch.voice != null
                    Card(
                        onClick = {
                            navigator.navigate(SharedRoute.PolarConversation(cid))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVoice) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = ch.name ?: "channel",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Fullscreen Voice Call Sheet & Minimized Floating PiP
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedVoiceFullscreenOverlay(
    voiceController: VoiceCallController,
    onMinimize: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val connState by voiceController.connectionState.collectAsState()
    val activeChannelId by voiceController.activeChannelId.collectAsState()
    val channelName by voiceController.activeChannelName.collectAsState()
    val isMuted by voiceController.isMuted.collectAsState()
    val isDeafened by voiceController.isDeafened.collectAsState()
    val participants by voiceController.participants.collectAsState()
    val statusMsg by voiceController.statusMessage.collectAsState()

    Surface(
        color = Color(0xFF111214),
        contentColor = Color.White,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onMinimize) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize to PiP",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = channelName ?: "Voice Channel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = statusMsg ?: connState.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF23A55A)
                    )
                }
                Spacer(Modifier.size(48.dp))
            }

            // Participant Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 20.dp)
            ) {
                items(participants, key = { it.userId }) { p ->
                    val speakingBorder = if (p.isSpeaking && !p.isMuted) Color(0xFF23A55A) else Color.Transparent
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E1F22),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .border(2.dp, speakingBorder, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            SharedAvatarCircle(
                                label = p.displayName,
                                seed = p.userId,
                                size = 64.dp
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = p.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (p.isMuted) {
                                    Icon(
                                        Icons.Default.MicOff,
                                        contentDescription = "Muted",
                                        tint = Color(0xFFF23F43),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Control Bar
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF2B2D31),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (connState == VoiceConnectionState.DISCONNECTED) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val cid = activeChannelId ?: return@Button
                                val cname = channelName ?: "Voice Channel"
                                scope.launch {
                                    val joinResp = runCatching { SharedStoatClient.joinVoiceCall(cid) }.getOrNull()
                                    voiceController.connect(
                                        channelId = cid,
                                        channelName = cname,
                                        livekitUrl = joinResp?.url.orEmpty(),
                                        token = joinResp?.token.orEmpty()
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF22C55E),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Join Call",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Join Call",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { voiceController.toggleMute() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color(0xFFF23F43) else Color(0xFF383A40))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Toggle Mute",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { voiceController.toggleDeafen() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isDeafened) Color(0xFFF23F43) else Color(0xFF383A40))
                        ) {
                            Icon(
                                imageVector = if (isDeafened) Icons.Default.HeadsetOff else Icons.Default.Headset,
                                contentDescription = "Toggle Deafen",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                voiceController.disconnect()
                                onMinimize()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDA373C))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Disconnect",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SharedVoiceFloatingPip(
    voiceController: VoiceCallController,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connState by voiceController.connectionState.collectAsState()
    val channelName by voiceController.activeChannelName.collectAsState()
    val isMuted by voiceController.isMuted.collectAsState()

    if (connState == VoiceConnectionState.DISCONNECTED) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .padding(16.dp)
            .clickable(onClick = onExpand)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Color(0xFF23A55A),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = channelName ?: "Voice Call",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Tap to return to call",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB5BAC1)
                )
            }
            IconButton(
                onClick = { voiceController.toggleMute() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = if (isMuted) Color(0xFFF23F43) else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = { voiceController.disconnect() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Hang Up",
                    tint = Color(0xFFDA373C),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
