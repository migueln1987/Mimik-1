@file:Suppress("PackageDirectoryMismatch")

package com.fiserv.mimik

import TapeCatalog
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
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
import networkRouting.FetchResponder
import networkRouting.MimikMock
import networkRouting.editorPages.DataGen
import networkRouting.editorPages.TapeRouting
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
            CallProcessor().init(this)
        }

        port(Ports.config) {
            MimikMock().init(this)
            TapeRouting().init(this)
            DataGen().init(this)
            FetchResponder().init(this)

            get { call.respondRedirect(TapeRouting.RoutePaths.rootPath) }
        }

        trace {
            @Suppress("UNUSED_VARIABLE")
            val traceViewer = it
        }

        intercept(ApplicationCallPipeline.Call) {
            @Suppress("UNUSED_VARIABLE")
            val interceptViewer = this
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
