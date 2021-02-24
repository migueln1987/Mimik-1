package io.ktor.application

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import okhttp3.internal.http.HttpMethod

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
