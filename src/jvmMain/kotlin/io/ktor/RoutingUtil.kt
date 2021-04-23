package io.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.util.*

val ApplicationCall.currentPath: String
    get() = URLBuilder.createFromCall(this).currentPath

fun Route.cssData(path: String, content: ApplicationCall.() -> String) {
    get(path) {
        call.respondCSS(content)
    }
}
