@file:Suppress("SpellCheckingInspection", "KDocUnresolvedReference", "unused", "RemoveRedundantQualifierName")

package mimik.helpers

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpGet
import mimik.helpers.attractors.Attractor
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import kotlinx.isFalse
import kotlinx.isValidJSON
import kotlinx.tryOrNull
import mimik.Localhost
import mimik.tapeItems.TapeCatalog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.internal.http.HttpMethod
import okreplay.toTapeData
import org.w3c.dom.NodeList
import java.nio.charset.Charset

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
        val bodyData = newResponse.body?.contents()

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
            .filterNot { h -> Attractor.skipHeaders.any { h.first == it } }
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
suspend fun ApplicationCall.toOkRequest(outboundHost: String = "local.host"): okhttp3.Request {
    val requestBody = tryGetBody()

    return okhttp3.Request.Builder().also { build ->
        build.url(
            "%s://%s%s".format(request.local.scheme, outboundHost, request.local.uri)
        )

        val headerCache = okhttp3.Headers.Builder()
        request.headers.forEach { s, list ->
            list.forEach { headerCache[s] = it }
        }

        // resolve what host would be taking to
        if (Localhost.All.any { headerCache["host"].orEmpty().startsWith(it) })
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
