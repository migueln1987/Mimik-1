import networkRouting.FiservRouting
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
            MimikMock("/mock"),
            TapeRouting("/tapes"),
            FiservRouting("/fiserver/cbes/perform.do")
        ).forEach { it.init(this) }

        get("/") {
            call.respondRedirect(TapeRouting.RoutePaths.ALL.path)
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
