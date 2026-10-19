package chat.stoat.api.routes.microservices.january

import chat.stoat.api.StoatAPI
import java.net.URLEncoder

fun asJanuaryProxyUrl(url: String): String {
    return "${StoatAPI.getJanuaryUrl()}/proxy?url=${URLEncoder.encode(url, "utf-8")}"
}
