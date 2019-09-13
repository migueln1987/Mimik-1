package tapeItems.helpers

import Networking
import Project.outboundUrl
import io.ktor.application.ApplicationCall
import io.ktor.request.contentType
import io.ktor.request.httpMethod
import io.ktor.request.receiveText
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

suspend fun ApplicationCall.toChain(): Interceptor.Chain {
    val requestBody = receiveText()

    fun getOKRequest(): Request {
        return Request.Builder().also { build ->
            build.url(
                "%s://%s%s".format(outboundUrl.scheme(), outboundUrl.host(), request.local.uri)
            )

            val headerCache = Headers.Builder()
            request.headers.forEach { s, list ->
                list.forEach { headerCache.set(s, it) }
            }

            // resolve what host would be taking to
            if ((headerCache.get("Host") ?: "").startsWith("0.0.0.0"))
                headerCache.set("Host", outboundUrl.host())

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

    return object : Interceptor.Chain {
        override fun request() = getOKRequest()

        override fun proceed(request: Request) = Networking.getData(request)

        override fun writeTimeoutMillis() = TODO()
        override fun call() = TODO()
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = TODO()
        override fun connectTimeoutMillis() = TODO()
        override fun connection() = TODO()
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = TODO()
        override fun withReadTimeout(timeout: Int, unit: TimeUnit) = TODO()
        override fun readTimeoutMillis() = TODO()
    }
}
