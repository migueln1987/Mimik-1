package io.ktor.http.content

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import kotlinx.ranges.toIntRange
import kotlinx.toByteReadChannel

/**
 * Responds with data formatted as `Text/CSS`
 */
suspend inline fun ApplicationCall.respondCSS(data: ApplicationCall.() -> String) {
    respond(CssData(data(this)))
}

class CssData() : OutgoingContent.ReadChannelContent() {
    lateinit var data: String

    constructor(inputData: String) : this() {
        data = inputData
    }

    constructor(builder: () -> String) : this() {
        data = builder()
    }

    override val contentLength get() = data.length.toLong()
    override val contentType get() = ContentType.Text.CSS
    override val status get() = HttpStatusCode.OK
    override fun readFrom() = data.toByteReadChannel()
    override fun readFrom(range: LongRange) = data.substring(range.toIntRange()).toByteReadChannel()
    override val headers = Headers.Empty
}
