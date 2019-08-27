package com.fiserv.ktmimic

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        // gson {}
    }

    install(CallLogging) {
        level = Level.DEBUG
    }

//    val client =
    HttpClient(OkHttp) {
        engine {
        }
    }

    val tapeCatalog = TapeCatalog(VCRConfig.getConfig)

    routing {
        post("/fiserver/cbes/perform.do") {
            val response = tapeCatalog.processCall(call) {
                call.request.queryParameters["opId"] ?: ""
            }

            call.respondText(ContentType.parse("application/json"), HttpStatusCode.OK) {
                withContext(Dispatchers.IO) {
                    response.toJson()
                }
            }
        }

        get("/html-dsl") {
            call.respondHtml {
                /*
                body {
                    h1 { +"HTML" }
                    ul {
                        for (n in 1..10) {
                            li { +"$n" }
                        }
                    }
                }
                */
            }
        }

        get("/json/gson") {
            // call.respond(mapOf("hello" to "world"))
        }
    }
}
