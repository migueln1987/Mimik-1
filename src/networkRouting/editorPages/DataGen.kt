package networkRouting.editorPages

import helpers.*
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.Parameters
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import kotlinx.html.*
import networkRouting.RoutingContract

@Suppress("RemoveRedundantQualifierName")
class DataGen(path: String = RoutePaths.rootPath) : RoutingContract(path) {

    private val noData = "{ no data }"

    enum class RoutePaths(val path: String) {
        Response("response"),
        Attractors("attractors");

        companion object {
            const val rootPath = "gen"
        }

        val asSubPath
            get() = "$rootPath/$path"
    }

    val CommonAttributeGroupFacade.disabledBG: Unit
        get() {
            if (isThrow { style })
                style = "background-color: #f6f6f6;"
            else
                style += "background-color: #f6f6f6;"
        }

    override fun init(route: Routing) {
        route.route(path) {
            response
        }
    }

    private val Route.response
        get() = route(RoutePaths.Response.path) {
            get {
                val limitParams = call.parameters
                    .limit(listOf("ref"))

                if (call.parameters != limitParams) {
                    call.respondRedirect {
                        //                        path(RoutePaths.Response.path)
                        parameters.clear()
                        parameters.appendAll(limitParams)
                    }
                    return@get
                }

                call.respondHtml { responsePage(call.parameters) }
            }
        }

