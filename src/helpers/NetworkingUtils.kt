package helpers

import TapeCatalog
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpGet
import helpers.attractors.RequestAttractors
import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.request.*
import io.ktor.response.ResponseHeaders
import io.ktor.util.StringValues
import io.ktor.util.filter
import io.ktor.util.toMap
import mimikMockHelpers.Requestdata
import mimikMockHelpers.Responsedata
import okhttp3.*
import okhttp3.internal.http.HttpMethod
import okio.Buffer
import org.w3c.dom.NodeList
import java.io.IOException
import java.nio.charset.Charset
import java.util.TreeMap
import javax.xml.bind.DatatypeConverter

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
 * Returns the string contents of a RequestBody, or [default] in the case the body is empty
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

/**
 * Returns the [ResponseBody]'s body as a string. Images are converted to Base64 (if not already)
 *
 * !! This action clears the origional data's body !!
 */
fun ResponseBody?.content(default: String = ""): String {
    return if (this == null) default
    else try {
        val data = tryOrNull { use { bytes() } }
        if (data == null) return ""
        val dataStr = String(data)
        val isBase64 = dataStr.isBase64

        if (!isBase64 && contentType()?.type() == "image")
            DatatypeConverter.printBase64Binary(data)
        else
            dataStr
    } catch (e: Exception) {
        default
    }
}

/**
 * toMultiMap function which can preserve the case of header keys
 */
