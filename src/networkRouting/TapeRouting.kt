package com.fiserv.mimik.networkRouting

import com.fiserv.mimik.TapeCatalog
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.Parameters
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.request.isMultipart
import io.ktor.request.receiveMultipart
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.html.FormEncType
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h3
import kotlinx.html.hiddenInput
import kotlinx.html.p
import kotlinx.html.postButton
import kotlinx.html.postForm
import kotlinx.html.style
import kotlinx.html.submitInput
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.tr

class TapeRouting {

    private val tapeCatalog = TapeCatalog.Instance

    companion object {
        enum class RoutePaths(private val route: String) {
            VIEW("/view"),
            EDIT("/edit"),
            DELETE("/delete"),
            ACTION("/action");

            val value: String
                get() = "/tapes/$route"
        }
    }

    fun init(route: Routing) {
        route.apply {
            view
            action
            edit
            delete
        }
    }

    private val Routing.view: Route
        get() = apply {
            get(RoutePaths.VIEW.value) {
                call.respondHtml { getTapesPage() }
            }
            post(RoutePaths.VIEW.value) {
                call.respondRedirect(RoutePaths.VIEW.value, true)
            }
        }

    private val Routing.action: Route
        get() = post(RoutePaths.ACTION.value) {
            if (call.request.isMultipart()) {
                val values = call.receiveMultipart()
                    .readAllParts().asSequence()
                    .filterIsInstance<PartData.FormItem>()
                    .filterNot { it.name.isNullOrBlank() }
                    .map { it.name!! to it.value }
                    .toMap()

                call.processData(values)
            } else
                call.respondRedirect(RoutePaths.VIEW.value)
        }

    private val Routing.edit: Route
        get() = apply {
            val path = RoutePaths.EDIT.value
            get(path) {
                val tt = call.parameters
                val aa = tt.entries()
                val headerValues = call.request.headers.entries().flatMap {
                    it.value.map { mValue -> it.key to mValue }
                }.toMap()

                call.respondHtml { getEditChapterPage(call.parameters) }
            }
            post(path) {
                call.respondRedirect(path, true)
            }
        }

    private val Routing.delete: Route
        get() = apply {
            get(RoutePaths.DELETE.value) {
                call.respondText("delete page")
            }
        }

    private suspend fun ApplicationCall.processData(data: Map<String, String>) {
        return when (data["Action"]) {
            "Edit" -> {
                respondRedirect(false) {
                    path(RoutePaths.EDIT.value.drop(1))
                    parameters.apply {
                        data.filterNot { it.key == "Action" }
                            .forEach { (t, u) -> append(t, u) }
                    }
                }
            }
            "Delete" -> respondRedirect(RoutePaths.DELETE.value)
            else -> respondRedirect(RoutePaths.VIEW.value)
        }
    }

    private fun HTML.getTapesPage() {
        body {
            style {
                +"""
                    table {
                        font: 1em Arial;
                        border: 1px solid black;
                        width: 100%;
                    }
                    th {
                        background-color: #ccc;
                        width: 200px;
                    }
                    td {
                        background-color: #eee;
                    }
                    th, td {
                        text-align: left;
                        padding: 0.5em 1em;
                    }
                """.trimIndent()
            }

            tapeCatalog.tapes
                .filter { it.tapeChapters.isNotEmpty() }
                .forEach { t ->
                    h3 { +t.tapeName }
                    table {
                        t.tapeChapters.forEach {
                            tr {
                                th { +it.chapterName }
                                td {
                                    postForm(
                                        action = RoutePaths.ACTION.value,
                                        encType = FormEncType.multipartFormData
                                    ) {
                                        p { hiddenInput(name = "tape") { value = t.tapeName } }
                                        p { hiddenInput(name = "chapter") { value = it.chapterName } }

                                        p {
                                            submitInput(name = "Action") { value = "Edit" }
                                        }
                                        p {
                                            submitInput(name = "Action") { value = "Delete" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Page to edit all the chapters in the tape or other global tape settings
     * todo; is this needed?
     */
    private fun HTML.getEditTapePage() {

    }

    /**
     * Page to edit individual chapters in a tape
     */
    private fun HTML.getEditChapterPage(params: Parameters) {
        val activeTape = tapeCatalog.tapes
            .firstOrNull { it.tapeName == params["tape"] }
        val activeChapter = activeTape?.tapeChapters
            ?.firstOrNull { it.chapterName == params["chapter"] }

        if (activeChapter == null) {
            body {
                h1 { +"Unable to process the request." }
                br()
                form {
                    postButton {
                        formAction = RoutePaths.VIEW.value
                        +"..back to View tapes-post"
                    }
                }
            }
            return
        }

        body {
            h1 { +"This page is intentionally left blank. Waiting for the \"Edit Chapter\" html page" }
        }
    }
}
