package com.fiserv.mimik

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import okreplay.Request
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.lang.StringBuilder
import java.util.Collections
import java.util.RandomAccess
import java.util.NoSuchElementException
import java.util.stream.IntStream
import kotlin.streams.toList


fun Request.toJson(): String {
    val bodyString = StringBuilder()
    if (hasBody()) bodyString.append(String(body()))

    val jsonData = Parser.default().parse(bodyString) as JsonObject
    return jsonData.toJsonString(true, true)
}

fun okreplay.Response.toJson(): String {
    val bodyString = StringBuilder()
    if (hasBody()) bodyString.append(String(body()))

    val jsonData = Parser.default().parse(bodyString) as JsonObject
    return jsonData.toJsonString(true, true)
}

fun okhttp3.Response.toJson(): String {
    return (try {
        body()?.byteStream()?.let { stream ->
            (Parser.default().parse(stream) as JsonObject)
                .toJsonString(true, true)
        }
    } catch (_: Exception) {
        null
    }) ?: ""
}

fun NodeList.asList() = (0..length).mapNotNull(this::item)
