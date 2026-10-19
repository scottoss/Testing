package chat.stoat.core.model.schemas

import chat.stoat.core.model.util.CommonParcelable
import chat.stoat.core.model.util.CommonParcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@CommonParcelize
@Serializable
data class AutumnResource(
    @SerialName("_id")
    val id: String? = null,

    val tag: String? = null,
    val filename: String? = null,
    val metadata: Metadata? = null,

    @SerialName("content_type")
    val contentType: String? = null,

    val size: Long? = null,
    val deleted: Boolean? = null,
    val reported: Boolean? = null,

    @SerialName("message_id")
    val messageID: String? = null,

    @SerialName("user_id")
    val userID: String? = null,

    @SerialName("server_id")
    val serverID: String? = null,

    @SerialName("object_id")
    val objectID: String? = null
) : CommonParcelable

@CommonParcelize
@Serializable
data class Metadata(
    val type: String? = null,
    val width: Long? = null,
    val height: Long? = null
) : CommonParcelable

@Serializable
data class AutumnId(
    val id: String
)

@Serializable
data class AutumnError(
    val type: String
)
