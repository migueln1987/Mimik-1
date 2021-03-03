package com.github.kittinunf.fuel.core

import io.ktor.http.*
import okhttp3.RequestData
import okhttp3.ResponseData
import okhttp3.reQuery
import java.nio.charset.Charset
import javax.xml.bind.DatatypeConverter

val Headers.toOkHeaders: okhttp3.Headers
    get() = okhttp3.Headers.Builder().also { builder ->
        entries.forEach { hKV ->
            hKV.value.forEach {
                if (builder[hKV.key] != it)
                    builder.add(hKV.key, it)
            }
        }
    }.build()

val Request.toRequestData: RequestData
    get() = RequestData {
        it.method = method.value
        it.url = url.toString()
        it.headers = headers.toOkHeaders
        it.httpUrl.reQuery(parameters.asSequence())
        it.body = body.asString(null)
    }

/**
 * Converts a (fuel) [Response] to [ResponseData]
 */
val Response.toResponseData: ResponseData
    get() = ResponseData { newResponse ->
        newResponse.code = statusCode
        newResponse.headers = headers.toOkHeaders

        val data = body().toByteArray()
        val isImage = headers[HttpHeaders.ContentType].any { it.startsWith("image") }
        newResponse.body = if (isImage)
            DatatypeConverter.printBase64Binary(data)
        else
            data.toString(Charset.defaultCharset())
    }
