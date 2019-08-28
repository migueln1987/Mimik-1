package com.fiserv.ktmimic

import com.fiserv.ktmimic.networkRouting.ChatSession
import com.fiserv.ktmimic.networkRouting.FiservRouting
import com.fiserv.ktmimic.networkRouting.TapeRouting
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.routing.routing
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    installFeatures()

//    val client =
    HttpClient(OkHttp) {
        engine {
        }
    }

    routing {
        FiservRouting(this).apply {
            perform()
            importResponse("/fiserver/mock")
        }

        TapeRouting(this).apply {
            view("/view")
            test("/test")
        }
    }
}

private fun Application.installFeatures() {
    install(ContentNegotiation) {
        // gson {}
    }

    install(CallLogging) {
        level = Level.DEBUG
    }

    install(Sessions) {
        cookie<ChatSession>("SESSION")
    }
}
