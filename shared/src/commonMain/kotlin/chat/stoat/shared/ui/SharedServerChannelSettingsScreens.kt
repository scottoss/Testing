package chat.stoat.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.stoat.core.model.data.DAWNCHAT_INVITE_SHORT
import chat.stoat.core.model.data.STOAT_MARKETING
import chat.stoat.core.model.schemas.User
import chat.stoat.shared.network.SharedStoatClient
import chat.stoat.shared.platform.PlatformInfo
import kotlinx.coroutines.launch

// ============================================================================
// 1. Channel Settings Screens
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedChannelSettingsHomeScreen(
    channelId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val channel = SharedStoatClient.channelCache[channelId]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channel Settings — #${SharedStoatClient.resolveChannelTitle(channel)}") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Overview", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Name, topic description, and age-restriction (NSFW)") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigator.navigate(SharedRoute.ChannelSettingsOverview(channelId))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Permissions", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Default and per-role channel permission overrides") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigator.navigate(SharedRoute.ChannelSettingsPermissions(channelId))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Webhooks", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Create and manage automated channel webhooks") },
                    leadingContent = { Icon(Icons.Default.Webhook, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigator.navigate(SharedRoute.ChannelSettingsWebhooks(channelId))
                    }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        scope.launch {
                            runCatching { SharedStoatClient.deleteOrLeaveChannel(channelId) }
                                .onSuccess {
                                    onStatusMessage("Channel deleted / left")
                                    navigator.pop()
                                }
                                .onFailure { onStatusMessage(it.message ?: "Failed to delete channel") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete / Close Channel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedChannelSettingsOverviewScreen(
    channelId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val channel = SharedStoatClient.channelCache[channelId]
    var name by remember(channel) { mutableStateOf(channel?.name.orEmpty()) }
    var description by remember(channel) { mutableStateOf(channel?.description.orEmpty()) }
    var nsfw by remember(channel) { mutableStateOf(channel?.nsfw == true) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channel Overview") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Channel Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Channel Topic / Description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Age-Restricted Channel (NSFW)")
                Switch(checked = nsfw, onCheckedChange = { nsfw = it })
            }
            Button(
                enabled = !isSaving && name.isNotBlank(),
                onClick = {
                    scope.launch {
                        isSaving = true
                        val remove = if (description.isBlank()) listOf("Description") else emptyList()
                        runCatching {
                            SharedStoatClient.patchChannel(
                                channelId = channelId,
                                name = name.trim(),
                                description = description.takeIf { it.isNotBlank() },
                                nsfw = nsfw,
                                remove = remove
                            )
                        }.onSuccess {
                            isSaving = false
                            onStatusMessage("Channel updated!")
                            navigator.pop()
                        }.onFailure { err ->
                            isSaving = false
                            onStatusMessage(err.message ?: "Failed to update channel")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (isSaving) "Saving..." else "Save Changes")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedChannelSettingsWebhooksScreen(
    channelId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val webhooks = SharedStoatClient.webhooksByChannel[channelId].orEmpty()
    var newWebhookName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(channelId) {
        isLoading = true
        runCatching { SharedStoatClient.fetchChannelWebhooks(channelId) }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channel Webhooks") },
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
                    value = newWebhookName,
                    onValueChange = { newWebhookName = it },
                    label = { Text("New Webhook Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = newWebhookName.isNotBlank(),
                    onClick = {
                        scope.launch {
                            runCatching { SharedStoatClient.createChannelWebhook(channelId, newWebhookName) }
                                .onSuccess {
                                    newWebhookName = ""
                                    onStatusMessage("Created webhook")
                                }
                                .onFailure { onStatusMessage(it.message ?: "Failed to create webhook") }
                        }
                    }
                ) {
                    Text("Create")
                }
            }

            if (isLoading && webhooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (webhooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No webhooks configured for this channel.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(webhooks, key = { it.id }) { wh ->
                        Card(
                            onClick = { navigator.navigate(SharedRoute.ChannelSettingsWebhookDetail(channelId, wh.id)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(wh.name, fontWeight = FontWeight.Bold)
                                    Text(wh.id, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                }
                                Icon(Icons.Default.Edit, contentDescription = null)
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
fun SharedChannelSettingsWebhookDetailScreen(
    channelId: String,
    webhookId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val wh = SharedStoatClient.webhooksByChannel[channelId].orEmpty().firstOrNull { it.id == webhookId }
    var name by remember(wh) { mutableStateOf(wh?.name.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(wh?.name ?: "Webhook") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Webhook Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    scope.launch {
                        runCatching { SharedStoatClient.editWebhook(channelId, webhookId, name) }
                            .onSuccess { onStatusMessage("Updated webhook") }
                            .onFailure { onStatusMessage(it.message ?: "Failed to update webhook") }
                    }
                }
            ) {
                Text("Save Name")
            }

            val webhookUrl = "${SharedStoatClient.apiBaseUrl}/webhooks/$webhookId/${wh?.token.orEmpty()}"
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Webhook URL", fontWeight = FontWeight.Bold)
                    Text(webhookUrl, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                    Button(
                        onClick = {
                            PlatformInfo.copyToClipboard("Webhook URL", webhookUrl)
                            onStatusMessage("Copied Webhook URL")
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Webhook URL")
                    }
                }
            }

            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = {
                    scope.launch {
                        runCatching { SharedStoatClient.deleteWebhook(channelId, webhookId) }
                            .onSuccess {
                                onStatusMessage("Deleted webhook")
                                navigator.pop()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Webhook")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedChannelSettingsPermissionsScreen(
    channelId: String,
    navigator: SharedNavigator
) {
    val channel = SharedStoatClient.channelCache[channelId]
    val server = channel?.server?.let { SharedStoatClient.serverCache[it] }
    val roles = server?.roles.orEmpty().entries.toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channel Permissions") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    onClick = { navigator.navigate(SharedRoute.ChannelSettingsPermissionEditor(channelId, "default")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("@everyone (Default Permissions)", fontWeight = FontWeight.Bold)
                        Text("Configure base permissions for all members in this channel", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(roles, key = { it.key }) { (rid, role) ->
                Card(
                    onClick = { navigator.navigate(SharedRoute.ChannelSettingsPermissionEditor(channelId, rid)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(role.name ?: rid, fontWeight = FontWeight.Bold)
                        Text("Role ID: $rid", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedChannelSettingsPermissionEditorScreen(
    channelId: String,
    roleId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val channel = SharedStoatClient.channelCache[channelId]
    val initialPerm = if (roleId == "default") {
        channel?.defaultPermissions
    } else {
        channel?.rolePermissions?.get(roleId)
    }

    var allowMask by remember(initialPerm) { mutableStateOf(initialPerm?.a ?: 0L) }
    var denyMask by remember(initialPerm) { mutableStateOf(initialPerm?.d ?: 0L) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (roleId == "default") "@everyone Permissions" else "Role Override") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            scope.launch {
                                isSaving = true
                                runCatching {
                                    SharedStoatClient.setChannelRolePermissions(channelId, roleId, allowMask, denyMask)
                                }.onSuccess {
                                    isSaving = false
                                    onStatusMessage("Channel permissions saved")
                                    navigator.pop()
                                }.onFailure { err ->
                                    isSaving = false
                                    onStatusMessage(err.message ?: "Failed to save permissions")
                                }
                            }
                        }
                    ) {
                        Text(if (isSaving) "..." else "Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(SharedPermissionFlag.entries.toList(), key = { it.name }) { flag ->
                val isAllowed = (allowMask and flag.bit) != 0L
                val isDenied = (denyMask and flag.bit) != 0L
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(flag.label, fontWeight = FontWeight.Bold)
                        Text(flag.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isAllowed,
                                onClick = {
                                    allowMask = allowMask or flag.bit
                                    denyMask = denyMask and flag.bit.inv()
                                },
                                label = { Text("Allow") }
                            )
                            FilterChip(
                                selected = !isAllowed && !isDenied,
                                onClick = {
                                    allowMask = allowMask and flag.bit.inv()
                                    denyMask = denyMask and flag.bit.inv()
                                },
                                label = { Text("Inherit") }
                            )
                            FilterChip(
                                selected = isDenied,
                                onClick = {
                                    denyMask = denyMask or flag.bit
                                    allowMask = allowMask and flag.bit.inv()
                                },
                                label = { Text("Deny") }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. Server Settings Screens
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedServerSettingsHomeScreen(
    serverId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val server = SharedStoatClient.serverCache[serverId]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Settings — ${server?.name ?: "Server"}") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Server Identity", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Customize your server nickname and pronouns") },
                    leadingContent = { Icon(Icons.Default.ManageAccounts, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigator.navigate(SharedRoute.ServerIdentitySettings(serverId, SharedStoatClient.selfId))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Overview", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Server name, description, and system messages") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigator.navigate(SharedRoute.ServerSettingsOverview(serverId))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Channels & Categories", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Create and organize text and voice channels") },
                    leadingContent = { Icon(Icons.Default.Tag, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigator.navigate(SharedRoute.ServerSettingsChannels(serverId))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Roles & Permissions", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Create roles, colors, hoists, and permission bitmasks") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigator.navigate(SharedRoute.ServerSettingsRoles(serverId))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Custom Emojis", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Manage custom server emojis") },
                    leadingContent = { Icon(Icons.Default.EmojiEmotions, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigator.navigate(SharedRoute.ServerSettingsEmojis(serverId))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Invites", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("View and revoke active server invite links") },
                    leadingContent = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigator.navigate(SharedRoute.ServerSettingsInvites(serverId))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Bans", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("View banned users and unban members") },
                    leadingContent = { Icon(Icons.Default.Block, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigator.navigate(SharedRoute.ServerSettingsBans(serverId))
                    }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        scope.launch {
                            runCatching { SharedStoatClient.leaveOrDeleteServer(serverId) }
                                .onSuccess {
                                    onStatusMessage("Left / deleted server")
                                    navigator.pop()
                                }
                                .onFailure { onStatusMessage(it.message ?: "Action failed") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (server?.owner == SharedStoatClient.selfId) "Delete Server" else "Leave Server")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedServerIdentitySettingsScreen(
    serverId: String,
    userId: String?,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val targetUserId = userId ?: SharedStoatClient.selfId ?: ""
    val member = SharedStoatClient.memberCache[serverId]?.get(targetUserId)
    var nickname by remember(member) { mutableStateOf(member?.nickname.orEmpty()) }
    var pronouns by remember(member) { mutableStateOf(member?.pronouns.orEmpty()) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Identity") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Server Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pronouns,
                onValueChange = { pronouns = it },
                label = { Text("Server Pronouns") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = !isSaving && targetUserId.isNotBlank(),
                onClick = {
                    scope.launch {
                        isSaving = true
                        val remove = buildList {
                            if (nickname.isBlank()) add("Nickname")
                            if (pronouns.isBlank()) add("Pronouns")
                        }
                        runCatching {
                            SharedStoatClient.patchServerMember(
                                serverId = serverId,
                                userId = targetUserId,
                                nickname = nickname.takeIf { it.isNotBlank() },
                                pronouns = pronouns.takeIf { it.isNotBlank() },
                                remove = remove
                            )
                        }.onSuccess {
                            isSaving = false
                            onStatusMessage("Server identity updated!")
                            navigator.pop()
                        }.onFailure { err ->
                            isSaving = false
                            onStatusMessage(err.message ?: "Failed to update server identity")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (isSaving) "Saving..." else "Save Identity")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedServerSettingsOverviewScreen(
    serverId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val server = SharedStoatClient.serverCache[serverId]
    var name by remember(server) { mutableStateOf(server?.name.orEmpty()) }
    var description by remember(server) { mutableStateOf(server?.description.orEmpty()) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Overview") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Server Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Server Description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = !isSaving && name.isNotBlank(),
                onClick = {
                    scope.launch {
                        isSaving = true
                        val remove = if (description.isBlank()) listOf("Description") else emptyList()
                        runCatching {
                            SharedStoatClient.patchServer(
                                serverId = serverId,
                                name = name.trim(),
                                description = description.takeIf { it.isNotBlank() },
                                remove = remove
                            )
                        }.onSuccess {
                            isSaving = false
                            onStatusMessage("Server overview saved!")
                            navigator.pop()
                        }.onFailure { err ->
                            isSaving = false
                            onStatusMessage(err.message ?: "Failed to update server")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (isSaving) "Saving..." else "Save Overview")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedServerSettingsChannelsScreen(
    serverId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val server = SharedStoatClient.serverCache[serverId]
    val channels = server?.channels.orEmpty().mapNotNull { SharedStoatClient.channelCache[it] }
    var newChannelName by remember { mutableStateOf("") }
    var isVoice by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Channels") },
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
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create Channel", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newChannelName,
                        onValueChange = { newChannelName = it },
                        label = { Text("Channel Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isVoice, onClick = { isVoice = false }, label = { Text("Text Channel") })
                        FilterChip(selected = isVoice, onClick = { isVoice = true }, label = { Text("Voice Channel") })
                    }
                    Button(
                        enabled = newChannelName.isNotBlank(),
                        onClick = {
                            scope.launch {
                                runCatching {
                                    SharedStoatClient.createServerChannel(serverId, newChannelName, isVoice)
                                }.onSuccess {
                                    newChannelName = ""
                                    onStatusMessage("Created channel #${it.name}")
                                }.onFailure {
                                    onStatusMessage(it.message ?: "Failed to create channel")
                                }
                            }
                        }
                    ) {
                        Text("Create Channel")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channels, key = { it.id ?: "" }) { ch ->
                    val cid = ch.id ?: return@items
                    Card(
                        onClick = { navigator.navigate(SharedRoute.ChannelSettingsHome(cid)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("#${ch.name ?: "channel"} (${ch.channelType?.value ?: "Text"})", fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedServerSettingsRolesScreen(
    serverId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val server = SharedStoatClient.serverCache[serverId]
    val roles = server?.roles.orEmpty().entries.toList()
    var newRoleName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Roles") },
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
                    value = newRoleName,
                    onValueChange = { newRoleName = it },
                    label = { Text("New Role Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = newRoleName.isNotBlank(),
                    onClick = {
                        scope.launch {
                            runCatching { SharedStoatClient.createServerRole(serverId, newRoleName) }
                                .onSuccess { (rid, _) ->
                                    newRoleName = ""
                                    onStatusMessage("Created role")
                                    navigator.navigate(SharedRoute.ServerSettingsRoleEditor(serverId, rid))
                                }
                                .onFailure { onStatusMessage(it.message ?: "Failed to create role") }
                        }
                    }
                ) {
                    Text("Create")
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(roles, key = { it.key }) { (rid, role) ->
                    val roleColor = parseHexColorOrDefault(role.colour, MaterialTheme.colorScheme.primary)
                    Card(
                        onClick = { navigator.navigate(SharedRoute.ServerSettingsRoleEditor(serverId, rid)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(roleColor)
                            )
                            Text(role.name ?: rid, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedServerSettingsRoleEditorScreen(
    serverId: String,
    roleId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val server = SharedStoatClient.serverCache[serverId]
    val role = server?.roles?.get(roleId)
    var name by remember(role) { mutableStateOf(role?.name.orEmpty()) }
    var colour by remember(role) { mutableStateOf(role?.colour.orEmpty()) }
    var hoist by remember(role) { mutableStateOf(role?.hoist == true) }
    var allowMask by remember(role) { mutableStateOf(role?.permissions?.a ?: 0L) }
    var denyMask by remember(role) { mutableStateOf(role?.permissions?.d ?: 0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Role — ${role?.name ?: roleId}") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                runCatching { SharedStoatClient.deleteServerRole(serverId, roleId) }
                                    .onSuccess {
                                        onStatusMessage("Deleted role")
                                        navigator.pop()
                                    }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Role", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Role Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = colour,
                    onValueChange = { colour = it },
                    label = { Text("Role Color Hex (e.g. #5865F2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Display role members separately (Hoist)")
                    Switch(checked = hoist, onCheckedChange = { hoist = it })
                }
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                SharedStoatClient.editServerRole(
                                    serverId = serverId,
                                    roleId = roleId,
                                    name = name,
                                    colour = colour.takeIf { it.isNotBlank() },
                                    hoist = hoist,
                                    remove = if (colour.isBlank()) listOf("Colour") else emptyList()
                                )
                                SharedStoatClient.setServerRolePermissions(serverId, roleId, allowMask, denyMask)
                            }.onSuccess {
                                onStatusMessage("Role updated!")
                                navigator.pop()
                            }.onFailure {
                                onStatusMessage(it.message ?: "Failed to update role")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Role & Permissions")
                }
            }

            items(SharedPermissionFlag.entries.toList(), key = { it.name }) { flag ->
                val isAllowed = (allowMask and flag.bit) != 0L
                val isDenied = (denyMask and flag.bit) != 0L
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(flag.label, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isAllowed,
                                onClick = {
                                    allowMask = allowMask or flag.bit
                                    denyMask = denyMask and flag.bit.inv()
                                },
                                label = { Text("Allow") }
                            )
                            FilterChip(
                                selected = !isAllowed && !isDenied,
                                onClick = {
                                    allowMask = allowMask and flag.bit.inv()
                                    denyMask = denyMask and flag.bit.inv()
                                },
                                label = { Text("Default") }
                            )
                            FilterChip(
                                selected = isDenied,
                                onClick = {
                                    denyMask = denyMask or flag.bit
                                    allowMask = allowMask and flag.bit.inv()
                                },
                                label = { Text("Deny") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedServerSettingsEmojisScreen(
    serverId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val emojis = SharedStoatClient.emojiCache.values.filter { it.parent?.id == serverId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Emojis (${emojis.size})") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (emojis.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No custom emojis in this server.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(emojis, key = { it.id ?: "" }) { emoji ->
                    val eid = emoji.id ?: return@items
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(":${emoji.name}:", fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { SharedStoatClient.deleteServerEmoji(eid) }
                                            .onSuccess { onStatusMessage("Deleted emoji") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
fun SharedServerSettingsInvitesScreen(
    serverId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val invites = SharedStoatClient.serverInvitesByServer[serverId].orEmpty()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(serverId) {
        isLoading = true
        runCatching { SharedStoatClient.fetchServerInvites(serverId) }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Invites") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && invites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (invites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No active invites for this server.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(invites, key = { it.effectiveCode }) { inv ->
                    val code = inv.effectiveCode
                    val url = "$DAWNCHAT_INVITE_SHORT/$code"
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(code, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(url, style = MaterialTheme.typography.labelSmall)
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        PlatformInfo.copyToClipboard("Invite", url)
                                        onStatusMessage("Copied $url")
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            runCatching { SharedStoatClient.deleteInvite(code, serverId) }
                                                .onSuccess { onStatusMessage("Deleted invite $code") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
fun SharedServerSettingsBansScreen(
    serverId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val banList = SharedStoatClient.serverBansByServer[serverId]
    val bans = banList?.bans.orEmpty()
    val bannedUsers = banList?.users.orEmpty().associateBy { it.id }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(serverId) {
        isLoading = true
        runCatching { SharedStoatClient.fetchServerBans(serverId) }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Bans (${bans.size})") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && bans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (bans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No banned users in this server.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bans, key = { it.id.user }) { ban ->
                    val uid = ban.id.user
                    val bu = bannedUsers[uid]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bu?.let { "${it.username}#${it.discriminator}" } ?: uid,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = ban.reason ?: "No reason provided",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { SharedStoatClient.unbanServerMember(serverId, uid) }
                                            .onSuccess { onStatusMessage("Unbanned user") }
                                    }
                                }
                            ) {
                                Text("Unban")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. About, OSS Attribution, Changelog & Labs Sandboxes
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedAboutScreen(navigator: SharedNavigator) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About DawnChat") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("DawnChat (${PlatformInfo.platformName})", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Version ${PlatformInfo.appVersion} • ${PlatformInfo.osVersion}")
                        Text("Connected to ${SharedStoatClient.apiBaseUrl}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { navigator.navigate(SharedRoute.AboutOss) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Source Licenses & Attribution")
                }
            }
            item {
                Text("Release Changelogs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(SharedStoatClient.changelogs, key = { it.id }) { log ->
                Card(
                    onClick = { navigator.navigate(SharedRoute.ReadChangelog(log.id)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(log.title, fontWeight = FontWeight.Bold)
                        Text(log.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(log.summary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedAttributionScreen(navigator: SharedNavigator) {
    val libraries = listOf(
        "Kotlin Multiplatform & Coroutines" to "JetBrains • Apache 2.0",
        "Compose Multiplatform" to "JetBrains & Google • Apache 2.0",
        "Ktor HTTP & WebSockets Client" to "JetBrains • Apache 2.0",
        "Kotlinx Serialization & Datetime" to "JetBrains • Apache 2.0",
        "Stoat / Revolt Protocol Schemas" to "Stoat & DawnChat Contributors • AGPLv3"
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Attribution") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(libraries, key = { it.first }) { (name, license) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(name, fontWeight = FontWeight.Bold)
                        Text(license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedReadChangelogScreen(
    changelogId: String,
    navigator: SharedNavigator
) {
    val entry = SharedStoatClient.changelogs.firstOrNull { it.id == changelogId } ?: SharedStoatClient.changelogs.first()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry.title) },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(entry.date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(entry.summary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            entry.sections.forEach { bullet ->
                Text("• $bullet", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLabsRootScreen(navigator: SharedNavigator) {
    val sandboxes = listOf(
        Triple("New Login Experience Mockup", "Interactive preview of the next-gen login flow", SharedRoute.LabsLoginMockup),
        Triple("Settings DSL Sandbox", "Declarative settings schema & builder testbed", SharedRoute.LabsSettingsDsl),
        Triple("Gradient Editor Sandbox", "Multi-stop role & profile gradient builder", SharedRoute.LabsGradientEditor),
        Triple("New Card Sandbox", "Material 3 expressive surface & card elevation testbed", SharedRoute.LabsNewCard),
        Triple("Telecom & CallKit Sandbox", "Audio routing, CallKit/ConnectionService & voice states", SharedRoute.LabsTelecom)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DawnChat Labs") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sandboxes, key = { it.first }) { (title, subtitle, route) ->
                Card(
                    onClick = { navigator.navigate(route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold)
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLabsSandboxDetailScreen(
    title: String,
    mode: String,
    navigator: SharedNavigator
) {
    var sliderA by remember { mutableStateOf(0.35f) }
    var sliderB by remember { mutableStateOf(0.75f) }
    var toggleA by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (mode) {
                "gradient" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.hsv(sliderA * 360f, 0.8f, 0.95f),
                                        Color.hsv(sliderB * 360f, 0.8f, 0.95f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Live Gradient Preview", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text("Start Hue: ${(sliderA * 360).toInt()}°")
                    Slider(value = sliderA, onValueChange = { sliderA = it })
                    Text("End Hue: ${(sliderB * 360).toInt()}°")
                    Slider(value = sliderB, onValueChange = { sliderB = it })
                }
                else -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Interactive multiplatform sandbox running natively on ${PlatformInfo.platformName}.")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable Experimental Behavior")
                                Switch(checked = toggleA, onCheckedChange = { toggleA = it })
                            }
                        }
                    }
                }
            }
        }
    }
}
