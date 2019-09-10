package com.fiserv.mimik.networkRouting

import com.fiserv.mimik.TapeCatalog
import com.fiserv.mimik.toJson
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FiservRouting {

    private val tapeCatalog = TapeCatalog.Instance

    /**
     * Networking call for all POSTs to "/fiserver/cbes/perform.do"
     */
    fun init(routing: Routing) {
        routing.post("/fiserver/cbes/perform.do") {
            val response = tapeCatalog.processCall(call) {
                call.request.queryParameters["opId"] ?: ""
            }

            val contentType = response.header("content-type") ?: "text/plain"
            val code = HttpStatusCode.fromValue(response.code())

            call.respondText(ContentType.parse(contentType), code) {
                withContext(Dispatchers.IO) {
                    response.toJson()
                }
            }
        }
    }
}
