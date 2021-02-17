package mimik

import mimik.helpers.isNotEmpty
import mimik.helpers.tryGetBody
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import mimik.helpers.firstNotNullResult
import mimik.networkRouting.CallProcessor
import mimik.networkRouting.FetchResponder
import mimik.networkRouting.MimikMock
import mimik.networkRouting.editorPages.DataGen
import mimik.networkRouting.editorPages.TapeRouting
import mimik.networkRouting.help.HelpPages
import mimik.networkRouting.port
import mimik.networkRouting.testingManager.TestManager
import org.slf4j.event.Level
import java.io.File
import java.util.*

object Ports {
    const val config = 4321
    const val live = 2202
}

@InternalAPI
@Suppress("UNUSED_PARAMETER")
fun main(args: Array<String> = arrayOf()) {
    val env = applicationEngineEnvironment {
        module { MimikModule() }
        connector { port = Ports.config }
        connector { port = Ports.live }
    }
    embeddedServer(Netty, env).start(true)
}

@kotlin.jvm.JvmOverloads
fun Application.MimikModule(testing: Boolean = false) {
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
            HelpPages().init(this)
            TapeRouting().init(this)
            DataGen().init(this)
            FetchResponder().init(this)
            TestManager().init(this)

            static("assets") {
                staticRootFolder = File("src/main/resources")
                static("libs") { files("libs") }
            }

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

    @Suppress("EXPERIMENTAL_API_USAGE")
    install(DoubleReceive) // https://ktor.io/servers/features/double-receive.html

    val deviceIDReg = """uniqueid.*?".+?"([^"]+)""".toRegex(RegexOption.IGNORE_CASE)
    install(CallId) {
        var activeID = ""
        fun ApplicationCall.getID(): String {
            if (request.local.port == Ports.config)
                return "Port.Config"

            val result = arrayOf(
                "x-dcmguid", "x-up-subno", "x-jphone-uid", "x-em-uid",
                "uniqueid"
            ).asSequence().firstNotNullResult { request.headers[it] }

            if (result == null) {
                runBlocking {
                    val body = tryGetBody()
                    deviceIDReg.find(body.orEmpty())?.groups?.get(1)?.value
                }?.isNotEmpty {
                    activeID = it
                    println("Unique ID (via body): $it")
                }
            } else {
                result.isNotEmpty {
                    activeID = it
                    println("Unique ID (via header): $it")
                }
            }

            return activeID
        }

        retrieve {
            val useID = it.getID()
            if (useID.isEmpty()) null else useID
        }

        generate {
            if (it.callId == null) {
                activeID = UUID.randomUUID().toString()
                println("Unique ID (via GenID): $activeID")
            }
            it.callId ?: activeID
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
