package chat.stoat.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
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
import chat.stoat.api.settings.ServerFolders
import chat.stoat.api.settings.ServerSidebarEntry
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.api.settings.resolveServerSidebar
import chat.stoat.composables.screens.chat.drawer.ServerIconImage
import chat.stoat.composables.screens.chat.drawer.parseFolderColour
import chat.stoat.core.model.schemas.Server
import chat.stoat.internals.extensions.zero
import chat.stoat.screens.chat.dialogs.safety.ReportServerDialog
import chat.stoat.sheets.AddServerSheet
import chat.stoat.sheets.ServerContextSheet
import chat.stoat.sheets.ServerFolderPickerSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CommunitiesScreen(navController: NavController) {
    var contextTarget by remember { mutableStateOf<String?>(null) }
    var folderPickerTarget by remember { mutableStateOf<String?>(null) }
    var reportTarget by remember { mutableStateOf<String?>(null) }
    var showAddServer by remember { mutableStateOf(false) }

    if (showAddServer) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismissRequest = { showAddServer = false }
        ) {
            AddServerSheet(onDismiss = { showAddServer = false })
        }
    }

    contextTarget?.let { serverId ->
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(sheetState = state, onDismissRequest = { contextTarget = null }) {
            ServerContextSheet(
                serverId = serverId,
                onHideSheet = {
                    state.hide()
                    contextTarget = null
                },
                onReportServer = { reportTarget = serverId },
                onPickFolder = {
                    state.hide()
                    contextTarget = null
                    folderPickerTarget = serverId
                }
            )
        }
    }

    folderPickerTarget?.let { serverId ->
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(sheetState = state, onDismissRequest = { folderPickerTarget = null }) {
            ServerFolderPickerSheet(
                serverId = serverId,
                onHideSheet = {
                    state.hide()
                    folderPickerTarget = null
                }
            )
        }
    }

    reportTarget?.let { serverId ->
        ReportServerDialog(onDismiss = { reportTarget = null }, serverId = serverId)
    }

    val entries = resolveServerSidebar(
        servers = StoatAPI.serverCache.filterValues { it.id != null },
        ordering = SyncedSettings.ordering,
        folders = SyncedSettings.serverFolders.folders,
    )

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(NavigationBarDefaults.windowInsets),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.main_tab_communities)) },
                windowInsets = WindowInsets.zero,
                actions = {
                    IconButton(onClick = { showAddServer = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24dp),
                            contentDescription = stringResource(R.string.overview_screen_create_or_join)
                        )
                    }
                }
            )
        },
    ) { pv ->
        LazyColumn(modifier = Modifier.padding(pv)) {
            entries.forEach { entry ->
                when (entry) {
                    is ServerSidebarEntry.Single -> item(key = entry.id) {
                        ServerRow(
                            server = entry.server,
                            onClick = { navController.navigate("main/server/${entry.id}") },
                            onLongClick = { contextTarget = entry.id },
                        )
                    }

                    is ServerSidebarEntry.Folder -> {
                        item(key = entry.id) {
                            val collapsed = entry.folder.collapsed == true
                            val unread = entry.servers.any {
                                it.id?.let { id -> StoatAPI.unreads.serverHasUnread(id) } == true
                            }
                            ListItem(
                                headlineContent = {
                                    Text(
                                        entry.folder.name.ifBlank {
                                            stringResource(R.string.server_folder_unnamed)
                                        }
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder_24dp),
                                        contentDescription = null,
                                        tint = entry.folder.colour?.let(::parseFolderColour)
                                            ?: LocalContentColor.current
                                    )
                                },
                                trailingContent = {
                                    if (collapsed && unread) Badge()
                                },
                                modifier = Modifier.combinedClickable(
                                    onClick = { ServerFolders.toggle(entry.id) }
                                )
                            )
                        }
                        if (entry.folder.collapsed != true) {
                            items(entry.servers, key = { "${entry.id}/${it.id}" }) { server ->
                                ServerRow(
                                    server = server,
                                    indented = true,
                                    onClick = { navController.navigate("main/server/${server.id}") },
                                    onLongClick = { contextTarget = server.id },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerRow(
    server: Server,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    indented: Boolean = false,
) {
    val hasUnread = server.id?.let { StoatAPI.unreads.serverHasUnread(it) } == true
    ListItem(
        headlineContent = {
            Text(
                server.name ?: stringResource(R.string.unknown),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = { ServerIconImage(server, Modifier.size(40.dp)) },
        trailingContent = { if (hasUnread) Badge() },
        modifier = Modifier
            .padding(start = if (indented) 24.dp else 0.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
}
