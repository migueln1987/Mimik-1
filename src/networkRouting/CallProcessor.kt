package networkRouting

import helpers.appendHeaders
import helpers.content
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.*

class CallProcessor : RoutingContract("{...}") {

    override fun init(route: Route) {
        route.route(path) {
            get { call.action() }
            post { call.action() }
        }
    }

    private suspend fun ApplicationCall.action() {
        val processResponse = tapeCatalog.processCall(this)
        val contentType = processResponse.header(HttpHeaders.ContentType) ?: "text/plain"
        val code = HttpStatusCode.fromValue(processResponse.code())

        response.headers.appendHeaders(processResponse.headers())
        respondText(ContentType.parse(contentType), code) {
            processResponse.body().content()
        }
    }
}
