package chat.stoat.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.stoat.core.model.data.DAWNCHAT_DISCOVERY
import chat.stoat.shared.gifs.SharedGifCategory
import chat.stoat.shared.gifs.SharedGifItem
import chat.stoat.shared.gifs.SharedGifProvider
import chat.stoat.shared.gifs.SharedGifService
import chat.stoat.shared.platform.FriendQrParser
import chat.stoat.shared.platform.PlatformInfo
import chat.stoat.shared.platform.PlatformLoopingVideoView
import chat.stoat.shared.platform.PlatformQrScannerView
import chat.stoat.shared.platform.VoiceCallController
import chat.stoat.shared.platform.VoiceConnectionState
import chat.stoat.shared.platform.rememberVoiceCallController
import chat.stoat.shared.ui.theme.SharedDawnChatTheme
import kotlinx.coroutines.launch

enum class SharedNavTab(val label: String) {
    CHAT("Servers"),
    FRIENDS("Friends"),
    DISCOVER("Discover"),
    SETTINGS("Settings")
}

data class SharedServerModel(
    val id: String,
    val name: String,
    val badge: String,
    val channels: List<SharedChannelModel>
)

data class SharedChannelModel(
    val id: String,
    val name: String,
    val isVoice: Boolean = false
)

data class SharedChatMessage(
    val id: String,
    val author: String,
    val content: String,
    val timestamp: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DawnChatMultiplatformApp() {
    SharedDawnChatTheme {
        val scope = rememberCoroutineScope()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val voiceController = rememberVoiceCallController()

        val servers = remember {
            listOf(
                SharedServerModel(
                    id = "dawn_official",
                    name = "DawnChat Lounge",
                    badge = "DC",
                    channels = listOf(
                        SharedChannelModel("general", "general", isVoice = false),
                        SharedChannelModel("memes_and_gifs", "memes-and-gifs", isVoice = false),
                        SharedChannelModel("voice_lobby", "General Voice", isVoice = true)
                    )
                ),
                SharedServerModel(
                    id = "kmp_devs",
                    name = "Multiplatform Hub",
                    badge = "KM",
                    channels = listOf(
                        SharedChannelModel("ios_and_android", "ios-and-android", isVoice = false),
                        SharedChannelModel("releases", "releases", isVoice = false),
                        SharedChannelModel("voice_standup", "Daily Standup", isVoice = true)
                    )
                )
            )
        }

        var selectedTab by remember { mutableStateOf(SharedNavTab.CHAT) }
        var selectedServer by remember { mutableStateOf(servers.first()) }
        var selectedTextChannel by remember { mutableStateOf(servers.first().channels.first()) }
        var selectedVoiceChannel by remember { mutableStateOf<SharedChannelModel?>(null) }
        var showFullscreenVoice by remember { mutableStateOf(false) }

        val messages = remember {
            mutableStateListOf(
                SharedChatMessage(
                    id = "1",
                    author = "DawnBot",
                    content = "Welcome to DawnChat ${PlatformInfo.appVersion} running on ${PlatformInfo.platformName}!",
                    timestamp = "12:00"
                ),
                SharedChatMessage(
                    id = "2",
                    author = "Alex",
                    content = "https://gifbox.me/view/welcome-wave-gif",
                    timestamp = "12:01"
                )
            )
        }

        if (showFullscreenVoice) {
            SharedFullscreenVoiceScreen(
                voiceController = voiceController,
                onLeaveCall = {
                    voiceController.disconnect()
                    showFullscreenVoice = false
                },
                onMinimize = {
                    showFullscreenVoice = false
                }
            )
            return@SharedDawnChatTheme
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(310.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Server rail inside drawer: tapping a server keeps drawer open to browse channels first
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(72.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            servers.forEach { server ->
                                val isSelected = server.id == selectedServer.id
                                Surface(
                                    shape = if (isSelected) RoundedCornerShape(16.dp) else CircleShape,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable {
                                            // Keep drawer open when switching server so user can browse channels first
                                            selectedServer = server
                                        }
                                        .testTag("server_icon_${server.id}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = server.badge,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Channel list for selectedServer
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = selectedServer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            selectedServer.channels.forEach { channel ->
                                val isCurrent = if (channel.isVoice) {
                                    selectedVoiceChannel?.id == channel.id
                                } else {
                                    selectedTextChannel.id == channel.id
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable {
                                            if (channel.isVoice) {
                                                selectedVoiceChannel = channel
                                            } else {
                                                selectedTextChannel = channel
                                                selectedTab = SharedNavTab.CHAT
                                                scope.launch { drawerState.close() }
                                            }
                                        }
                                        .testTag("channel_item_${channel.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (channel.isVoice) {
                                                Icons.AutoMirrored.Filled.VolumeUp
                                            } else {
                                                Icons.Default.Tag
                                            },
                                            contentDescription = channel.name,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = channel.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (channel.isVoice && selectedVoiceChannel?.id == channel.id) {
                                    Button(
                                        onClick = {
                                            voiceController.connect(
                                                channelId = channel.id,
                                                channelName = channel.name
                                            )
                                            showFullscreenVoice = true
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp, bottom = 8.dp)
                                            .testTag("join_voice_button_${channel.id}")
                                    ) {
                                        Text("Join Voice")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.safeDrawingPadding(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (selectedTab) {
                                    SharedNavTab.CHAT -> "# ${selectedTextChannel.name}"
                                    SharedNavTab.FRIENDS -> "Friends & QR"
                                    SharedNavTab.DISCOVER -> "Discover"
                                    SharedNavTab.SETTINGS -> "Settings"
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("open_drawer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Server & Channel Drawer"
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == SharedNavTab.CHAT,
                            onClick = { selectedTab = SharedNavTab.CHAT },
                            icon = { Icon(Icons.Default.Tag, contentDescription = "Servers") },
                            label = { Text("Chat") },
                            modifier = Modifier.testTag("nav_chat")
                        )
                        NavigationBarItem(
                            selected = selectedTab == SharedNavTab.FRIENDS,
                            onClick = { selectedTab = SharedNavTab.FRIENDS },
                            icon = { Icon(Icons.Default.Group, contentDescription = "Friends") },
                            label = { Text("Friends") },
                            modifier = Modifier.testTag("nav_friends")
                        )
                        NavigationBarItem(
                            selected = selectedTab == SharedNavTab.DISCOVER,
                            onClick = { selectedTab = SharedNavTab.DISCOVER },
                            icon = { Icon(Icons.Default.Explore, contentDescription = "Discover") },
                            label = { Text("Discover") },
                            modifier = Modifier.testTag("nav_discover")
                        )
                        NavigationBarItem(
                            selected = selectedTab == SharedNavTab.SETTINGS,
                            onClick = { selectedTab = SharedNavTab.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        SharedNavTab.CHAT -> SharedChatView(
                            messages = messages,
                            onSendMessage = { text ->
                                if (text.isNotBlank()) {
                                    messages.add(
                                        SharedChatMessage(
                                            id = (messages.size + 1).toString(),
                                            author = "You",
                                            content = text.trim(),
                                            timestamp = "Now"
                                        )
                                    )
                                }
                            }
                        )
                        SharedNavTab.FRIENDS -> SharedFriendsView()
                        SharedNavTab.DISCOVER -> SharedDiscoverView()
                        SharedNavTab.SETTINGS -> SharedSettingsView()
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedChatView(
    messages: List<SharedChatMessage>,
    onSendMessage: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var showPickerPane by remember { mutableStateOf(false) }
    var selectedPickerTab by remember { mutableIntStateOf(0) } // 0 = Emoji, 1 = GIFs

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val standaloneGifUrl = SharedGifService.extractStandaloneGifUrl(msg.content)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_message_${msg.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = msg.author,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    // Hide standalone GIF link text and render borderless GIF embed
                    if (standaloneGifUrl != null) {
                        PlatformLoopingVideoView(
                            url = standaloneGifUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("borderless_gif_embed_${msg.id}")
                        )
                    } else {
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showPickerPane = !showPickerPane },
                modifier = Modifier.testTag("toggle_picker_pane_button")
            ) {
                Icon(
                    imageVector = if (selectedPickerTab == 1 && showPickerPane) {
                        Icons.Default.Gif
                    } else {
                        Icons.Default.EmojiEmotions
                    },
                    contentDescription = "Open Emoji & GIF Picker"
                )
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message channel...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("message_input_field")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSendMessage(draft)
                        draft = ""
                    }
                },
                modifier = Modifier.testTag("send_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedVisibility(visible = showPickerPane) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                TabRow(selectedTabIndex = selectedPickerTab) {
                    Tab(
                        selected = selectedPickerTab == 0,
                        onClick = { selectedPickerTab = 0 },
                        text = { Text("Emoji") },
                        modifier = Modifier.testTag("picker_tab_emoji")
                    )
                    Tab(
                        selected = selectedPickerTab == 1,
                        onClick = { selectedPickerTab = 1 },
                        text = { Text("GIFs") },
                        modifier = Modifier.testTag("picker_tab_gifs")
                    )
                }

                if (selectedPickerTab == 0) {
                    val emojis = remember {
                        listOf("😀", "😂", "🔥", "🎉", "❤️", "👍", "✨", "🚀", "👋", "😎", "🥳", "💯")
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(56.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(emojis) { emoji ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { draft += emoji }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                } else {
                    SharedGifPickerPane(
                        onGifSelected = { gifUrl ->
                            onSendMessage(gifUrl)
                            showPickerPane = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedGifPickerPane(
    onGifSelected: (String) -> Unit
) {
    var provider by remember { mutableStateOf(SharedGifProvider.GIFBOX) }
    var searchQuery by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<SharedGifCategory>>(emptyList()) }
    var gifs by remember { mutableStateOf<List<SharedGifItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(provider, searchQuery) {
        isLoading = true
        runCatching {
            if (searchQuery.isBlank()) {
                categories = SharedGifService.fetchCategories(provider)
            } else {
                gifs = SharedGifService.fetchGifs(provider, searchQuery)
            }
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SharedGifProvider.entries.forEach { entry ->
                FilterChip(
                    selected = provider == entry,
                    onClick = { provider = entry },
                    label = { Text(entry.displayName) },
                    modifier = Modifier.testTag("shared_gif_provider_${entry.id}")
                )
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search ${provider.displayName}...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("shared_gif_search_input")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        } else if (searchQuery.isBlank()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable { searchQuery = category.query }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(gifs, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clickable { onGifSelected(item.sendUrl) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall,
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

@Composable
private fun SharedFullscreenVoiceScreen(
    voiceController: VoiceCallController,
    onLeaveCall: () -> Unit,
    onMinimize: () -> Unit
) {
    val channelName by voiceController.activeChannelName.collectAsState()
    val connectionState by voiceController.connectionState.collectAsState()
    val isMuted by voiceController.isMuted.collectAsState()
    val isDeafened by voiceController.isDeafened.collectAsState()
    val participants by voiceController.participants.collectAsState()
    val statusMessage by voiceController.statusMessage.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("fullscreen_voice_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = channelName ?: "Voice Channel",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = connectionState.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                statusMessage?.let { status ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                participants.forEach { participant ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = participant.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (participant.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (participant.isMuted) "Muted" else "Unmuted"
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onMinimize,
                    modifier = Modifier.testTag("voice_minimize_button")
                ) {
                    Text("Minimize")
                }
                IconButton(
                    onClick = { voiceController.toggleMute() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("voice_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle Mute"
                    )
                }
                IconButton(
                    onClick = { voiceController.toggleDeafen() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("voice_deafen_button")
                ) {
                    Icon(
                        imageVector = if (isDeafened) Icons.Default.HeadsetOff else Icons.Default.Headset,
                        contentDescription = "Toggle Deafen"
                    )
                }
                Button(
                    onClick = onLeaveCall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("voice_leave_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Leave Voice Call"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun SharedFriendsView() {
    var showQrScanner by remember { mutableStateOf(false) }
    var lastScannedFriend by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add Friend via QR",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showQrScanner = !showQrScanner },
                modifier = Modifier.testTag("open_qr_scanner_button")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Friend QR")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (showQrScanner) "Hide Scanner" else "Scan QR Code")
            }
        }

        if (showQrScanner) {
            PlatformQrScannerView(
                onQrCodeScanned = { rawCode ->
                    val parsed = FriendQrParser.parse(rawCode)
                    lastScannedFriend = parsed?.identifier ?: rawCode
                    showQrScanner = false
                },
                onClose = { showQrScanner = false }
            )
        }

        lastScannedFriend?.let { friendId ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "Parsed Friend Request Target: $friendId",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SharedDiscoverView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .testTag("centered_discover_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Discover Communities",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Discover DawnChat Communities",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Browse public communities and servers centered across all screen sizes.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { PlatformInfo.openExternalUrl(DAWNCHAT_DISCOVERY) },
                    modifier = Modifier.testTag("open_discover_url_button")
                ) {
                    Text("Open Discover Portal")
                }
            }
        }
    }
}

@Composable
private fun SharedSettingsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "DawnChat Multiplatform",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version: ${PlatformInfo.appVersion}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Platform: ${PlatformInfo.platformName} (${PlatformInfo.osVersion})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
