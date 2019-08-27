package com.fiserv.ktmimic

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

object Networking {
    private val client = OkHttpClient()

    fun getData(request: Request): Response {
        val outRequest = request.newBuilder()
            .header("HOST", request.url().host())
            .build()

        return client.newCall(outRequest).execute()
    }
}
