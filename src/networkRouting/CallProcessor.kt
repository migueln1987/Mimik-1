package networkRouting

import helpers.appendHeaders
import helpers.asContentType
import helpers.content
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.routing.*
import javax.xml.bind.DatatypeConverter

class CallProcessor : RoutingContract("{...}") {

    override fun init(route: Route) {
        route.route(path) {
            get { call.action() }
            post { call.action() }
        }
    }

    private suspend fun ApplicationCall.action() {
        val processResponse = tapeCatalog.processCall(this)
        val contentType = (processResponse.header(HttpHeaders.ContentType) ?: "text/plain")
            .asContentType
        val code = HttpStatusCode.fromValue(processResponse.code())

        response.headers.appendHeaders(processResponse.headers())
        val content = processResponse.body().content()
        when {
            contentType.contentType == "image" -> {
                val data = DatatypeConverter.parseBase64Binary(content)
                respondBytes(contentType, code) { data }
            }
            else ->
                respondText(contentType, code) { content }
        }
    }
}
