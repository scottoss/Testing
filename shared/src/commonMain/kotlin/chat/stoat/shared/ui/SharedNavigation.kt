package chat.stoat.shared.ui

import androidx.compose.runtime.mutableStateListOf

sealed class SharedRoute {
    // 1. Startup & Connectivity
    data object DefaultDestination : SharedRoute()

    // 2. Authentication & Onboarding
    data object LoginGreeting : SharedRoute()
    data object LoginInit : SharedRoute()
    data object Login : SharedRoute()
    data class LoginMfa(
        val mfaTicket: String,
        val allowedMethods: List<String>
    ) : SharedRoute()
    data class AccountRecovery(val mode: String) : SharedRoute() // "password" or "verify"
    data class ConfirmPasswordReset(val token: String = "") : SharedRoute()
    data class TokenAction(val mode: String, val token: String = "") : SharedRoute() // "verify" or "delete"
    data object RegisterGreeting : SharedRoute()
    data object RegisterDetails : SharedRoute()
    data class RegisterVerify(val email: String) : SharedRoute()
    data object Onboarding : SharedRoute()

    // 3. Main Chat & Polar Layouts
    data object ChatRouter : SharedRoute()
    data object PolarMain : SharedRoute()
    data class PolarServerChannels(val serverId: String) : SharedRoute()
    data class PolarConversation(val channelId: String) : SharedRoute()
    data object CatchUp : SharedRoute()
    data object CreateGroup : SharedRoute()
    data object Discover : SharedRoute()
    data class ChannelPins(val channelId: String) : SharedRoute()
    data class ChannelSearch(val channelId: String) : SharedRoute()

    // 4. User Settings
    data object SettingsRoot : SharedRoute()
    data object AccountSettings : SharedRoute()
    data object MfaSettings : SharedRoute()
    data object ProfileSettings : SharedRoute()
    data object SessionsSettings : SharedRoute()
    data object BotsSettings : SharedRoute()
    data class BotDetailSettings(val botId: String) : SharedRoute()
    data object AppearanceSettings : SharedRoute()
    data object ChatSettings : SharedRoute()
    data object VoiceSettings : SharedRoute()
    data object NotificationsSettings : SharedRoute()
    data object LanguageSettings : SharedRoute()
    data object ExperimentsSettings : SharedRoute()
    data object DebugSettings : SharedRoute()
    data object NetworkInspector : SharedRoute()

    // 5. Channel Settings
    data class ChannelSettingsHome(val channelId: String) : SharedRoute()
    data class ChannelSettingsOverview(val channelId: String) : SharedRoute()
    data class ChannelSettingsWebhooks(val channelId: String) : SharedRoute()
    data class ChannelSettingsWebhookDetail(val channelId: String, val webhookId: String) : SharedRoute()
    data class ChannelSettingsPermissions(val channelId: String) : SharedRoute()
    data class ChannelSettingsPermissionEditor(val channelId: String, val roleId: String) : SharedRoute()

    // 6. Server Settings
    data class ServerSettingsHome(val serverId: String) : SharedRoute()
    data class ServerIdentitySettings(val serverId: String, val userId: String? = null) : SharedRoute()
    data class ServerSettingsOverview(val serverId: String) : SharedRoute()
    data class ServerSettingsEmojis(val serverId: String) : SharedRoute()
    data class ServerSettingsInvites(val serverId: String) : SharedRoute()
    data class ServerSettingsBans(val serverId: String) : SharedRoute()
    data class ServerSettingsChannels(val serverId: String) : SharedRoute()
    data class ServerSettingsRoles(val serverId: String) : SharedRoute()
    data class ServerSettingsRoleEditor(val serverId: String, val roleId: String) : SharedRoute()

    // 7. About, Changelog & Labs
    data object About : SharedRoute()
    data object AboutOss : SharedRoute()
    data class ReadChangelog(val id: String) : SharedRoute()
    data object LabsRoot : SharedRoute()
    data object LabsLoginMockup : SharedRoute()
    data object LabsSettingsDsl : SharedRoute()
    data object LabsGradientEditor : SharedRoute()
    data object LabsNewCard : SharedRoute()
    data object LabsTelecom : SharedRoute()
}

class SharedNavigator(initial: SharedRoute = SharedRoute.DefaultDestination) {
    private val backStack = mutableStateListOf(initial)

    val current: SharedRoute
        get() = backStack.lastOrNull() ?: SharedRoute.DefaultDestination

    val canGoBack: Boolean
        get() = backStack.size > 1

    fun navigate(route: SharedRoute) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun replaceRoot(route: SharedRoute) {
        backStack.clear()
        backStack.add(route)
    }

    fun replaceCurrent(route: SharedRoute) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = route
        } else {
            backStack.add(route)
        }
    }

    fun pop(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            return true
        }
        return false
    }
}
