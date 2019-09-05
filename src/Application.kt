package com.fiserv.mimik

import com.fiserv.mimik.networkRouting.FiservRouting
import com.fiserv.mimik.networkRouting.TapeRouting
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.routing.routing
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

        TapeRouting().init(this)
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
