package chat.stoat.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chat.stoat.core.model.schemas.Message
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.User
import chat.stoat.shared.network.SharedStoatClient
import chat.stoat.shared.platform.rememberVoiceCallController
import chat.stoat.shared.ui.theme.SharedDawnChatTheme
import kotlinx.coroutines.launch

@Composable
fun DawnChatMultiplatformApp() {
    SharedDawnChatTheme {
        val scope = rememberCoroutineScope()
        val navigator = remember { SharedNavigator(SharedRoute.DefaultDestination) }
        val voiceController = rememberVoiceCallController()
        val snackbarHostState = remember { SnackbarHostState() }

        // Global Modal & Overlay States
        var showVoiceFullscreen by remember { mutableStateOf(false) }
        var showCreateOrJoinServerDialog by remember { mutableStateOf(false) }
        var activeInviteChannelId by remember { mutableStateOf<String?>(null) }
        var activeContextServer by remember { mutableStateOf<Server?>(null) }
        var activeProfileUser by remember { mutableStateOf<Pair<User, String?>?>(null) }
        var activeContextMessage by remember { mutableStateOf<Message?>(null) }
        var activeReportMessage by remember { mutableStateOf<Message?>(null) }
        var activeReportUser by remember { mutableStateOf<User?>(null) }
        var activeReportServer by remember { mutableStateOf<Server?>(null) }

        val showStatusMessage: (String) -> Unit = { message ->
            if (message.isNotBlank()) {
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(message)
                }
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val route = navigator.current) {
                    // 1. Startup & Connectivity
                    SharedRoute.DefaultDestination -> SharedDefaultDestinationScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )

                    // 2. Authentication & Onboarding
                    SharedRoute.LoginGreeting -> SharedLoginGreetingScreen(navigator = navigator)
                    SharedRoute.LoginInit -> SharedInitScreen(navigator = navigator)
                    SharedRoute.Login -> SharedLoginScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.LoginMfa -> SharedMfaScreen(
                        mfaTicket = route.mfaTicket,
                        allowedMethods = route.allowedMethods,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.AccountRecovery -> SharedAccountRecoveryScreen(
                        mode = route.mode,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ConfirmPasswordReset -> SharedConfirmPasswordResetScreen(
                        initialToken = route.token,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.TokenAction -> SharedTokenActionScreen(
                        mode = route.mode,
                        initialToken = route.token,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.RegisterGreeting -> SharedRegisterGreetingScreen(navigator = navigator)
                    SharedRoute.RegisterDetails -> SharedRegisterDetailsScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.RegisterVerify -> SharedRegisterVerifyScreen(
                        email = route.email,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.Onboarding -> SharedOnboardingScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )

                    // 3. Main Chat & Polar Layouts
                    SharedRoute.ChatRouter -> SharedChatRouterScreen(
                        navigator = navigator,
                        voiceController = voiceController,
                        onOpenVoiceFullscreen = { showVoiceFullscreen = true },
                        onOpenCreateJoinServer = { showCreateOrJoinServerDialog = true },
                        onOpenInviteDialog = { cid -> activeInviteChannelId = cid },
                        onOpenServerContextSheet = { srv -> activeContextServer = srv },
                        onOpenUserProfileSheet = { u, sid -> activeProfileUser = u to sid },
                        onOpenMessageContextSheet = { msg -> activeContextMessage = msg },
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.PolarMain -> SharedPolarMainScreen(
                        navigator = navigator,
                        onOpenCreateJoinServer = { showCreateOrJoinServerDialog = true },
                        onOpenUserProfile = { u, sid -> activeProfileUser = u to sid },
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.PolarServerChannels -> SharedPolarServerChannelsScreen(
                        serverId = route.serverId,
                        navigator = navigator,
                        voiceController = voiceController,
                        onOpenVoiceFullscreen = { showVoiceFullscreen = true },
                        onOpenServerContextSheet = { srv -> activeContextServer = srv }
                    )
                    is SharedRoute.PolarConversation -> SharedChannelScreen(
                        channelId = route.channelId,
                        navigator = navigator,
                        onBack = { navigator.pop() },
                        onOpenInvite = { activeInviteChannelId = route.channelId },
                        onOpenUserProfile = { u ->
                            val sid = SharedStoatClient.channelCache[route.channelId]?.server
                            activeProfileUser = u to sid
                        },
                        onOpenMessageContext = { msg -> activeContextMessage = msg },
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.CatchUp -> SharedCatchUpScreen(
                        navigator = navigator,
                        onOpenChannel = { cid ->
                            navigator.navigate(SharedRoute.PolarConversation(cid))
                        },
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.CreateGroup -> SharedCreateGroupScreen(
                        navigator = navigator,
                        onGroupCreated = { group ->
                            group.id?.let { cid ->
                                navigator.replaceCurrent(SharedRoute.PolarConversation(cid))
                            } ?: navigator.pop()
                        },
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.Discover -> SharedDiscoverScreen(
                        navigator = navigator,
                        onServerJoined = { sid ->
                            if (SharedStoatClient.polarLayoutEnabled) {
                                navigator.replaceCurrent(SharedRoute.PolarServerChannels(sid))
                            } else {
                                navigator.pop()
                            }
                        },
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ChannelPins -> SharedChannelPinsScreen(
                        channelId = route.channelId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ChannelSearch -> SharedChannelSearchScreen(
                        channelId = route.channelId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )

                    // 4. User Settings
                    SharedRoute.SettingsRoot -> SharedSettingsRootScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.AccountSettings -> SharedAccountSettingsScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.MfaSettings -> SharedMfaSettingsScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.ProfileSettings -> SharedProfileSettingsScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.SessionsSettings -> SharedSessionsSettingsScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.BotsSettings -> SharedBotsSettingsScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.BotDetailSettings -> SharedBotDetailSettingsScreen(
                        botId = route.botId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.AppearanceSettings -> SharedAppearanceSettingsScreen(navigator = navigator)
                    SharedRoute.ChatSettings -> SharedChatSettingsScreen(navigator = navigator)
                    SharedRoute.VoiceSettings -> SharedVoiceSettingsScreen(navigator = navigator)
                    SharedRoute.NotificationsSettings -> SharedNotificationsSettingsScreen(navigator = navigator)
                    SharedRoute.LanguageSettings -> SharedLanguageSettingsScreen(navigator = navigator)
                    SharedRoute.ExperimentsSettings -> SharedExperimentsSettingsScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.DebugSettings -> SharedDebugSettingsScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    SharedRoute.NetworkInspector -> SharedNetworkInspectorScreen(
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )

                    // 5. Channel Settings
                    is SharedRoute.ChannelSettingsHome -> SharedChannelSettingsHomeScreen(
                        channelId = route.channelId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ChannelSettingsOverview -> SharedChannelSettingsOverviewScreen(
                        channelId = route.channelId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ChannelSettingsWebhooks -> SharedChannelSettingsWebhooksScreen(
                        channelId = route.channelId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ChannelSettingsWebhookDetail -> SharedChannelSettingsWebhookDetailScreen(
                        channelId = route.channelId,
                        webhookId = route.webhookId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ChannelSettingsPermissions -> SharedChannelSettingsPermissionsScreen(
                        channelId = route.channelId,
                        navigator = navigator
                    )
                    is SharedRoute.ChannelSettingsPermissionEditor -> SharedChannelSettingsPermissionEditorScreen(
                        channelId = route.channelId,
                        roleId = route.roleId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )

                    // 6. Server Settings
                    is SharedRoute.ServerSettingsHome -> SharedServerSettingsHomeScreen(
                        serverId = route.serverId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ServerIdentitySettings -> SharedServerIdentitySettingsScreen(
                        serverId = route.serverId,
                        userId = route.userId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ServerSettingsOverview -> SharedServerSettingsOverviewScreen(
                        serverId = route.serverId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ServerSettingsEmojis -> SharedServerSettingsEmojisScreen(
                        serverId = route.serverId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ServerSettingsInvites -> SharedServerSettingsInvitesScreen(
                        serverId = route.serverId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ServerSettingsBans -> SharedServerSettingsBansScreen(
                        serverId = route.serverId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ServerSettingsChannels -> SharedServerSettingsChannelsScreen(
                        serverId = route.serverId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ServerSettingsRoles -> SharedServerSettingsRolesScreen(
                        serverId = route.serverId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )
                    is SharedRoute.ServerSettingsRoleEditor -> SharedServerSettingsRoleEditorScreen(
                        serverId = route.serverId,
                        roleId = route.roleId,
                        navigator = navigator,
                        onStatusMessage = showStatusMessage
                    )

                    // 7. About, Attribution, Changelog & Labs
                    SharedRoute.About -> SharedAboutScreen(navigator = navigator)
                    SharedRoute.AboutOss -> SharedAttributionScreen(navigator = navigator)
                    is SharedRoute.ReadChangelog -> SharedReadChangelogScreen(
                        changelogId = route.id,
                        navigator = navigator
                    )
                    SharedRoute.LabsRoot -> SharedLabsRootScreen(navigator = navigator)
                    SharedRoute.LabsLoginMockup -> SharedLabsSandboxDetailScreen(
                        title = "New Login Experience Sandbox",
                        mode = "login",
                        navigator = navigator
                    )
                    SharedRoute.LabsSettingsDsl -> SharedLabsSandboxDetailScreen(
                        title = "Settings DSL Sandbox",
                        mode = "dsl",
                        navigator = navigator
                    )
                    SharedRoute.LabsGradientEditor -> SharedLabsSandboxDetailScreen(
                        title = "Gradient Editor Sandbox",
                        mode = "gradient",
                        navigator = navigator
                    )
                    SharedRoute.LabsNewCard -> SharedLabsSandboxDetailScreen(
                        title = "Material 3 Expressive Card Sandbox",
                        mode = "card",
                        navigator = navigator
                    )
                    SharedRoute.LabsTelecom -> SharedLabsSandboxDetailScreen(
                        title = "Telecom & CallKit Sandbox",
                        mode = "telecom",
                        navigator = navigator
                    )
                }

                // Floating Voice Call PiP when minimized
                if (!showVoiceFullscreen) {
                    SharedVoiceFloatingPip(
                        voiceController = voiceController,
                        onExpand = { showVoiceFullscreen = true },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }

                // Fullscreen Voice Call Overlay
                if (showVoiceFullscreen) {
                    SharedVoiceFullscreenOverlay(
                        voiceController = voiceController,
                        onMinimize = { showVoiceFullscreen = false }
                    )
                }
            }
        }

        // Global Modals & BottomSheets
        if (showCreateOrJoinServerDialog) {
            SharedCreateOrJoinServerDialog(
                onDismiss = { showCreateOrJoinServerDialog = false },
                onServerSelected = { sid ->
                    if (SharedStoatClient.polarLayoutEnabled) {
                        navigator.navigate(SharedRoute.PolarServerChannels(sid))
                    }
                },
                onStatusMessage = showStatusMessage
            )
        }

        activeInviteChannelId?.let { cid ->
            SharedInviteDialog(
                channelId = cid,
                onDismiss = { activeInviteChannelId = null },
                onStatusMessage = showStatusMessage
            )
        }

        activeContextServer?.let { srv ->
            val sid = srv.id ?: ""
            val defaultChannelId = srv.channels?.firstOrNull()
            SharedServerContextSheet(
                server = srv,
                onDismiss = { activeContextServer = null },
                onInvite = {
                    if (defaultChannelId != null) {
                        activeInviteChannelId = defaultChannelId
                    } else {
                        showStatusMessage("No channels available to create invite")
                    }
                },
                onOpenServerSettings = {
                    if (sid.isNotBlank()) {
                        navigator.navigate(SharedRoute.ServerSettingsHome(sid))
                    }
                },
                onOpenServerIdentity = {
                    if (sid.isNotBlank()) {
                        navigator.navigate(SharedRoute.ServerIdentitySettings(sid, SharedStoatClient.selfId))
                    }
                },
                onReportServer = {
                    activeReportServer = srv
                },
                onStatusMessage = showStatusMessage
            )
        }

        activeProfileUser?.let { (user, serverId) ->
            SharedUserProfileSheet(
                user = user,
                serverId = serverId,
                onDismiss = { activeProfileUser = null },
                onOpenDm = { dm ->
                    dm.id?.let { cid ->
                        navigator.navigate(SharedRoute.PolarConversation(cid))
                    }
                },
                onOpenServerIdentity = { sid, uid ->
                    navigator.navigate(SharedRoute.ServerIdentitySettings(sid, uid))
                },
                onReportUser = { u ->
                    activeReportUser = u
                },
                onStatusMessage = showStatusMessage
            )
        }

        activeContextMessage?.let { msg ->
            val cid = msg.channel ?: ""
            SharedMessageContextSheet(
                message = msg,
                onDismiss = { activeContextMessage = null },
                onReply = {
                    showStatusMessage("Long-press message in channel to reply or edit inline")
                },
                onEdit = {
                    showStatusMessage("Long-press message in channel to edit inline")
                },
                onReact = { m, emoji ->
                    val mid = m.id ?: return@SharedMessageContextSheet
                    if (cid.isNotBlank()) {
                        scope.launch {
                            runCatching { SharedStoatClient.reactToMessage(cid, mid, emoji) }
                        }
                    }
                },
                onTogglePin = { m ->
                    val mid = m.id ?: return@SharedMessageContextSheet
                    if (cid.isNotBlank()) {
                        scope.launch {
                            runCatching {
                                if (m.pinned == true) {
                                    SharedStoatClient.unpinMessage(cid, mid)
                                } else {
                                    SharedStoatClient.pinMessage(cid, mid)
                                }
                            }.onSuccess {
                                showStatusMessage(if (m.pinned == true) "Unpinned message" else "Pinned message")
                            }
                        }
                    }
                },
                onReport = { m ->
                    activeReportMessage = m
                },
                onDelete = { m ->
                    val mid = m.id ?: return@SharedMessageContextSheet
                    if (cid.isNotBlank()) {
                        scope.launch {
                            runCatching { SharedStoatClient.deleteMessage(cid, mid) }
                                .onSuccess { showStatusMessage("Message deleted") }
                        }
                    }
                },
                onStatusMessage = showStatusMessage
            )
        }

        activeReportMessage?.let { msg ->
            SharedReportMessageDialog(
                message = msg,
                onDismiss = { activeReportMessage = null },
                onStatusMessage = showStatusMessage
            )
        }

        activeReportUser?.let { u ->
            SharedReportUserDialog(
                user = u,
                onDismiss = { activeReportUser = null },
                onStatusMessage = showStatusMessage
            )
        }

        activeReportServer?.let { srv ->
            SharedReportServerDialog(
                server = srv,
                onDismiss = { activeReportServer = null },
                onStatusMessage = showStatusMessage
            )
        }
    }
}
