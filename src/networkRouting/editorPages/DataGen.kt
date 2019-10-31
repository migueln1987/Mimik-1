package networkRouting.editorPages

import helpers.*
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import kotlinx.html.*
import networkRouting.RoutingContract
import okhttp3.HttpUrl
import java.util.Date

@Suppress("RemoveRedundantQualifierName")
class DataGen(path: String = RoutePaths.rootPath) : RoutingContract(path) {

    private val noData = "{ no data }"

    enum class RoutePaths(val path: String) {
        Response("response"),
        ResponseGen("responseGen"),
        Attractors("attractors");

        companion object {
            const val rootPath = "gen"
        }

        val asSubPath
            get() = "$rootPath/$path"
    }

    private enum class ResponseActions {
        Make, Remove, Use, NewChapter
    }

    override fun init(route: Routing) {
        route.route(path) {
            response
            responseGen
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

    private val Route.responseGen
        get() = route(RoutePaths.ResponseGen.path) {
            post {
                val params = call.anyParameters()
                if (params == null) {
                    call.respondRedirect(RoutePaths.Response.path)
                    return@post
                }

                when (enumSafeValue<ResponseActions>(params["Action"])) {
                    null -> {
                    }
                    ResponseActions.Make -> {
                    }

                    ResponseActions.Remove -> {
                    }

                    ResponseActions.Use -> {
                    }

                    ResponseActions.NewChapter -> {
                    }
                }
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

            val genResponses = (actChap.genResponses ?: listOf())
                .toMutableList().apply {
                    actChap.responseData?.also { add(0, it) }
                }

            val itemName = params["item"]
            val item = when (itemName?.trim()) {
                null -> null
                "" -> genResponses
                    .minBy { it.recordedDate ?: Date() }

                else -> genResponses
                    .firstOrNull { it.hashCode().toString() == itemName }
            }

            val actItem = item ?: actChap.responseData

            +"Tape ID: %s, Name: %s".format(
                actTape.hashCode(),
                actTape.name
            )
            br()
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

            var useCustom = true

            form(encType = FormEncType.multipartFormData) {
                hiddenInput(name = "ref") { value = refName.orEmpty() }
                hiddenInput(name = "tape") { value = actTape.name }
                hiddenInput(name = "chapter") { value = actChap.name }

                h2 { +"Request" }
                makeToggleArea {
                    var usingURL: HttpUrl? = null
                    table {
                        tr {
                            th { +"Method" }
                            td {
                                makeToggleArea {
                                    div(classes = "radioDiv") {
                                        val data = actChap.requestData?.method
                                        val isValid = data != null
                                        useCustom = !isValid
                                        radioInput(name = "reqMethod") {
                                            value = data.orEmpty()
                                            disabled = !isValid
                                            checked = isValid
                                        }
                                        infoText("Chapter Method") {
                                            it.inlineDiv
                                            if (!isValid) it.disabledText
                                        }
                                        br()

                                        textInput {
                                            readonly = true
                                            disabled = true
                                            if (isValid) readonlyBG else disabledBG
                                            value = data ?: noData
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        val data = actChap.recentRequest?.method
                                        radioInput(name = "reqMethod") {
                                            id = "reqMethodCustom"
                                            value = data.orEmpty()
                                            checked = data != null || useCustom
                                        }
                                        infoText("Custom Method") { it.inlineDiv }
                                        br()

                                        select {
                                            onChange = "reqMethodCustom.value = selectedIndex;"
                                            HttpMethod.DefaultMethods.forEach {
                                                option {
                                                    if (data?.toUpperCase() == it.value)
                                                        selected = true
                                                    +it.value.toLowerCase().uppercaseFirstLetter()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        tr {
                            th {
                                style = "width: 20%"
                                +"Request URL"
                            }
                            td {
                                makeToggleArea {
                                    div(classes = "radioDiv") {
                                        usingURL = actTape.routingUrl.asHttpUrl
                                        val data = usingURL.let { it?.hostPath ?: noData }
                                        val isValid = data.isValidURL
                                        useCustom = !isValid
                                        radioInput(name = "reqUrl") {
                                            value = data
                                            checked = isValid
                                            disabled = !isValid
                                        }
                                        infoText("Tape URL") {
                                            it.inlineDiv
                                            if (!isValid) it.disabledText
                                        }
                                        br()

                                        textInput {
                                            readonly = true
                                            disabled = !isValid
                                            if (isValid) readonlyBG else disabledBG
                                            value = data
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        usingURL = actChap.requestData?.url.asHttpUrl
                                        val data = usingURL.let { it?.hostPath ?: noData }
                                        val isValid = data.isValidURL
                                        useCustom = !isValid
                                        radioInput(name = "reqUrl") {
                                            value = data
                                            checked = isValid
                                            disabled = !isValid
                                        }
                                        infoText("Chapter URL") {
                                            it.inlineDiv
                                            if (!isValid) it.disabledText
                                        }
                                        br()

                                        textInput {
                                            readonly = true
                                            disabled = !isValid
                                            if (isValid) readonlyBG else disabledBG
                                            value = data
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        val data = actChap.recentRequest?.httpUrl?.hostPath
                                        radioInput(name = "reqUrl") {
                                            id = "reqUrlCustom"
                                            checked = usingURL == null || useCustom
                                        }
                                        infoText("Custom URL") { it.inlineDiv }
                                        br()

                                        textInput {
                                            placeholder = data ?: "Example: http://google.com/"
                                            value = data.orEmpty()
                                            onKeyUp = "reqUrlCustom.value = value;"
                                        }
                                    }
                                }
                            }
                        }

                        tr {
                            th { +"Params" }
                            td {
                                makeToggleArea {
                                    tooltipText("Info", "genKVDataField")
                                    br()

                                    div(classes = "radioDiv") {
                                        val data = actChap.requestData?.url.asHttpUrl.toParameters
                                        val isValid = data?.isEmpty().isFalse()
                                        useCustom = !isValid
                                        radioInput(name = "reqParams") {
                                            checked = isValid
                                            disabled = !isValid
                                            value = (data ?: "").toString()
                                        }
                                        infoText("Chapter Params") {
                                            it.inlineDiv
                                            if (!isValid) it.disabledText
                                        }
                                        br()

                                        paramTextArea(data) {
                                            readonly = true
                                            disabled = !isValid
                                            if (isValid) readonlyBG else disabledBG
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        val data = actChap.recentRequest?.url.asHttpUrl.toParameters
                                        radioInput(name = "reqParams") {
                                            id = "reqCustomParams"
                                            checked = data?.isEmpty().isFalse() || useCustom
                                            value = (data ?: "").toString()
                                        }
                                        infoText("Custom Params") { it.inlineDiv }
                                        br()

                                        paramTextArea(data) {
                                            onKeyPress = "keypressNewlineEnter(this);"
                                            onKeyUp = "reqCustomParams.value = value;"
                                        }
                                    }
                                }
                            }
                        }

                        tr {
                            th { +"Headers" }
                            td {
                                makeToggleArea {
                                    tooltipText("Info", "genKVDataField")
                                    br()

                                    div(classes = "radioDiv") {
                                        val data = actChap.requestData?.headers
                                        val isValid = (data?.size() ?: 0) > 0
                                        useCustom = !isValid
                                        radioInput(name = "netHeaders") {
                                            checked = isValid
                                            disabled = !isValid
                                            value = (data ?: "").toString().trim()
                                        }
                                        infoText("Chapter Headers") {
                                            it.inlineDiv
                                            if (!isValid) it.disabledText
                                        }
                                        br()

                                        headerTextArea(data) {
                                            readonly = true
                                            disabled = !isValid
                                            if (isValid) readonlyBG else disabledBG
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        val data = actChap.recentRequest?.headers
                                        radioInput(name = "netHeaders") {
                                            id = "customHeaders"
                                            value = (data ?: "").toString()
                                            checked = (data?.size() ?: 0) > 0 || useCustom
                                        }
                                        infoText("Custom Headers") { it.inlineDiv }
                                        br()

                                        headerTextArea(data) {
                                            onKeyPress = "keypressNewlineEnter(this);"
                                            onKeyUp = "customHeaders.value = value.trim();"
                                        }
                                    }
                                }
                            }
                        }

                        tr {
                            th { +"Body" }
                            td {
                                makeToggleArea {
                                    div(classes = "radioDiv") {
                                        val data = actChap.requestData?.body
                                        val isValid = !data.isNullOrEmpty()
                                        useCustom = !isValid
                                        radioInput(name = "reqBody") {
                                            value = data.orEmpty()
                                            checked = isValid
                                            disabled = !isValid
                                        }
                                        infoText("Chapter Body") {
                                            it.inlineDiv
                                            if (!isValid) it.disabledText
                                        }
                                        br()

                                        textArea {
                                            readonly = true
                                            disabled = !isValid
                                            if (isValid) readonlyBG else disabledBG
                                            +data.orEmpty()
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        val data = actChap.recentRequest?.body
                                        radioInput(name = "reqBody") {
                                            id = "reqBodyCustom"
                                            checked = !data.isNullOrEmpty() || useCustom
                                        }
                                        infoText("Custom Body") { it.inlineDiv }
                                        br()

                                        textArea {
                                            onKeyPress = "keypressNewlineEnter(this);"
                                            onKeyUp = "reqBodyCustom.value = value;"
                                            +data.orEmpty()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                linebreak()

                h2 { +"Response" }
                makeToggleArea(item != null) {
                    table {
                        tr {
                            td {
                                style = "text-align: center;"
                                postButton(name = "Action") {
                                    value = ResponseActions.Make.name
                                    formAction = RoutePaths.ResponseGen.path
                                    +"Make call"
                                }
                            }

                            td {
                                style = "text-align: center;"
                                if (genResponses.isEmpty())
                                    +noData else select {
                                    name = "selectResponse"
                                    genResponses.forEach {
                                        option {
                                            onChange = ""
                                            selected = when (item) {
                                                null -> actChap.responseData.hashCode()
                                                else -> item.hashCode()
                                            } == it.hashCode()
                                            value = it.hashCode().toString()
                                            +"[%d] - %s".format(
                                                it.hashCode(),
                                                it.recordedDate?.toString() ?: "[file data]"
                                            )
                                        }
                                    }
                                }
                            }

                            td {
                                style = "text-align: center;"
                                postButton(name = "Action") {
                                    formAction = RoutePaths.ResponseGen.path
                                    value = ResponseActions.Remove.name
                                    disabled = genResponses.isEmpty()
                                    +"Remove call"
                                }
                            }

                            td {
                                postButton(name = "Action") {
                                    formAction = RoutePaths.ResponseGen.path
                                    value = ResponseActions.Use.name
                                    disabled = genResponses.isEmpty()
                                    +"Use call -> Chapter"
                                }

                                br()
                                div {
                                    postButton(name = "Action") {
                                        formAction = RoutePaths.ResponseGen.path
                                        value = ResponseActions.NewChapter.name
                                        disabled = genResponses.isEmpty()
                                        +"Use call -> New Chapter"
                                    }
                                    +" "
                                    textInput(name = "newChapter") {
                                        disabled = genResponses.isEmpty()
                                        if (genResponses.isEmpty())
                                            disabledBG
                                        placeholder = "New Chapter name"
                                    }
                                }
                            }
                        }
                    }

                    if (actItem != null) {
                        table {
                            id = "resultTable"
                            tr {
                                th {
                                    style = "width: 20%;"
                                    +"Response code"
                                }
                                td {
                                    +"Code: %d".format(actItem.code)
                                    br()
                                    +"Status - '%s'".format(
                                        HttpStatusCode.fromValue(actItem.code ?: 200).description
                                    )
                                }
                            }

                            tr {
                                th { +"Headers" }
                                td {
                                    table {
                                        thead {
                                            tr {
                                                th { +"Key" }
                                                th { +"Value" }
                                            }
                                        }
                                        tbody {
                                            val headerMap = actItem.headers?.toMultimap()
                                            if (headerMap.isNullOrEmpty())
                                                +noData else headerMap.forEach { (t, u) ->
                                                u.forEach {
                                                    tr {
                                                        td { +t }
                                                        td { +it }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            tr {
                                th { +"Body" }
                                td {
                                    textArea {
                                        readonly = true
                                        // +actItem.body.orEmpty()

                                        +actItem.toJson
                                    }
                                    script {
                                        unsafe {
                                            +"beautifyField(getScriptElem().previousElementSibling);"
                                        }
                                    }
                                }
                            }
                        }
                    } else +noData
                }
            }
        }
    }
}
