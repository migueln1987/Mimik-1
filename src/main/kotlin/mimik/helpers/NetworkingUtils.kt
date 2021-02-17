@file:Suppress("SpellCheckingInspection", "KDocUnresolvedReference", "unused", "RemoveRedundantQualifierName")

package mimik.helpers

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpGet
import mimik.helpers.attractors.RequestAttractors
import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.request.*
import io.ktor.util.StringValues
import io.ktor.util.filter
import io.ktor.util.toMap
import kotlinUtils.isFalse
import kotlinUtils.isValidJSON
import kotlinUtils.tryOrNull
import mimik.TapeCatalog
import okhttp3.RequestData
import okhttp3.ResponseData
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.internal.http.HttpMethod
import okio.Buffer
import okreplay.toTapeData
import org.w3c.dom.NodeList
import java.nio.charset.Charset
import java.util.TreeMap

// == okHttp3
/**
 * [text/plain] Media Type
 */
val MediaType.Companion.Text_Plain: MediaType?
    get() = "text/plain".toMediaTypeOrNull()

val okhttp3.Response.toJson: String
    get() = tryOrNull {
        body?.byteStream()?.let { stream ->
            (Parser.default().parse(stream) as JsonObject)
                .toJsonString(prettyPrint = true, canonical = true)
        }
    }.orEmpty()

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
 * toMultiMap function which can preserve the case of header keys
 */
fun Headers.toMultimap(caseSensitive: Boolean): Map<String, List<String>> {
    if (!caseSensitive) return this.toMultimap()

    val result = TreeMap<String, ArrayList<String>>()
    (0 until size).forEach { i ->
        val name = name(i)
        if (!result.containsKey(name))
            result[name] = ArrayList()
        val data = result.getValue(name)
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

/**
 * Creates a [Headers] which can have multiple different values to the same key
 */
val Map<String, List<String>>.toHeaders_dupKeys: Headers
    get() {
        return Headers.Builder().also { build ->
            forEach { (key, values) ->
                values.forEach { value ->
                    build.add(key, value)
                }
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
    get() = if (size > 0) this else null

fun Headers.contains(key: String, value: String) = values(key).contains(value)

/** Returns an immutable (optional) case-sensitive set of header names. */
fun Headers.names(caseSensitive: Boolean): Set<String> {
    return if (caseSensitive) {
        (0 until size).map { name(it) }.toSet()
    } else names()
}

/**
 * Converts the [Headers] into a list of Key/Value pairs
 */
val Headers.toPairs: List<Pair<String, String>>
    get() = toMultimap().asSequence()
//        .filter { it.key != null }
        .flatMap { kv ->
            kv.value.asSequence().map { kv.key to it }
        }
        .toList()

val Headers.toArray: Array<String>
    get() {
        val result = mutableListOf<String>()
        toPairs.forEach {
            result.add(it.first)
            result.add(it.second)
        }
        return result.toTypedArray()
    }

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

/**
 * Converts a [okhttp3.Request] to [okreplay.Request]
 */
val okhttp3.Request.toReplayRequest: okreplay.Request
    get() {
        val newRequest = newBuilder().build()
        val contentCharset = newRequest.body?.contentType()?.charset()
            ?: Charset.forName("UTF-8")
        val bodyData = newRequest.body?.content()

        return object : okreplay.Request {
            override fun method() = newRequest.method
            override fun url() = newRequest.url

            override fun headers() = newRequest.headers
            override fun header(name: String) = newRequest.headers[name]
            override fun getContentType() = newRequest.headers[HttpHeaders.ContentType]

            override fun getCharset() = contentCharset
            override fun getEncoding() = charset.name()

            override fun hasBody() = bodyData.isNullOrEmpty().isFalse
            override fun body() = bodyData?.toByteArray() ?: byteArrayOf()
            override fun bodyAsText() = bodyData.orEmpty()

            override fun newBuilder() = TODO()
            override fun toYaml() = TODO()
        }
    }

/**
 * Converts a [okhttp3.Request] to [RequestData]
 */
val okhttp3.Request.toTapeData: RequestData
    get() = this.toReplayRequest.toTapeData

val okhttp3.Response.toReplayResponse: okreplay.Response
    get() {
        val newResponse = newBuilder().build()
        val contentCharset = newResponse.body?.contentType()?.charset()
            ?: Charset.forName("UTF-8")
        val bodyData = newResponse.body?.content()

        return object : okreplay.Response {
            override fun code() = newResponse.code
            override fun protocol() = newResponse.protocol

            override fun headers() = newResponse.headers
            override fun header(name: String) = newResponse.headers[name]
            override fun getContentType() = newResponse.headers[HttpHeaders.ContentType]

            override fun getCharset() = contentCharset
            override fun getEncoding() = charset.name()

            override fun hasBody() = bodyData.isNullOrEmpty().isFalse
            override fun body() = bodyData?.toByteArray() ?: byteArrayOf()
            override fun bodyAsText() = bodyData.orEmpty()

            override fun newBuilder() = TODO()
            override fun toYaml() = TODO()
        }
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
        if (HttpMethod.requiresRequestBody(request.method))
            it.body(
                (if (TapeCatalog.isTestRunning) request.body.content() else "")
                    .toResponseBody(MediaType.Text_Plain)
            )
        it.message(status.description)
    }.build()
}

inline fun OkHttpClient.newCallRequest(builder: (Request.Builder) -> Unit): okhttp3.Response? {
    val requestBuilder = Request.Builder().also(builder)
    val request = tryOrNull { requestBuilder.build() } ?: return null
    return newCall(request).execute()
}

/**
 * Returns the hashCode of this [Request]'s toString.
 *
 * If another [Request] has the same method + url + headers + body, then the contentHash will be the same
 */
val okhttp3.Request.contentHash: Int
    get() {
        val filterHeaders = headers.asIterable()
            .filterNot { h -> RequestAttractors.skipHeaders.any { h.first == it } }
            .joinToString(separator = "\n") { it.first + ": " + it.second }

        return "%s%s%s%s".format(
            method,
            url.toString(),
            filterHeaders,
            body.content()
        ).hashCode()
    }

val String.asMediaType: MediaType?
    get() = this.toMediaTypeOrNull()

val okhttp3.Response.toTapeData: ResponseData
    get() = toReplayResponse.toTapeData

operator fun okhttp3.Headers.Builder.invoke(action: (okhttp3.Headers.Builder) -> Unit): okhttp3.Headers.Builder {
    action(this)
    return this
}
// == end okHttp3

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
            list.forEach { headerCache[s] = it }
        }

        // resolve what host would be taking to
        val localHosts = listOf("0.0.0.0", "10.0.2.2")
        if (localHosts.any { headerCache["host"].orEmpty().startsWith(it) })
            headerCache["host"] = outboundHost

        build.headers(headerCache.build())

        build.method(
            request.httpMethod.value,
            if (HttpMethod.requiresRequestBody(request.httpMethod.value))
                requestBody.orEmpty()
                    .toRequestBody(request.contentType().toString().toMediaTypeOrNull())
            else null
        )
    }.build()
}

/**
 * Returns the [Parameters] which this [ApplicationCall] provides.
 *
 * Supports single and MultiPart type calls.
 */
suspend fun ApplicationCall.anyParameters(): Parameters {
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
            .toJsonString(prettyPrint = true, canonical = true)
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

// == end fuel
