package networkRouting

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import tapeItems.helpers.content

class CallProcessor(path: String) : RoutingContract(path) {

    private val tapeCatalog = TapeCatalog.Instance

    override fun init(route: Routing) {
        route.route(path) {
            get { call.action() }
            post { call.action() }
            intercept(ApplicationCallPipeline.Call) {
                val ee = this
            }
        }
    }

    private suspend fun ApplicationCall.action() {
        val response = tapeCatalog.processCall(this)
        val contentType = response.header("content-type") ?: "text/plain"
        val code = HttpStatusCode.fromValue(response.code())

        respondText(ContentType.parse(contentType), code) {
            response.body().content
        }
    }
}
