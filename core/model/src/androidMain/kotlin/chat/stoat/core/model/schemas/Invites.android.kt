package chat.stoat.core.model.schemas

import android.net.Uri

fun Uri.isInviteUri(): Boolean = isInviteUrl(this.toString())
