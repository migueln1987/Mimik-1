@file:Suppress("PackageDirectoryMismatch")

package com.fiserv.mimik

import TapeCatalog
import helpers.tryGetBody
import io.ktor.application.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DoubleReceive
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import networkRouting.CallProcessor
import networkRouting.FetchResponder
import networkRouting.MimikMock
import networkRouting.editorPages.DataGen
import networkRouting.editorPages.TapeRouting
import networkRouting.TestingManager.TestManager
import networkRouting.port
import org.slf4j.event.Level

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

    TapeCatalog.isTestRunning = testing
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
            TestManager().init(this)

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

@KtorExperimentalAPI
private fun Application.installFeatures() {
    val deviceIDReg = """uniqueid.*?":"(.+?)"""".toRegex(RegexOption.IGNORE_CASE)
    install(DoubleReceive) // https://ktor.io/servers/features/double-receive.html

    install(CallId) {
        retrieve {
            var result = it.request.headers["x-dcmguid"]
            result = result ?: it.request.headers["x-up-subno"]
            result = result ?: it.request.headers["x-jphone-uid"]
            result = result ?: it.request.headers["x-em-uid"]
            if (result != null) return@retrieve result

            val deviceID = runBlocking {
                val body = it.tryGetBody()
                deviceIDReg.find(body.orEmpty())?.run { groups[1]?.value }
            }

            if (deviceID != null)
                return@retrieve deviceID

            ""
        }
    }

    install(ContentNegotiation) {
        // gson {}
    }

    install(CallLogging) {
        level = Level.DEBUG
    }
}
