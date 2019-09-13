package networkRouting

import TapeCatalog
import helpers.toJson
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.post

/**
 * Networking call for all POSTs to "/fiserver/cbes/perform.do"
 */
@Deprecated("Convert into a tape")
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
