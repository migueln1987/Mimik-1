@file:Suppress("PackageDirectoryMismatch")

package com.fiserv.mimik

import networkRouting.MimikMock
import networkRouting.TapeRouting
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import networkRouting.CallProcessor
import networkRouting.port
import org.slf4j.event.Level
import tapeItems.BlankTape

object Ports {
    const val config = 4321
    const val live = 2202
}

fun main(args: Array<String> = arrayOf()) {
    val env = applicationEngineEnvironment {
        module { module() }
        connector { port = Ports.config }
        connector { port = Ports.live }
    }
    embeddedServer(Netty, env).start(true)
}

@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    installFeatures()

    BlankTape.isTestRunning = testing
    TapeCatalog.Instance // +loads the tape data

//    val client =
    HttpClient(OkHttp) { engine {} }

    routing {
        port(Ports.live) {
            CallProcessor("{...}").init(this@routing)
        }

        port(Ports.config) {
            MimikMock("mock").init(this@routing)
            TapeRouting("tapes").init(this@routing)
            get { call.respondRedirect("tapes") }
        }

        trace {
            @Suppress("UNUSED_VARIABLE")
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
