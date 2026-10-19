package chat.stoat.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import chat.stoat.api.routes.user.OwnedBot
import chat.stoat.api.routes.user.OwnedBotUser
import chat.stoat.api.routes.user.createBot
import chat.stoat.api.routes.user.deleteBot
import chat.stoat.api.routes.user.fetchOwnedBots
import chat.stoat.api.routes.user.renameBot
import chat.stoat.api.routes.user.resetBotToken
import chat.stoat.api.routes.user.setBotPublic
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotsSettingsScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var bots by remember { mutableStateOf<List<Pair<OwnedBot, OwnedBotUser?>>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val res = fetchOwnedBots()
            bots = res.bots.map { bot -> bot to res.users.firstOrNull { it.id == bot.id } }
        } catch (e: Exception) {
            failed = true
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.settings_bots_create)) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.settings_bots_name)) },
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
                                val created = createBot(name)
                                navController.navigate("settings/bots/${created.id}")
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_bots_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_bots_create_confirm))
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
                        text = stringResource(R.string.settings_bots),
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
            val list = bots
            when {
                failed -> Text(
                    text = stringResource(R.string.settings_bots_error),
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
                        headlineContent = { Text(stringResource(R.string.settings_bots_create)) },
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
                            text = stringResource(R.string.settings_bots_empty),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    list.forEachIndexed { index, (bot, user) ->
                        Spacer(Modifier.height(2.dp))
                        SettingsListItem(
                            last = index == list.lastIndex,
                            headlineContent = { Text(user?.username ?: bot.id) },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_smart_toy_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                navController.navigate("settings/bots/${bot.id}")
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
fun BotSettingsScreen(navController: NavController, botId: String) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var bot by remember { mutableStateOf<OwnedBot?>(null) }
    var username by remember { mutableStateOf("") }
    var savedUsername by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    fun toast(res: Int) = Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()

    LaunchedEffect(botId) {
        try {
            val res = fetchOwnedBots()
            bot = res.bots.firstOrNull { it.id == botId }
            savedUsername = res.users.firstOrNull { it.id == botId }?.username ?: ""
            username = savedUsername
        } catch (e: Exception) {
            toast(R.string.settings_bots_error)
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.settings_bots_reset_token_confirm)) },
            dismissButton = {
                TextButton(onClick = { showReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(onClick = {
                    showReset = false
                    scope.launch {
                        try {
                            bot = resetBotToken(botId)
                        } catch (e: Exception) {
                            toast(R.string.settings_bots_error)
                        }
                    }
                }) {
                    Text(stringResource(R.string.settings_bots_reset_token))
                }
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.settings_bots_delete_confirm)) },
            text = { Text(stringResource(R.string.settings_bots_delete_confirm_description)) },
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
                            deleteBot(botId)
                            navController.popBackStack()
                        } catch (e: Exception) {
                            toast(R.string.settings_bots_error)
                        }
                    }
                }) {
                    Text(stringResource(R.string.settings_bots_delete))
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
                        text = savedUsername.ifEmpty { stringResource(R.string.settings_bots) },
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
            val current = bot
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
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.settings_bots_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )
                Button(
                    enabled = username.isNotBlank() && username.trim() != savedUsername,
                    onClick = {
                        scope.launch {
                            try {
                                renameBot(botId, username.trim())
                                savedUsername = username.trim()
                            } catch (e: Exception) {
                                toast(R.string.settings_bots_error)
                            }
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(stringResource(R.string.channel_settings_overview_save))
                }

                SettingsListItem(
                    first = true,
                    headlineContent = { Text(stringResource(R.string.settings_bots_public)) },
                    supportingContent = { Text(stringResource(R.string.settings_bots_public_description)) },
                    leadingContent = {
                        SettingsIcon {
                            Icon(
                                painter = painterResource(R.drawable.ic_smart_toy_24dp),
                                contentDescription = null,
                            )
                        }
                    },
                    trailingContent = {
                        Switch(
                            checked = current.isPublic,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    try {
                                        bot = setBotPublic(botId, checked)
                                    } catch (e: Exception) {
                                        toast(R.string.settings_bots_error)
                                    }
                                }
                            }
                        )
                    },
                )
                Spacer(Modifier.height(2.dp))
                SettingsListItem(
                    headlineContent = { Text(stringResource(R.string.settings_bots_copy_token)) },
                    leadingContent = {
                        SettingsIcon {
                            Icon(
                                painter = painterResource(R.drawable.ic_key_24dp),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString(current.token))
                        toast(R.string.settings_bots_copied)
                    }
                )
                Spacer(Modifier.height(2.dp))
                SettingsListItem(
                    headlineContent = { Text(stringResource(R.string.settings_bots_copy_id)) },
                    leadingContent = {
                        SettingsIcon {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_copy_24dp),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString(current.id))
                        toast(R.string.settings_bots_copied)
                    }
                )
                Spacer(Modifier.height(2.dp))
                SettingsListItem(
                    headlineContent = { Text(stringResource(R.string.settings_bots_reset_token)) },
                    supportingContent = { Text(stringResource(R.string.settings_bots_reset_token_description)) },
                    leadingContent = {
                        SettingsIcon {
                            Icon(
                                painter = painterResource(R.drawable.ic_key_24dp),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.clickable { showReset = true }
                )
                Spacer(Modifier.height(2.dp))
                SettingsListItem(
                    last = true,
                    headlineContent = { Text(stringResource(R.string.settings_bots_delete)) },
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
