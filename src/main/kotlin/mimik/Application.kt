package mimik

import com.typesafe.config.ConfigFactory
import io.ktor.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import kotlinUtils.isNotEmpty
import kotlinx.coroutines.runBlocking
import mimik.helpers.firstNotNullResult
import mimik.networkRouting.CallProcessor
import mimik.networkRouting.FetchResponder
import mimik.networkRouting.MimikMock
import mimik.networkRouting.editorPages.DataGen
import mimik.networkRouting.editorPages.TapeRouting
import mimik.networkRouting.help.HelpPages
import mimik.networkRouting.testingManager.TestManager
import mimik.tapeItems.TapeCatalog
import org.slf4j.event.Level
import java.io.File
import java.util.*

object Ports {
    const val config = 4321
    const val mock = 2202
    const val http = 8080

    val deployment: Int
        get() = ConfigFactory.load().getInt("ktor.deployment.port")
}

@Suppress("UNUSED_PARAMETER")
fun main(args: Array<String> = arrayOf()) {
    val env = applicationEngineEnvironment {
        module { MimikModule() }
        connector { port = Ports.config }
        connector { port = Ports.mock }
        // https://ktor.io/docs/auto-reload.html
    }

    embeddedServer(Jetty, env).start(true)
}

@Suppress("FunctionName")
fun main_(args: Array<String> = arrayOf()) = EngineMain.main(args)

@kotlin.jvm.JvmOverloads
fun Application.MimikModule(testing: Boolean = false) {
    installFeatures()

    TapeCatalog.isTestRunning = testing
    TapeCatalog.Instance // +loads the tape data
    println("RootPath: ${environment.rootPath}")

    routing {
        // https://ktor.io/docs/routing-in-ktor.html#match_url
        debugging()

        if (environment.rootPath.isNotEmpty()) {
            println("Deployment port: ${Ports.deployment}")
            println("Adding rootPath items")
            route("/") {
                route("mock") { MockPaths() }
                ConfigPaths()

                get { call.redirect(TapeRouting.RoutePaths.ALL.asSubPath) }
                post { call.redirect("mock") }
            }
        } else {
            port(Ports.mock) { MockPaths() }
            port(Ports.config) {
                ConfigPaths()
                get { call.redirect(TapeRouting.RoutePaths.ALL.asSubPath) }
            }
        }

        displayRegisteredPaths()
    }
}

private fun Route.MockPaths() {
    CallProcessor().init(this)
}

private fun Route.ConfigPaths() {
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
}

private fun Route.displayRegisteredPaths() {
    println("====== registered paths ====")
    fun getChildItems(rt: Route): List<String> {
        return if (rt.children.isEmpty())
            listOf(rt.toString().replace("///", "/"))
        else rt.children.flatMap { getChildItems(it) }
    }

    getChildItems(this).forEach {
        println(it)
    }
    println("====== end registered paths ====")
}

@Suppress("UNUSED_VARIABLE")
private fun Routing.debugging(doDebug: Boolean = true) {
    if (!doDebug) return

    trace {
        val traceViewer = it
        println("-> Trace: (${it.call.request.httpMethod.value}) ${it.call.currentPath}")
    }

    intercept(ApplicationCallPipeline.Setup) {
        val interceptViewer = this
    }

    intercept(ApplicationCallPipeline.Call) {
        val interceptViewer = this
        println("-> Incoming call: ${call.currentPath}")
    }
}

private fun Application.installFeatures() {
    install(DefaultHeaders)

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

    // https://ktor.io/servers/features/double-receive.html
    @Suppress("EXPERIMENTAL_API_USAGE")
    // install(DoubleReceive) { receiveEntireContent = true }
    install(LocalDoubleReceive) { receiveEntireContent = true }

    val deviceIDReg = """uniqueid.*?".+?"([^"]+)""".toRegex(RegexOption.IGNORE_CASE)
    install(CallId) {
        var activeID = ""
        fun ApplicationCall.printID(item: String, id: String) {
            val port = if (request.port() == Ports.http)
                "" else "; port ${request.port()}"
            "Unique ID (via %s%s): %s".format(item, port, id)
                .also { println(it) }
        }

        fun ApplicationCall.getID(): String {
            if (request.local.port == Ports.config) {
                printID("config", "config")
                activeID = "Port.Config"
//                return activeID
            }

            val result = arrayOf(
                "x-dcmguid", "x-up-subno", "x-jphone-uid", "x-em-uid",
                "uniqueid"
            ).asSequence().firstNotNullResult { request.headers[it] }

            if (result == null) {
                runBlocking {
                    val body = tryGetBody().orEmpty()
                    deviceIDReg.find(body)?.groups?.get(1)?.value
                }?.isNotEmpty {
                    activeID = it
                    printID("body", it)
                }
            } else {
                result.isNotEmpty {
                    activeID = it
                    printID("header", it)
                }
            }

            return activeID
        }

        retrieve {
            val useID = it.getID()
            if (useID.isEmpty() || activeID.isEmpty()) null else useID
        }

        generate {
            if (activeID.isEmpty()) {
                activeID = UUID.randomUUID().toString()
                it.printID("GenID", activeID)
            }
            activeID
        }

        verify { it == activeID }

        reply { call, callId ->
            call.response.headers.append("callId", callId)
        }
    }

//    install(ContentNegotiation) {
    // gson {}
//    }

    install(CallLogging) {
        level = Level.DEBUG
    }
}
