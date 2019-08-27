package com.fiserv.ktmimic

import com.fiserv.ktmimic.tapeTypes.helpers.TapeCatalog
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.html.*
import kotlinx.html.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson {
        }
    }

    install(CallLogging) {
        level = Level.DEBUG
    }

    val client = HttpClient(OkHttp) {
        engine {
        }

    }

    val tapeCatalog = TapeCatalog(VCRConfig.getConfig)

    routing {
        post("/fiserver/cbes/perform.do") {

            val response = tapeCatalog.processCall(call) {
                call.request.header("opId") ?: ""
            }

            call.respondText {
                withContext(Dispatchers.IO) {
                    String(response.body()?.bytes() ?: byteArrayOf())
                }
            }
        }

        get("/html-dsl") {
            call.respondHtml {
                body {
                    h1 { +"HTML" }
                    ul {
                        for (n in 1..10) {
                            li { +"$n" }
                        }
                    }
                }
            }
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

