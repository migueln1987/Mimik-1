@file:Suppress("PackageDirectoryMismatch")
package mimik

import TapeCatalog
import helpers.tryGetBody
import io.ktor.application.*
import io.ktor.features.*
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
import networkRouting.testingManager.TestManager
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
//    HttpClient(OkHttp) { engine {} }
//    HttpClient(CIO) { engine {} }

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
//    install(Compression) {
//        gzip {
//            priority = 1.0
//        }
//        deflate {
//            priority = 5.0
//            minimumSize(1024) // condition
//        }
//        identity{
//            priority = 10.0
//        }
//    }

    val deviceIDReg = """uniqueid.*?".+?"(.+?)"""".toRegex(RegexOption.IGNORE_CASE)
    install(DoubleReceive) // https://ktor.io/servers/features/double-receive.html

    install(CallId) {
        var activeID = ""
        fun ApplicationCall.getID(): String {
            if (request.local.port == Ports.config)
                return ""
            var result = request.headers["x-dcmguid"]
            result = result ?: request.headers["x-up-subno"]
            result = result ?: request.headers["x-jphone-uid"]
            result = result ?: request.headers["x-em-uid"]
            result = result ?: request.headers["uniqueid"]
            if (result != null) return result.also {
                activeID = it
                println("Result ID: $it")
            }

            return runBlocking {
                val body = tryGetBody()
                deviceIDReg.find(body.orEmpty())?.groups?.get(1)?.value
            }.orEmpty().also {
                activeID = it
                println("Use ID: $it")
            }
        }

        retrieve { it.getID() }

        generate {
            if (it.callId == null)
                it.getID().also {
                    activeID = it
                    println("GenID: $it")
                }
            else ""
        }

        verify { it == activeID }
    }

//    install(ContentNegotiation) {
    // gson {}
//    }

    install(CallLogging) {
        level = Level.DEBUG
    }
}
