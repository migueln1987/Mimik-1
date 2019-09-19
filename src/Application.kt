@file:Suppress("PackageDirectoryMismatch")

package com.fiserv.mimik

import networkRouting.MimikMock
import networkRouting.TapeRouting
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.routing.routing
import networkRouting.CallProcessor
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused", "UNUSED_PARAMETER") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    installFeatures()

//    val client =
    HttpClient(OkHttp) {
        engine {
        }
    }

    routing {
        arrayOf(
            CallProcessor("/*"),
            MimikMock("/mock"),
            TapeRouting("/tapes")

        ).forEach { it.init(this) }

        trace {
            val traceViewer = it
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
}
