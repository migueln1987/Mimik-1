package com.fiserv.mimik.networkRouting

import com.fiserv.mimik.TapeCatalog
import com.fiserv.mimik.toJson
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.post

/**
 * Networking call for all POSTs to "/fiserver/cbes/perform.do"
 */
class FiservRouting(path: String) : RoutingContract(path) {

    private val tapeCatalog = TapeCatalog.Instance

    override fun init(route: Routing) {
        route.apply {
            post(path) {
                val response = tapeCatalog.processCall(call)
                val contentType = response.header("content-type") ?: "text/plain"
                val code = HttpStatusCode.fromValue(response.code())

                call.respondText(ContentType.parse(contentType), code) {
                    response.toJson()
                }
            }
        }
    }
}
