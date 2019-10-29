package helpers

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.contentType
import io.ktor.request.httpMethod
import io.ktor.request.receiveText
import io.ktor.response.ResponseHeaders
import io.ktor.util.StringValues
import io.ktor.util.filter
import mimikMockHelpers.Requestdata
import mimikMockHelpers.Responsedata
import okhttp3.*
import okhttp3.internal.http.HttpMethod
import okio.Buffer
import org.w3c.dom.NodeList
import java.io.IOException
import java.nio.charset.Charset

// == okHttp3
val okhttp3.Response.toJson: String
    get() {
        return try {
            body()?.byteStream()?.let { stream ->
                (Parser.default().parse(stream) as JsonObject)
                    .toJsonString(true, true)
            }
        } catch (_: Exception) {
            null
        }.orEmpty()
    }

/**
 * Returns the string contents of a RequestBody
 */
fun RequestBody?.content(default: String = ""): String {
    return this?.let { _ ->
        Buffer().let { buffer ->
            writeTo(buffer)
            val charset: Charset = contentType()?.charset() ?: Charset.defaultCharset()
            buffer.readString(charset)
        }
    } ?: default
}

fun ResponseBody?.content(default: String = ""): String {
    return try {
        this?.string().orEmpty()
    } catch (e: Exception) {
        default
    }
}

val StringValues.toHeaders: Headers
    get() {
        return Headers.Builder().also { build ->
            entries().forEach { entry ->
                entry.value.forEach { value ->
                    build.add(entry.key, value)
                }
            }
        }.build()
    }

val StringValues.toParameters: Parameters
    get() = Parameters.build { appendAll(this@toParameters) }

/**
 * Limits the input [StringValues] to only those within the [items] list.
 */
fun StringValues.limit(items: List<String>, allowDuplicates: Boolean = false): Parameters {
    val limitParams: MutableList<String> = mutableListOf()

    return filter { s, _ ->
        s.toLowerCase().let { pKey ->
            if (items.contains(pKey)) {
                if (limitParams.contains(pKey) && !allowDuplicates)
                    return@filter false
                limitParams.add(pKey)
                return@filter true
            } else false
        }
    }.toParameters
}

val Map<String, String>.toHeaders: Headers
    get() {
        return Headers.Builder().also { build ->
            forEach { entry ->
                build.add(entry.key, entry.value)
            }
        }.build()
    }

/**
 * Returns the value of [Headers] if it contains any values, or [null]
 */
val Headers.valueOrNull: Headers?
    get() = if (size() > 0) this else null

fun ResponseHeaders.appendHeaders(headers: okhttp3.Headers) {
    headers.toMultimap().forEach { t, u ->
        u.forEach {
            if (!HttpHeaders.isUnsafe(t))
                append(t, it)
        }
    }
}

fun Headers.contains(key: String, value: String) = values(key).contains(value)

fun HttpUrl.containsPath(vararg path: String) =
    pathSegments().containsAll(path.toList())

/**
 * Returns a brief okHttp response to respond with a defined response [status] and [message]
 */
fun okhttp3.Request.makeCatchResponse(
    status: HttpStatusCode,
    message: () -> String = { "" }
): okhttp3.Response {
    return okhttp3.Response.Builder().also {
        it.request(this)
        it.protocol(Protocol.HTTP_1_1)
        it.code(status.value)
        it.message(message.invoke())
    }.build()
}

fun okhttp3.Request.reHost(outboundHost: HttpUrl?): okhttp3.Request {
    return newBuilder().also { build ->
        if (outboundHost != null) {
            val newUrl = HttpUrl.parse(
                "%s://%s%s%s".format(
                    outboundHost.scheme(),
                    outboundHost.host(),
                    url().encodedPath(),
                    if (url().querySize() > 0) "?" + url().query() else ""
                )
            )

            if (newUrl != null) {
                build.url(newUrl)
                build.header("HOST", newUrl.host())
            }
        }
    }.build()
}

/**
 * Converts a [okhttp3.Request] to [okreplay.Request]
 */
