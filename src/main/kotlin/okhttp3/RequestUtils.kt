package okhttp3

import io.ktor.http.*
import kotlinUtils.tryOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Returns a brief okHttp response to respond with a defined response [status] and [message]
 */
inline fun Request.createResponse(
    status: HttpStatusCode,
    message: () -> String = { "" }
): Response {
    return Response.Builder().also {
        it.request(this)
        it.protocol(Protocol.HTTP_1_1)
        it.code(status.value.coerceAtLeast(0))
        it.message(tryOrNull { message.invoke() }.orEmpty())
    }.build()
}

fun Request.reHost(outboundHost: HttpUrl?): Request {
    return newBuilder().also { build ->
        if (outboundHost != null) {
            "%s://%s%s%s".format(
                outboundHost.scheme,
                outboundHost.host,
                url.encodedPath,
                if (url.querySize > 0) "?" + url.query else ""
            ).toHttpUrlOrNull()?.also { newUrl ->
                build.url(newUrl)
                build.header("HOST", newUrl.host)
            }
        }
    }.build()
}
