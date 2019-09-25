package helpers

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import io.ktor.application.ApplicationCall
import io.ktor.request.contentType
import io.ktor.request.httpMethod
import io.ktor.request.receiveText
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.internal.http.HttpMethod
import okio.Buffer
import org.w3c.dom.NodeList
import java.nio.charset.Charset

/**
 * Returns the body, if any, or [default] when null
 */
fun okreplay.Request.tryGetBody(default: String = "") =
    if (hasBody()) bodyAsText() else default

/**
 * Returns the body, if any, or [default] when null
 */
fun okreplay.Response.tryGetBody(default: String = "") =
    if (hasBody()) bodyAsText() else default

fun StringBuilder.toJson(): String {
    return if (toString().isJSONValid) {
        (Parser.default().parse(this) as JsonObject)
            .toJsonString(true, true)
    } else ""
}

fun okhttp3.Response.toJson(): String {
    return (try {
        body()?.byteStream()?.let { stream ->
            (Parser.default().parse(stream) as JsonObject)
                .toJsonString(true, true)
        }
    } catch (_: Exception) {
        null
    }) ?: ""
}

fun NodeList.asList() = (0..length).mapNotNull(this::item)

suspend fun ApplicationCall.toOkRequest(outboundHost: String = "local.host"): okhttp3.Request {
    val requestBody = when {
        HttpMethod.requiresRequestBody(request.httpMethod.value) -> {
            try {
                receiveText()
            } catch (e: Exception) {
                System.out.println(e)
                ""
            }
        }
        else -> null
    }

    return okhttp3.Request.Builder().also { build ->
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
            if (HttpMethod.requiresRequestBody(request.httpMethod.value))
                RequestBody.create(
                    MediaType.parse(request.contentType().toString()),
                    requestBody ?: ""
                )
            else null
        )
    }.build()
}

fun okhttp3.Request.reHost(outboundHost: HttpUrl): okhttp3.Request {
    return newBuilder().also { build ->
        build.url(
            "%s://%s%s%s".format(
                outboundHost.scheme(),
                outboundHost.host(),
                url().encodedPath(),
                if (url().querySize() > 0) {
                    "?" + url().query()
                } else ""
            )
        )
    }.build()
}

/**
 * Returns the string contents of a RequestBody
 */
val RequestBody?.content: String
    get() = this?.let { _ ->
        Buffer().let { buffer ->
            writeTo(buffer)
            val charset: Charset = contentType()?.charset() ?: Charset.defaultCharset()
            buffer.readString(charset)
        }
    } ?: ""

val ResponseBody?.content: String
    get() {
        return try {
            this?.let { string() } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
