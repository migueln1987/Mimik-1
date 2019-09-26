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
import java.io.IOException
import java.nio.charset.Charset

/**
 * Returns the body, if any, or [default] when null
 */
fun okreplay.Request.tryGetBody(default: String = ""): String? {
    return if (HttpMethod.requiresRequestBody(method())) {
        if (hasBody()) bodyAsText() else default
    } else null
}

fun okreplay.Request.asRequestBody(default: String = ""): RequestBody? {
    return if (HttpMethod.requiresRequestBody(method())) {
        RequestBody.create(
            MediaType.parse(contentType),
            if (hasBody()) bodyAsText() else default
        )
    } else null
}

/**
 * Returns the body, if any, or [default] when null
 */
fun okreplay.Response.tryGetBody(default: String = ""): String =
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
            this?.string() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

val okreplay.Request.toOkRequest: okhttp3.Request
    get() {
        return okhttp3.Request.Builder().also { builder ->
            builder.url(url())
            builder.headers(headers())
            builder.method(method(), asRequestBody())
        }.build()
    }

fun cloneResponseBody(responseBody: ResponseBody): ResponseBody {
    try {
        val source = responseBody.source()
        source.request(java.lang.Long.MAX_VALUE)
        return ResponseBody.create(
            responseBody.contentType(), responseBody.contentLength(),
            source.buffer.clone()
        )
    } catch (e: IOException) {
        throw RuntimeException("Failed to read response body", e)
    }
}

fun ResponseBody?.clone(): ResponseBody? {
    try {
        if (this == null) return null
        val source = source()
        source.request(java.lang.Long.MAX_VALUE)
        return ResponseBody.create(
            contentType(), contentLength(),
            source.buffer.clone()
        )
    } catch (e: IOException) {
        return null
    }
}

fun toReplayResponse(response: okhttp3.Response): okreplay.Response {
    val input = response.newBuilder().build()
    val bodyData = input.body()?.bytes()
    return object : okreplay.Response {
        override fun code() = input.code()
        override fun protocol() = input.protocol()

        override fun getEncoding() = charset.name()
        override fun getCharset() = Charset.forName("UTF-8")

        override fun headers() = input.headers()
        override fun header(name: String) = headers().get(name)
        override fun getContentType() = headers().get("Content-Type")

        override fun hasBody() = bodyData != null
        override fun body() = bodyData ?: byteArrayOf()
        override fun bodyAsText() = bodyData?.toString(charset) ?: "missing Data"

        override fun newBuilder() = TODO()
        override fun toYaml() = TODO()
    }
}