fun Headers.toMultimap(caseSensitive: Boolean): Map<String, List<String>> {
    if (!caseSensitive) return this.toMultimap()

    var result = TreeMap<String, ArrayList<String>>()
    (0..size() - 1).forEach { i ->
        val name = name(i)
        if (!result.containsKey(name))
            result[name] = ArrayList()
        var data = result.getValue(name)
        data.add(value(i))
    }

    return result
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

val Map<String, String>.toHeaders: Headers
    get() {
        return Headers.Builder().also { build ->
            forEach { entry ->
                build.add(entry.key, entry.value)
            }
        }.build()
    }

val Iterable<Pair<String, String>>.toHeaders: Headers
    get() {
        return Headers.Builder()
            .also { build -> forEach { build.add(it.first, it.second) } }
            .build()
    }

/**
 * Returns the value of [Headers] if it contains any values, or [null]
 */
val Headers.valueOrNull: Headers?
    get() = if (size() > 0) this else null

/**
 * Appends the data from [headers] to this [ResponseHeaders]
 */
fun ResponseHeaders.append(headers: okhttp3.Headers) =
    append(headers.toMultimap())

/**
 * Appends the data from [headers] to this [ResponseHeaders]
 */
fun ResponseHeaders.append(headers: Map<String, List<String>>) {
    headers.forEach { (t, u) ->
        u.forEach { isThrow { append(t, it) } }
    }
}

fun ResponseHeaders.append(vararg values: Pair<String, String>) =
    values.forEach { isThrow { append(it.first, it.second) } }

fun ResponseHeaders.append(key: String, vararg values: String) =
    values.forEach { isThrow { append(key, it) } }

fun ResponseHeaders.append(key: String, values: List<String>) =
    isThrow { values.forEach { isThrow { append(key, it) } } }

fun Headers.contains(key: String, value: String) = values(key).contains(value)

/**
 * Converts the [Headers] into a list of Key/Value pairs
 */
val Headers.toPairs: List<Pair<String, String>>
    get() = toMultimap().asSequence()
        .filter { it.key != null }
        .flatMap { kv ->
            kv.value.asSequence().map { kv.key!! to it.orEmpty() }
        }
        .toList()

/**
 * Returns this [Headers] as a list of "Key: Value", or user defined [format]
 */
inline fun Headers.toStringPairs(
    format: (Pair<String, String>) -> String = { "${it.first}: ${it.second}" }
) = toPairs.map(format)

fun okhttp3.Headers.asIterable() = toPairs.asIterable()

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

fun HttpUrl.containsPath(vararg path: String) =
    pathSegments().containsAll(path.toList())

/**
 * Returns a brief okHttp response to respond with a defined response [status] and [message]
 */
inline fun okhttp3.Request.createResponse(
    status: HttpStatusCode,
    message: () -> String = { "" }
): okhttp3.Response {
    return okhttp3.Response.Builder().also {
        it.request(this)
        it.protocol(Protocol.HTTP_1_1)
        it.code(status.value.coerceAtLeast(0))
        it.message(tryOrNull { message.invoke() }.orEmpty())
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
    return tryOrNull {
        this?.source()?.use { source ->
            source.request(java.lang.Long.MAX_VALUE)
            ResponseBody.create(
                contentType(), contentLength(),
                source.buffer.clone()
            )
        }
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

/**
 * Returns this [HttpUrl] as the host and path in url form.
 *
 * Example: http://url.ext
 */
val HttpUrl.hostPath: String
    get() = "%s://%s%s".format(
        scheme(),
        host(),
        encodedPath()
    )

/**
 * Replaces the scheme and host in the input [HttpUrl] with the new value in newHost.
 *
 * newHost can be in the format of "url.ext" or "http://url.ext"
 */
fun HttpUrl?.reHost(newHost: String): HttpUrl? {
    val newHttpHost = HttpUrl.parse(newHost.ensureHttpPrefix)

    if (this == null || newHttpHost == null) return newHttpHost
    return newBuilder().also {
        it.scheme(newHttpHost.scheme())
        it.host(newHttpHost.host())
    }.build()
}

/**
 * Replaces/ appends [newPort] to this [HttpUrl]
 */
fun HttpUrl?.rePort(newPort: Int): HttpUrl? {
    return this?.let {
        newBuilder().also {
            it.port(newPort)
        }.build()
    }
}

/**
 * Removes all the current query parameters from [HttpUrl], and applies the items from [newQuerys].
 *
 * [append]: When true, items are appended instead of clearing before [newQuerys] is added.
 */
fun HttpUrl?.reQuery(newQuerys: Sequence<Pair<String, Any?>>?, append: Boolean = false): HttpUrl? {
    if (this == null) return null
    return newBuilder().also { builder ->
        if (!append)
            queryParameterNames().forEach {
                builder.removeAllQueryParameters(it)
            }
        newQuerys?.forEach {
            builder.addQueryParameter(it.first, it.second.toString())
        }
    }.build()
}

/**
 * Returns a Response from the given request.
 *
 * Note: if [isTestRunning] is true, the response body will contain the request body
 */
fun miniResponse(
    request: okhttp3.Request,
    status: HttpStatusCode = HttpStatusCode.OK
): okhttp3.Response {
    return okhttp3.Response.Builder().also {
        it.request(request)
        it.protocol(Protocol.HTTP_1_1)
        it.code(status.value)
        it.header(HttpHeaders.ContentType, "text/plain")
        if (HttpMethod.requiresRequestBody(request.method()))
            it.body(
                ResponseBody.create(
                    MediaType.parse("text/plain"),
                    if (TapeCatalog.isTestRunning) request.body().content() else ""
                )
            )
        it.message(status.description)
    }.build()
}

inline fun OkHttpClient.newCallRequest(builder: (Request.Builder) -> Unit): okhttp3.Response? {
    val requestBuilder = Request.Builder()
        .also { builder.invoke(it) }

    val request = try {
        requestBuilder.build()
    } catch (_: Exception) {
        null
    } ?: return null

    return newCall(request).execute()
}

/**
 * Returns the hashCode of this [Request]'s toString.
 *
 * If another [Request] has the same method + url + headers + body, then the contentHash will be the same
 */
val okhttp3.Request.contentHash: Int
    get() {
        val filterHeaders = headers().asIterable()
            .filterNot { h -> RequestAttractors.skipHeaders.any { h.first == it } }
            .map { it.first + ": " + it.second }
            .joinToString(separator = "\n")

        return "%s%s%s%s".format(
            method(),
            url().toString(),
            filterHeaders,
            body().content()
        ).hashCode()
    }

val String.asMediaType: MediaType?
    get() = MediaType.parse(this)

val okhttp3.Response.toTapeData: Responsedata
    get() = toReplayResponse.toTapeData

/**
 * Returns a list of this HttpUrl's query items in a "item=value" format
 */
fun okhttp3.HttpUrl?.queryItems(): List<String> {
    return if (this == null) listOf()
    else queryParameterNames().flatMap { name ->
        queryParameterValues(name).map { value -> "$name=$value" }
    }
}
// == end okHttp3

// == okreplay
/**
 * Returns the body, if any (and required), or [default] when null.
 *
 * Images are converted to Base64
 */
fun okreplay.Request.content(default: String = ""): String? {
    return when {
        !HttpMethod.requiresRequestBody(method()) -> null
        !hasBody() -> default
        else -> {
            val data = body()
            val dataStr = String(data)
            val isBase64 = dataStr.isBase64

            if (!isBase64 && (contentType.orEmpty()).startsWith("image"))
                DatatypeConverter.printBase64Binary(data)
            else
                dataStr
        }
    }
}

/**
 * Attempts to parse the [okreplay.Request] as a [RequestBody], otherwise [default] is returned.
 * Note: The process is skipped if the [okreplay.Request] method does not require a body
 */
fun okreplay.Request.asRequestBody(default: String = ""): RequestBody? {
    return if (HttpMethod.requiresRequestBody(method())) {
        RequestBody.create(
            MediaType.parse(contentType),
            content() ?: default
        )
    } else null
}

/**
 * Returns the body, if any, or [default] when null
 */
fun okreplay.Response.tryGetBody(default: String = ""): String {
    return if (hasBody()) {
        val data = body()
        val dataStr = String(data)
        val isBase64 = dataStr.isBase64

        if (!isBase64 && (contentType.orEmpty()).startsWith("image"))
            DatatypeConverter.printBase64Binary(data)
        else
            dataStr
    } else default
}

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
// == end okreplay

// == ktor
/**
 * Tries to get the body text from this request (if request supports hosting a body).
 * [default] is returned if the body is expected, but can't be recieved.
 * [null] is returned if the request does not support hosting a body
 */
suspend fun ApplicationCall.tryGetBody(default: String = ""): String? {
    return when {
        HttpMethod.requiresRequestBody(request.httpMethod.value) -> {
            try {
                receiveText()
            } catch (ex: Exception) {
                println("ApplicationCall.tryGetBody \n$ex")
                default
            }
        }
        else -> null
    }
}

suspend fun ApplicationCall.toOkRequest(outboundHost: String = "local.host"): okhttp3.Request {
    val requestBody = tryGetBody()

    return okhttp3.Request.Builder().also { build ->
        build.url(
            "%s://%s%s".format(request.local.scheme, outboundHost, request.local.uri)
        )

        val headerCache = Headers.Builder()
        request.headers.forEach { s, list ->
            list.forEach { headerCache.set(s, it) }
        }

        // resolve what host would be taking to
        val localHosts = listOf("0.0.0.0", "10.0.2.2")
        if (localHosts.any { headerCache.get("host").orEmpty().startsWith(it) })
            headerCache.set("host", outboundHost)

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

/**
 * Returns the [HttpUrl] as a [Parameters] object. Keys with no values are returned as keys with empty strings
 */
val HttpUrl?.toParameters: Parameters?
    get() {
        if (this == null) return null
        val pairs = this.queryParameterNames().asSequence()
            .filterNotNull().flatMap { name ->
                if (name.isBlank()) return@flatMap emptySequence<Pair<String, String>>()
                this.queryParameterValues(name).asSequence().map {
                    name to (it ?: "")
                }
            }

        return Parameters.build {
            pairs.forEach { append(it.first, it.second) }
        }
    }

/**
 * Returns the [Parameters] which this [ApplicationCall] provides.
 *
 * Supports single and MultiPart type calls.
 */
suspend fun ApplicationCall.anyParameters(): Parameters? {
    if (!request.isMultipart()) return parameters

    return Parameters.build {
        receiveMultipart()
            .readAllParts().asSequence()
            .filterIsInstance<PartData.FormItem>()
            .filterNot { it.name.isNullOrBlank() }
            .forEach { append(it.name!!, it.value) }
    }
}

/**
 * Returns the first values for each key
 */
val Parameters.toSingleMap: Map<String, String>
    get() = toMap().mapValues { it.value.firstOrNull().orEmpty() }

val String.asContentType: ContentType
    get() = ContentType.parse(this)
// == end ktor

// == Others
fun StringBuilder.toJson(): String {
    return if (toString().isValidJSON) {
        (Parser.default().parse(this) as JsonObject)
            .toJsonString(true, true)
    } else ""
}

fun NodeList.asList() = (0..length).mapNotNull(this::item)

val hasNetworkAccess: Boolean
    get() {
        val response = "http://google.com"
            .httpGet().responseString().second
        return (response.statusCode != -1)
    }

// == end Others

// == fuel
val com.github.kittinunf.fuel.core.Headers.toOkHeaders: okhttp3.Headers
    get() = Headers.Builder().also { builder ->
        entries.forEach { hKV ->
            hKV.value.forEach {
                if (builder.get(hKV.key) != it)
                    builder.add(hKV.key, it)
            }
        }
    }.build()

val com.github.kittinunf.fuel.core.Request.toRequestData: Requestdata
    get() = Requestdata {
        it.method = method.value
        it.url = url.toString()
        it.headers = headers.toOkHeaders
        it.httpUrl.reQuery(parameters.asSequence())
        it.body = body.asString(null)
    }

/**
 * Converts a (fuel) [Response] to [Responsedata]
 */
val com.github.kittinunf.fuel.core.Response.toResponseData: Responsedata
    get() = Responsedata {
        it.code = statusCode
        it.headers = headers.toOkHeaders

        val data = body().toByteArray()
        val isImage = headers[HttpHeaders.ContentType].any { it.startsWith("image") }
        it.body = if (isImage)
            DatatypeConverter.printBase64Binary(data)
        else
            data.toString(Charset.defaultCharset())
    }

// == end fuel
