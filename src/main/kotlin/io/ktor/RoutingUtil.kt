package io.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.*

val ApplicationCall.currentPath: String
    get() = URLBuilder.createFromCall(this).currentPath
