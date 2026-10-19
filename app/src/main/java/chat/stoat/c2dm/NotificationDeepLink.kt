package chat.stoat.c2dm

import chat.stoat.internals.StoatWebLink
import kotlinx.coroutines.flow.MutableStateFlow

object NotificationDeepLink {
    val pendingNavigation = MutableStateFlow<StoatWebLink?>(null)

    // Nav-graph route for auth links (reset/verify/delete) from the web
    val pendingAuthRoute = MutableStateFlow<String?>(null)
}
