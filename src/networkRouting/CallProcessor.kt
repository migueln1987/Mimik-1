package networkRouting

import helpers.appendHeaders
import helpers.content
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route

class CallProcessor(path: String) : RoutingContract(path) {

    private val tapeCatalog by lazy { TapeCatalog.Instance }

    override fun init(route: Routing) {
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