val okhttp3.Request.toReplayRequest: okreplay.Request
    get() {
        val newRequest = newBuilder().build()
        val contentCharset = newRequest.body()?.contentType()?.charset()
            ?: Charset.forName("UTF-8")
        val bodyData = newRequest.body()?.content()

        return object : okreplay.Request {
            override fun method() = newRequest.method()
            override fun url() = newRequest.url()

            override fun headers() = newRequest.headers()
            override fun header(name: String) = headers().get(name)
            override fun getContentType() = headers().get(HttpHeaders.ContentType)

            override fun getCharset() = contentCharset
            override fun getEncoding() = charset.name()

            override fun hasBody() = bodyData.isNullOrEmpty().isFalse()
            override fun body() = bodyData?.toByteArray() ?: byteArrayOf()
            override fun bodyAsText() = bodyData.orEmpty()

            override fun newBuilder() = TODO()
            override fun toYaml() = TODO()
        }
    }

/**
 * Converts a [okhttp3.Request] to [Requestdata]
 */
val okhttp3.Request.toTapeData: Requestdata
    get() = this.toReplayRequest.toTapeData

val okhttp3.Response.toReplayResponse: okreplay.Response
    get() {
        val newResponse = newBuilder().build()
        val contentCharset = newResponse.body()?.contentType()?.charset()
            ?: Charset.forName("UTF-8")
        val bodyData = newResponse.body()?.content()

        return object : okreplay.Response {
            override fun code() = newResponse.code()
            override fun protocol() = newResponse.protocol()

            override fun headers() = newResponse.headers()
            override fun header(name: String) = headers().get(name)
            override fun getContentType() = headers().get(HttpHeaders.ContentType)

            override fun getCharset() = contentCharset
            override fun getEncoding() = charset.name()

            override fun hasBody() = bodyData.isNullOrEmpty().isFalse()
            override fun body() = bodyData?.toByteArray() ?: byteArrayOf()
            override fun bodyAsText() = bodyData.orEmpty()

            override fun newBuilder() = TODO()
            override fun toYaml() = TODO()
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

// == okreplay

/**
 * Returns the body, if any, or [default] when null
 */
fun okreplay.Request.tryGetBody(default: String = ""): String? {
    return if (HttpMethod.requiresRequestBody(method())) {
        if (hasBody()) bodyAsText() else default
    } else null
}

/**
 * Attempts to parse the [okreplay.Request] as a [RequestBody], otherwise [default] is returned.
 * Note: The process is skipped if the [okreplay.Request] method does not require a body
 */
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

/**
 * Converts the [okreplay.Request] to [okhttp3.Request]
 */
val okreplay.Request.toOkRequest: okhttp3.Request
    get() {
        return okhttp3.Request.Builder().also { builder ->
            builder.url(url())
            builder.headers(headers())
            builder.method(method(), asRequestBody())
        }.build()
    }

/**
 * Converts the [okreplay.Request] to [Requestdata]
 */
val okreplay.Request.toTapeData: Requestdata
    get() = Requestdata(this)

/**
 * Converts the [okreplay.Response] to [Responsedata]
 */
val okreplay.Response.toTapeData: Responsedata
    get() = Responsedata(this)

// ktor
suspend fun ApplicationCall.toOkRequest(outboundHost: String = "local.host"): okhttp3.Request {
    val requestBody = when {
        HttpMethod.requiresRequestBody(request.httpMethod.value) -> {
            try {
                receiveText()
            } catch (e: Exception) {
                println("ApplicationCall.toOkRequest \n$e")
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
        if ((headerCache.get("Host").orEmpty()).startsWith("0.0.0.0"))
            headerCache.set("Host", outboundHost)

        build.headers(headerCache.build())

        build.method(
            request.httpMethod.value,
            if (HttpMethod.requiresRequestBody(request.httpMethod.value))
                RequestBody.create(
                    MediaType.parse(request.contentType().toString()),
                    requestBody.orEmpty()
                )
            else null
        )
    }.build()
}

// == mimik
/**
 * Returns if the response
 */
val Responsedata?.isImage: Boolean
    get() = if (this == null) false else
        tapeHeaders.get(HttpHeaders.ContentType)?.contains("image").isTrue()

// == Others
fun StringBuilder.toJson(): String {
    return if (toString().isValidJSON) {
        (Parser.default().parse(this) as JsonObject)
            .toJsonString(true, true)
    } else ""
}

fun NodeList.asList() = (0..length).mapNotNull(this::item)
