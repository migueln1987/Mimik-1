package com.fiserv.ktmimic.tapeTypes.helpers

import com.fiserv.ktmimic.Networking
import com.fiserv.ktmimic.Project.outboundUrl
import io.ktor.application.ApplicationCall
import io.ktor.request.contentType
import io.ktor.request.httpMethod
import io.ktor.request.receiveText
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

            request.headers.forEach { s, list ->
                list.forEach { build.header(s, it) }
            }

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
