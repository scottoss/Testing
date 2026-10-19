package chat.stoat.screens.settings.channel

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.api
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.channel.EditWebhookBody
import chat.stoat.api.routes.channel.Webhook
import chat.stoat.api.routes.channel.createChannelWebhook
import chat.stoat.api.routes.channel.deleteWebhook
import chat.stoat.api.routes.channel.editWebhook
import chat.stoat.api.routes.channel.fetchChannelWebhooks
import chat.stoat.api.routes.microservices.autumn.uploadToAutumn
import chat.stoat.internals.extensions.rememberChannelPermissions
import chat.stoat.screens.settings.SettingsIcon
import chat.stoat.screens.settings.SettingsListItem
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSettingsWebhooks(navController: NavController, channelId: String) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val permissions by rememberChannelPermissions(channelId)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var webhooks by remember { mutableStateOf<List<Webhook>?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(channelId) {
        try {
            webhooks = fetchChannelWebhooks(channelId)
        } catch (e: Exception) {
            failed = true
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.channel_settings_webhooks_create)) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.channel_settings_webhooks_name)) },
                    singleLine = true,
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        val name = newName.trim()
                        showCreate = false
                        newName = ""
                        scope.launch {
                            try {
                                val created = createChannelWebhook(channelId, name)
                                navController.navigate("settings/channel/$channelId/webhooks/${created.id}")
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.channel_settings_webhooks_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.channel_settings_webhooks_create_confirm))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.channel_settings_webhooks),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Box(Modifier.padding(pv)) {
            val list = webhooks
            when {
                !permissions.hasPermission(PermissionBit.ManageWebhooks) -> Unit
                failed -> Text(
                    text = stringResource(R.string.channel_settings_webhooks_error),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )

                list == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    SettingsListItem(
                        first = true,
                        last = list.isEmpty(),
                        headlineContent = {
                            Text(stringResource(R.string.channel_settings_webhooks_create))
                        },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier.clickable { showCreate = true }
                    )
                    if (list.isEmpty()) {
                        Text(
                            text = stringResource(R.string.channel_settings_webhooks_empty),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    list.forEachIndexed { index, webhook ->
                        Spacer(Modifier.height(2.dp))
                        SettingsListItem(
                            last = index == list.lastIndex,
                            headlineContent = { Text(webhook.name) },
                            supportingContent = { Text(webhook.id) },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_webhook_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                navController.navigate("settings/channel/$channelId/webhooks/${webhook.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSettingsWebhook(navController: NavController, channelId: String, webhookId: String) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val permissions by rememberChannelPermissions(channelId)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var webhook by remember { mutableStateOf<Webhook?>(null) }
    var name by remember { mutableStateOf("") }
    var removeAvatar by remember { mutableStateOf(false) }
    var pickedAvatarId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    fun toast(res: Int) = Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()

    LaunchedEffect(channelId, webhookId) {
        try {
            webhook = fetchChannelWebhooks(channelId).firstOrNull { it.id == webhookId }
            name = webhook?.name ?: ""
        } catch (e: Exception) {
            toast(R.string.channel_settings_webhooks_error)
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            try {
                pickedAvatarId = withContext(Dispatchers.IO) {
                    val mime = context.contentResolver.getType(uri) ?: "image/*"
                    val tmp = File.createTempFile("webhook_avatar", null, context.cacheDir)
                    try {
                        context.contentResolver.openInputStream(uri)!!.use { input ->
                            tmp.outputStream().use { input.copyTo(it) }
                        }
                        uploadToAutumn(tmp, "avatar", "avatars", ContentType.parse(mime))
                    } finally {
                        tmp.delete()
                    }
                }
                removeAvatar = false
            } catch (e: Exception) {
                toast(R.string.channel_settings_webhooks_error)
            }
            busy = false
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.channel_settings_webhooks_delete_confirm)) },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDelete = false
                    scope.launch {
                        try {
                            deleteWebhook(webhookId)
                            navController.popBackStack()
                        } catch (e: Exception) {
                            toast(R.string.channel_settings_webhooks_error)
                        }
                    }
                }) {
                    Text(stringResource(R.string.channel_settings_webhooks_delete))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = webhook?.name ?: stringResource(R.string.channel_settings_webhooks),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Box(Modifier.padding(pv)) {
            val current = webhook
            if (!permissions.hasPermission(PermissionBit.ManageWebhooks)) return@Box
            if (current == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                return@Box
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.channel_settings_webhooks_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { avatarPicker.launch("image/*") },
                    enabled = !busy,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        stringResource(
                            if (pickedAvatarId != null) R.string.channel_settings_webhooks_avatar_picked
                            else R.string.channel_settings_webhooks_avatar_change
                        )
                    )
                }
                if (current.avatar != null || pickedAvatarId != null) {
                    TextButton(
                        onClick = {
                            removeAvatar = !removeAvatar
                            if (removeAvatar) pickedAvatarId = null
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            stringResource(
                                if (removeAvatar) R.string.channel_settings_webhooks_avatar_remove_undo
                                else R.string.channel_settings_webhooks_avatar_remove
                            )
                        )
                    }
                }
                Button(
                    enabled = !busy && name.isNotBlank() &&
                            (name.trim() != current.name || removeAvatar || pickedAvatarId != null),
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val updated = editWebhook(
                                    webhookId,
                                    EditWebhookBody(
                                        name = name.trim().takeIf { it != current.name },
                                        avatar = pickedAvatarId,
                                        remove = if (removeAvatar) listOf("Avatar") else null,
                                    )
                                )
                                webhook = updated.copy(token = updated.token ?: current.token)
                                pickedAvatarId = null
                                removeAvatar = false
                            } catch (e: Exception) {
                                toast(R.string.channel_settings_webhooks_error)
                            }
                            busy = false
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(R.string.channel_settings_overview_save))
                }
                Spacer(Modifier.height(16.dp))

                val token = current.token
                SettingsListItem(
                    first = true,
                    last = token == null,
                    headlineContent = { Text(stringResource(R.string.channel_settings_webhooks_copy_url)) },
                    leadingContent = {
                        SettingsIcon {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_copy_24dp),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.clickable(enabled = token != null) {
                        clipboard.setText(AnnotatedString("/webhooks/${current.id}/$token".api()))
                        toast(R.string.channel_settings_webhooks_copied)
                    }
                )
                if (token != null) {
                    Spacer(Modifier.height(2.dp))
                    SettingsListItem(
                        headlineContent = { Text(stringResource(R.string.channel_settings_webhooks_copy_token)) },
                        leadingContent = {
                            SettingsIcon {
                                Icon(
                                    painter = painterResource(R.drawable.ic_key_24dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            clipboard.setText(AnnotatedString(token))
                            toast(R.string.channel_settings_webhooks_copied)
                        }
                    )
                }
                Spacer(Modifier.height(2.dp))
                SettingsListItem(
                    last = true,
                    headlineContent = { Text(stringResource(R.string.channel_settings_webhooks_delete)) },
                    leadingContent = {
                        SettingsIcon(danger = true) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_24dp),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.clickable { showDelete = true }
                )
            }
        }
    }
}
