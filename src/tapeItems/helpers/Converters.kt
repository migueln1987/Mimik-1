package tapeItems.helpers

import io.ktor.application.ApplicationCall
import io.ktor.request.contentType
import io.ktor.request.httpMethod
import io.ktor.request.receiveText
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import java.nio.charset.Charset

suspend fun ApplicationCall.toOkRequest(outboundHost: String = "blank.com"): Request {
    val requestBody = try {
        receiveText()
    } catch (e: Exception) {
        System.out.println(e)
        ""
    }

    return Request.Builder().also { build ->
        build.url(
            "%s://%s%s".format(request.local.scheme, outboundHost, request.local.uri)
        )

        val headerCache = Headers.Builder()
        request.headers.forEach { s, list ->
            list.forEach { headerCache.set(s, it) }
        }

        // resolve what host would be taking to
        if ((headerCache.get("Host") ?: "").startsWith("0.0.0.0"))
            headerCache.set("Host", outboundHost)

        build.headers(headerCache.build())

        build.method(
            request.httpMethod.value,
            RequestBody.create(
                MediaType.parse(request.contentType().toString()),
                requestBody
            )
        )
    }.build()
}

fun Request.reHost(outboundHost: HttpUrl): Request {
    return newBuilder().also { build ->
        build.url(
            "%s://%s%s".format(
                outboundHost.scheme(),
                outboundHost.host(),
                url().encodedPath()
            )
        )
    }.build()
}

/**
 * Returns the string contents of a RequestBody
 */
val RequestBody.content: String
    get() = Buffer().run {
        writeTo(this)
        val charset: Charset = contentType()?.charset() ?: Charset.defaultCharset()
        readString(charset)
    }