    private fun HTML.responsePage(params: Parameters) {
        val refName = params["ref"]

        val actChap = tapeCatalog.tapes.asSequence()
            .flatMap { tape ->
                tape.chapters
                    .map { "%s%s".format(tape.name, it.name).hashCode().toString() to it }
                    .asSequence()
            }
            .filter { it.first == refName }
            .firstOrNull()?.second

        head {
            setupStyle()
            script {
                unsafe { +JS.all }
            }
        }

        val inlineDiv: (DIV) -> Unit = {
            it.style = "display: inline;"
        }

        body {
            if (actChap == null) {
                p {
                    +"No valid chapter was found."
                    linebreak()

                    a {
                        href = "../tapes/all"
                        +"Goto View All Tapes"
                    }
                }
                return@body
            }

            val actTape = tapeCatalog.tapes
                .first { it.chapters.contains(actChap) }

            +"Chapter ID: %s, Name: %s".format(
                actChap.hashCode(),
                actChap.name
            )

            linebreak()
            a {
                href = "../%s?tape=%s&chapter=%s".format(
                    TapeRouting.RoutePaths.EDIT.asSubPath,
                    actTape.name,
                    actChap.name
                )
                +"return to chapter"
            }

            h2 { +"Request" }
            makeToggleArea {
                table {
                    tr {
                        th {
                            style = "width: 20%"
                            +"Routing URL"
                        }
                        td {
                            makeToggleArea {
                                var useCustom = true

                                div(classes = "radioDiv") {
                                    val data = actTape.routingUrl
                                    val isValid = data.isValidURL
                                    radioInput(name = "reqUrl") {
                                        value = data.orEmpty()
                                        checked = isValid
                                        disabled = !isValid
                                        useCustom = useCustom or isValid
                                    }
                                    infoText(property = "Tape URL", divArgs = inlineDiv)
                                    br()

                                    textInput {
                                        readonly = true
                                        disabled = !isValid
                                        if (!isValid)
                                            disabledBG
                                        value = data ?: noData
                                    }
                                }

                                div(classes = "radioDiv") {
                                    val data = actChap.requestData?.url
                                    val isValid = data.isValidURL

                                    radioInput(name = "reqUrl") {
                                        value = data.orEmpty()
                                        checked = isValid
                                        disabled = !isValid
                                        useCustom = useCustom or isValid
                                    }
                                    infoText(property = "Chapter URL", divArgs = inlineDiv)
                                    br()

                                    textInput {
                                        readonly = true
                                        disabled = !isValid
                                        if (!isValid)
                                            disabledBG
                                        value = data ?: noData
                                    }
                                }

                                div(classes = "radioDiv") {
                                    radioInput(name = "reqUrl") {
                                        id = "reqUrlCustom"
                                        checked = useCustom
                                    }
                                    infoText(property = "Custom URL", divArgs = inlineDiv)
                                    br()

                                    textInput {
                                        placeholder = "Example: http://google.com"
                                        onKeyUp = "reqUrlCustom.value = value;"
                                    }
                                }
                            }
                        }
                    }

                    tr {
                        th { +"Path" }
                        td {
                            makeToggleArea {
                                var useCustom = true

                                div(classes = "radioDiv") {
                                    val data = actTape.attractors?.routingPath?.value
                                    val isValid = !data.isNullOrEmpty()
                                    radioInput(name = "reqPath") {
                                        value = data.orEmpty()
                                        checked = isValid
                                        disabled = !isValid
                                        useCustom = useCustom or isValid
                                    }
                                    infoText(property = "Tape Path", divArgs = inlineDiv)
                                    br()

                                    textInput {
                                        readonly = true
                                        disabled = !isValid
                                        if (!isValid)
                                            disabledBG
                                        value = data ?: noData
                                    }
                                }

                                div(classes = "radioDiv") {
                                    val data = actChap.attractors?.routingPath?.value
                                    val isValid = !data.isNullOrEmpty()
                                    radioInput(name = "reqPath") {
                                        value = data.orEmpty()
                                        checked = isValid
                                        disabled = !isValid
                                        useCustom = useCustom or isValid
                                    }
                                    infoText(property = "Chapter Path", divArgs = inlineDiv)
                                    br()

                                    textInput {
                                        readonly = true
                                        disabled = !isValid
                                        if (!isValid)
                                            disabledBG
                                        value = data ?: noData
                                    }
                                }

                                div(classes = "radioDiv") {
                                    radioInput(name = "reqPath") {
                                        id = "reqPathCustom"
                                        checked = useCustom
                                    }
                                    infoText(property = "Custom Path", divArgs = inlineDiv)
                                    br()

                                    textInput {
                                        placeholder = "Example: sub/path"
                                        onKeyUp = "reqPathCustom.value = value;"
                                    }
                                }
                            }
                        }
                    }

                    tr {
                        th { +"Headers" }
                        td {
                            makeToggleArea {
                                +"[ todo; add header data ]"
                            }
                        }
                    }

                    tr {
                        th { +"Body" }
                        td {
                            makeToggleArea {
                                var useCustom = true
                                div(classes = "radioDiv") {
                                    val data = actChap.requestData?.body
                                    val isValid = !data.isNullOrEmpty()
                                    radioInput(name = "reqBody") {
                                        style = ""
                                        value = data.orEmpty()
                                        checked = isValid
                                        disabled = !isValid
                                        useCustom = !isValid
                                    }
                                    infoText(property = "Chapter Body", divArgs = inlineDiv)
                                    br()

                                    textArea {
                                        readonly = true
                                        disabled = !isValid
                                        if (!isValid)
                                            disabledBG
                                        +data.orEmpty()
                                    }
                                }

                                div(classes = "radioDiv") {
                                    radioInput(name = "reqBody") {
                                        id = "reqBodyCustom"
                                        checked = useCustom
                                    }
                                    infoText(property = "Custom Body", divArgs = inlineDiv)
                                    br()

                                    textArea {
                                        id = "reqbody"
                                        onKeyPress = "keypressNewlineEnter(reqbody);"
                                        onKeyUp = "reqBodyCustom.value = value;"
                                    }
                                }
                            }
                        }
                    }
                }
            }

            linebreak()

            h2 { +"Response" }
            makeToggleArea {
                table {
                    tr {
                        th { +"Something" }
                        td { +"aa" }
                        td { +"bb" }
                        td { +"cc" }
                    }
                }
            }
        }
    }
}
