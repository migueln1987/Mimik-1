package com.fiserv.ktmimic.networkRouting

import com.fiserv.ktmimic.TapeCatalog
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.html.FormMethod
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.body
import kotlinx.html.form
import kotlinx.html.h3
import kotlinx.html.input
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.tr

class TapeRouting(private val routing: Routing) {

    private val tapeCatalog = TapeCatalog.Instance

    fun view(path: String) {
        routing.apply {
            get(path) {
                call.respondHtml { getTapesPage() }
            }
        }
    }

    fun view2(path: String) {
        routing.apply {
            get {
                call.respondHtml {
                    body {
                        //                    form(action = "/tapes", method = FormMethod.get) {
//                        input(InputType.submit) {
//                            value = "Logout"
//                            name = "username"
//                        }
//
//                        input(InputType.submit) {
//                            value = "test2"
//                            name = "username2"
//                        }
//                    }

                        tapeCatalog.tapes.filter {
                            it.tapeChapters.isNotEmpty()
                        }.forEach { t ->
                            h3 { +t.tapeName }
                            table {
                                t.tapeChapters.forEach {
                                    tr {
                                        td { +it.chapterName }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun test(path: String) {
        routing.apply {
            get(path) {
                //                val post = call.parameters()
//                val first = post[""]
                call.respond(mapOf("hello" to "world"))
            }
        }
    }

    private fun HTML.getTapesPage() {
        body {
            form(action = "/tapes", method = FormMethod.get) {
                input(InputType.submit) {
                    value = "Logout"
                    name = "username"
                }

//                input(InputType.submit) {
//                    value = "test2"
//                    name = "username2"
//                }
            }
        }
    }
}

data class ChatSession(val id: String)
