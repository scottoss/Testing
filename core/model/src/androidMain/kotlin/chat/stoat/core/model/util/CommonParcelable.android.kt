package chat.stoat.core.model.util

import java.io.Serializable

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
actual annotation class CommonParcelize actual constructor()

actual typealias CommonParcelable = Serializable
