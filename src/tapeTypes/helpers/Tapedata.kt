package com.fiserv.ktmimic.tapeTypes.helpers

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import okhttp3.Headers
import okreplay.Request
import okreplay.Response
import java.lang.StringBuilder

abstract class Tapedata {
    lateinit var headers: Headers
    lateinit var body: String

    fun Request.toJson(): String {
        val bodyString = StringBuilder()
        if (hasBody()) bodyString.append(String(body()))

        val jsonData = Parser.default().parse(bodyString) as JsonObject
        return jsonData.toJsonString(true, true)
    }

    fun Response.toJson(): String {
        val bodyString = StringBuilder()
        if (hasBody()) bodyString.append(String(body()))

        val jsonData = Parser.default().parse(bodyString) as JsonObject
        return jsonData.toJsonString(true, true)
    }
}
