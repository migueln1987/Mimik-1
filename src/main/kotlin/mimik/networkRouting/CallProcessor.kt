package mimik.networkRouting

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import mimik.tapeItems.MimikContainer
import okhttp3.contents
import okhttp3.contentType
import javax.xml.bind.DatatypeConverter

/**
 * Interaction which testing devices will use to retrieve mocked data
 *
 * @see <a href="{host}.com/{rootpath}/mock">Live</a>
 * @see <a href="0.0.0.0:2202">Live</a>
 */
class CallProcessor : RoutingContract("{...}") {

    override fun init(route: Route) {
        route.route(path) {
            get { call.action() }
            post { call.action() }
        }
    }

    private suspend fun ApplicationCall.action() {
        if (this.request.uri == "/") { // ping
            respond(HttpStatusCode.OK, "")
            return
        }
        val processResponse = MimikContainer.processCall(this)
        val contentType = processResponse.headers.contentType ?: ContentType.Text.Plain
        val code = HttpStatusCode.fromValue(processResponse.code)

        response.headers.append(processResponse.headers)
        val content = processResponse.body.contents()
        if (content.isEmpty() && processResponse.body?.contentLength() ?: 0 > 0) {
            respondText(contentType, HttpStatusCode.ExpectationFailed) { "Body length is longer than content length." }
            return
        }
        when (contentType.contentType) {
            "image" -> {
                val data = DatatypeConverter.parseBase64Binary(content)
                respondBytes(contentType, code) { data }
            }
            else -> respondText(contentType, code) { content }
        }
    }
}
