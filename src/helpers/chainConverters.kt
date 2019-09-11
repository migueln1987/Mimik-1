package com.fiserv.mimik.helpers

import okhttp3.Interceptor
import okio.Buffer
import java.nio.charset.Charset

val Interceptor.Chain.toReplayRequest: okreplay.Request
    get() = this.request().toReplayRequest

val okhttp3.Request.toReplayRequest: okreplay.Request
    get() = object : okreplay.Request {
        override fun url() = this@toReplayRequest.url()

        override fun method() = this@toReplayRequest.method()

        override fun body(): ByteArray {
            return this@toReplayRequest.body()?.let {
                val buffer = Buffer()
                it.writeTo(buffer)
                buffer.readByteArray()
            } ?: byteArrayOf()
        }

        override fun hasBody() = body().isNotEmpty()

        override fun bodyAsText() = this@toReplayRequest.body().toString()

        override fun getContentType(): String =
            this@toReplayRequest.body()?.contentType().toString()

        override fun headers() = this@toReplayRequest.headers()

        override fun getEncoding(): String = TODO("not implemented")

        override fun getCharset() = Charset.defaultCharset()

        override fun header(name: String) = headers().get(name)

        override fun newBuilder() = TODO()
        override fun toYaml() = TODO()
    }
