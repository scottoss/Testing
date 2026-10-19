package chat.stoat.api.internals

import chat.stoat.api.StoatAPI
import chat.stoat.api.api
import chat.stoat.core.model.schemas.User

object ResourceLocations {
    fun userAvatarUrl(user: User?): String {
        if (user?.avatar != null) {
            return "${StoatAPI.getAutumnUrl()}/avatars/${user.avatar!!.id}"
        }
        return "/users/${(user?.id ?: "").ifBlank { "0".repeat(26) }}/default_avatar".api()
    }
}
