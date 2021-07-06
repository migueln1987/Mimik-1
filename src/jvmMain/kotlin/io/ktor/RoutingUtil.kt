package io.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*

val ApplicationCall.currentPath: String
    get() = URLBuilder.createFromCall(this).currentPath

class CssContent(val data: String)

fun Route.respondCss(path: String, content: CssContent) {
    get(path) {
//        call.response.headers.append(HttpHeaders.CacheControl, CacheControl.MaxAge(maxAgeSeconds = 3600).toString())
        call.respondText(content.data, ContentType.Text.CSS)
    }
}

fun Route.respondCss_b(path: String, content: () -> CssContent) {
    get(path) {
        call.respondText(content().data, ContentType.Text.CSS)
    }
}
