package mimik.networkRouting.GUIPages

import R
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.toRequestData
import com.github.kittinunf.fuel.core.toResponseData
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.routing.Route
import kotlinx.*
import kotlinx.html.*
import mimik.Ports
import mimik.helpers.*
import mimik.mockHelpers.toAttractors
import mimik.networkRouting.RoutingContract
import mimik.networkRouting.GUIPages.DataGen.RoutePaths.Response
import mimik.networkRouting.routers.JsUtils
import mimik.networkRouting.routers.StyleUtils.setupStyle
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.*

class DataGen : RoutingContract(RoutePaths.rootPath) {

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

    override fun init(route: Route) {
        route.route(path) {
            response
            responseGen
        }
    }

    private val Route.response
        get() = route(Response.path) {
            get {
                val limitParams = call.parameters.limitKeys("ref", "item")

                if (call.parameters != limitParams) {
                    call.redirect() {
                        // path(RoutePaths.Response.path)
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
//                if (params == null) {
//                    call.respondRedirect(Response.path)
//                    return@post
//                }

                val tape = tapeCatalog.tapes
                    .firstOrNull { it.name == params["tape"] }

                if (tape == null) {
                    call.respondRedirect(Response.path)
                    return@post
                }

                val chap = tape.chapters
                    .firstOrNull { it.name == params["chapter"] }

                when (enumSafeValue<ResponseActions>(params["Action"])) {
                    null -> Unit

                    ResponseActions.Make -> {
                        val (request, response, _) = params.asResponseCall()

                        if (chap == null) {
                            call.redirect(Response.path)
                            return@post
                        }

                        chap.recentRequest = request.toRequestData
                        if (chap.genResponses.isNullOrEmpty())
                            chap.genResponses = mutableListOf()
                        val requestResponse = response.toResponseData.also {
                            if (params["useLocalhost"] != null)
                                it.isLocalhostCall = true
                        }
                        chap.genResponses?.add(requestResponse)

                        call.redirect(Response.path) {
                            parameters["ref"] = params["ref"].orEmpty()
                            parameters["item"] = requestResponse.hashCode().toString()
                        }
                    }

                    ResponseActions.Remove -> {
                        chap?.genResponses?.removeIf {
                            it.hashCode().toString() == params["selectResponse"]
                        }

                        call.redirect(Response.path) {
                            parameters["ref"] = params["ref"].orEmpty()
                        }
                    }

                    ResponseActions.Use -> {
                        if (chap == null) {
                            call.redirect_abs(TapeRouting.RoutePaths.EDIT.asSubPath) {
                                parameters["tape"] = tape.name
                            }
                            return@post
                        }

                        chap.responseData = chap.genResponses
                            ?.firstOrNull { it.hashCode().toString() == params["selectResponse"] }
                        tape.saveIfExists()

                        call.redirect_abs(TapeRouting.RoutePaths.EDIT.asSubPath) {
                            parameters.clear()
                            parameters["tape"] = tape.name
                            parameters["chapter"] = chap.name
                        }
                    }

                    ResponseActions.NewChapter -> {
                        val requestData: RequestData?
                        val responseData: ResponseData?
                        if (chap == null) {
                            val (request, response, _) = params.asResponseCall()
                            requestData = request.toRequestData
                            responseData = response.toResponseData
                        } else {
                            requestData = chap.recentRequest?.clone()
                            chap.recentRequest = null

                            responseData = chap.genResponses
                                ?.firstOrNull { it.hashCode().toString() == params["selectResponse"] }
                                ?.clone()

                            chap.genResponses?.removeIf {
                                it == responseData
                            }
                        }

                        val newChap = tape.createNewInteraction {
                            it.chapterName = params["newChapter"]
                            it.requestData = requestData
                            it.attractors = requestData?.toAttractors
                            it.cachedCalls.clear()
                            it.responseData = responseData
                        }

                        tape.saveIfExists()

                        call.redirect_abs(TapeRouting.RoutePaths.EDIT.asSubPath) {
                            parameters.clear()
                            parameters["tape"] = tape.name
                            parameters["chapter"] = newChap.name
                        }
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
            unsafeScript { +JsUtils.Functions.all }
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

            val genResponses = (actChap.genResponses ?: mutableListOf())
                .apply {
                    actChap.responseData?.also { add(0, it) }
                }

            val itemName = params["item"]
            val item = when (itemName?.trim()) {
                null -> null
                "" -> genResponses
                    .minByOrNull { it.recordedDate ?: Date() }

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
                toggleArea {
                    var usingURL: HttpUrl? = null
                    table {
                        tr {
                            th { +"Method" }
                            td {
                                toggleArea {
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
                                            inlineDiv
                                            if (!isValid) disabledText
                                        }
                                        br()

                                        textInput {
                                            width = "6em"
                                            readonly = true
                                            disabled = true
                                            if (isValid) readonlyBG else disabledBG
                                            value = data ?: R["noData", ""]
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        val data = actChap.recentRequest?.method
                                        val dataDefault = data?.toUpperCase() ?: HttpMethod.Get.value
                                        radioInput(name = "reqMethod") {
                                            id = "reqMethodCustom"
                                            value = dataDefault
                                            checked = data != null || useCustom
                                        }
                                        infoText("Custom Method") { inlineDiv }
                                        br()

                                        select {
                                            onChange = "reqMethodCustom.value = selectedIndex;"
                                            HttpMethod.DefaultMethods.forEach {
                                                option {
                                                    if (dataDefault == it.value)
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
                                width = "20%"
                                +"Request URL"
                            }
                            td {
                                toggleArea {
                                    div(classes = "radioDiv") {
                                        usingURL = actTape.routingUrl.asHttpUrl
                                        val data = usingURL.let { it?.hostPath ?: R["noData", ""] }
                                        val isValid = data.isValidURL
                                        useCustom = !isValid
                                        radioInput(name = "reqUrl") {
                                            value = data
                                            checked = isValid
                                            disabled = !isValid
                                        }
                                        infoText("Tape URL") {
                                            inlineDiv
                                            if (!isValid) disabledText
                                        }
                                        br()

                                        textInput(classes = "hoverExpand") {
                                            readonly = true
                                            disabled = !isValid
                                            if (isValid) readonlyBG else disabledBG
                                            value = data
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        usingURL = actChap.requestData?.url.asHttpUrl
                                        val data = usingURL.let { it?.hostPath ?: R["noData", ""] }
                                        val isValid = data.isValidURL
                                        useCustom = !isValid
                                        radioInput(name = "reqUrl") {
                                            value = data
                                            checked = isValid
                                            disabled = !isValid
                                        }
                                        infoText("Chapter URL") {
                                            inlineDiv
                                            if (!isValid) disabledText
                                        }
                                        br()

                                        textInput(classes = "hoverExpand") {
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
                                        infoText("Custom URL") { inlineDiv }
                                        br()

                                        textInput(classes = "hoverExpand") {
                                            placeholder = data ?: R["urlPlaceholderExample", ""]
                                            value = data.orEmpty()
                                            onKeyUp = "reqUrlCustom.value = value;"
                                        }
                                    }
                                }
                            }
                        }

                        tr {
                            th { +"Queries" }
                            td {
                                toggleArea {
                                    tooltipText("Info", "genKVDataField")
                                    br()

                                    div(classes = "radioDiv") {
                                        val data = actChap.requestData?.url.asHttpUrl.toParameters
                                        val isValid = data?.isEmpty().isFalse
                                        useCustom = !isValid
                                        radioInput(name = "reqQuery") {
                                            checked = isValid
                                            disabled = !isValid
                                            value = (data ?: "").toString()
                                        }
                                        infoText("Chapter Queries") {
                                            inlineDiv
                                            if (!isValid) disabledText
                                        }
                                        br()

                                        paramTextArea(data) {
                                            readonly = true
                                            disabled = !isValid
                                            if (isValid) readonlyBG else disabledBG
                                            setMinMaxSizes("12em", "100%", "4em", "20em")
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        val data = actChap.recentRequest?.url.asHttpUrl.toParameters
                                        radioInput(name = "reqQuery") {
                                            id = "reqCustomQuery"
                                            checked = data?.isEmpty().isFalse || useCustom
                                            value = (data ?: "").toString()
                                        }
                                        infoText("Custom Queries") { inlineDiv }
                                        br()

                                        paramTextArea(data) {
                                            onKeyPress = "keypressNewlineEnter(this);"
                                            onKeyUp = "reqCustomQuery.value = value;"
                                            setMinMaxSizes("12em", "100%", "4em", "20em")
                                        }
                                    }
                                }
                            }
                        }

                        tr {
                            th { +"Headers" }
                            td {
                                toggleArea {
                                    tooltipText("Info", "genKVDataField")
                                    br()

                                    div(classes = "radioDiv") {
                                        val data = actChap.requestData?.headers
                                        val isValid = (data?.size ?: 0) > 0
                                        useCustom = !isValid
                                        radioInput(name = "reqHeaders") {
                                            checked = isValid
                                            disabled = !isValid
                                            value = (data ?: "").toString().trim()
                                        }
                                        infoText("Chapter Headers") {
                                            inlineDiv
                                            if (!isValid) disabledText
                                        }
                                        br()

                                        headerTextArea(data) {
                                            readonly = true
                                            disabled = !isValid
                                            if (isValid) readonlyBG else disabledBG
                                            setMinMaxSizes("12em", "100%", "4em", "20em")
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        val data = actChap.recentRequest?.headers
                                        radioInput(name = "reqHeaders") {
                                            id = "customHeaders"
                                            value = (data ?: "").toString()
                                            checked = (data?.size ?: 0) > 0 || useCustom
                                        }
                                        infoText("Custom Headers") { inlineDiv }
                                        br()

                                        headerTextArea(data) {
                                            onKeyPress = "keypressNewlineEnter(this);"
                                            onKeyUp = "customHeaders.value = value.trim();"
                                            setMinMaxSizes("12em", "100%", "4em", "20em")
                                        }
                                    }
                                }
                            }
                        }

                        tr {
                            th { +"Body" }
                            td {
                                toggleArea {
                                    div(classes = "radioDiv") {
                                        val data = actChap.requestData?.body
                                        val isValid = !data.isNullOrEmpty()
                                        useCustom = !isValid
                                        radioInput(name = "reqBody") {
                                            checked = isValid
                                            disabled = !isValid
                                            value = data.orEmpty()
                                        }
                                        infoText("Chapter Body") {
                                            inlineDiv
                                            if (!isValid) disabledText
                                        }
                                        br()

                                        textArea {
                                            readonly = true
                                            disabled = !isValid
                                            if (isValid) readonlyBG else disabledBG
                                            val outStr = data.tryAsPrettyJson.orEmpty()
                                            val longestStr = outStr.longestLine

                                            width = "${(longestStr?.length ?: 18) - 6}em"
                                            val lineCnt = outStr.lines().size
                                            height = "${lineCnt + 2}em"
                                            setMinMaxSizes("12em", "100%", "4em", "20em")

                                            +outStr
                                        }
                                    }

                                    div(classes = "radioDiv") {
                                        val data = actChap.recentRequest?.body
                                        radioInput(name = "reqBody") {
                                            id = "reqBodyCustom"
                                            checked = !data.isNullOrEmpty() || useCustom
                                            value = data.orEmpty()
                                        }
                                        infoText("Custom Body") { inlineDiv }
                                        br()

                                        textArea {
                                            onKeyPress = "keypressNewlineEnter(this);"
                                            onKeyUp = "reqBodyCustom.value = value;"

                                            val outStr = data.tryAsPrettyJson.orEmpty()
                                            val longestStr = outStr.longestLine

                                            width = "${(longestStr?.length ?: 0) - 6}em"
                                            val lineCnt = outStr.lines().size
                                            height = "${lineCnt + 2}em"
                                            setMinMaxSizes("12em", "100%", "4em", "20em")

                                            +outStr
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                linebreak()

                h2 { +"Response" }
                table {
                    tr {
                        td {
                            style = "text-align: center;"
                            postButton(name = "Action") {
                                value = ResponseActions.Make.name
                                formAction = RoutePaths.ResponseGen.path
                                +"Make call"
                            }

                            br()
                            hiddenInput(name = "useLocalhost") {
                                id = name
                                disabled = true
                            }
                            postButton(name = "Action") {
                                value = ResponseActions.Make.name
                                formAction = RoutePaths.ResponseGen.path
                                onClick = "useLocalhost.disabled = false;"
                                +"Localhost call"
                            }
                        }

                        td {
                            style = "text-align: center;"
                            if (genResponses.isEmpty())
                                +R["noData", ""] else {
                                select {
                                    name = "selectResponse"
                                    id = name
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
                                +" "

                                hiddenInput(name = "item") {
                                    id = name
                                    disabled = true
                                }

                                getButton {
                                    formAction = Response.path
                                    onClick = """
                                            item.disabled = false;
                                            item.value = selectResponse.value;
                                        """.trimIndent()
                                    +"View"
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
                            val isLocalhostCall = actItem?.isLocalhostCall.isTrue

                            postButton(name = "Action") {
                                formAction = RoutePaths.ResponseGen.path
                                value = ResponseActions.Use.name
                                disabled = isLocalhostCall || genResponses.isEmpty()
                                +"Use call -> Chapter"
                            }

                            br()
                            div {
                                postButton(name = "Action") {
                                    formAction = RoutePaths.ResponseGen.path
                                    value = ResponseActions.NewChapter.name
                                    disabled = isLocalhostCall || genResponses.isEmpty()
                                    +"Use call -> New Chapter"
                                }
                                +" "
                                textInput(name = "newChapter") {
                                    disabled = isLocalhostCall || genResponses.isEmpty()
                                    if (genResponses.isEmpty())
                                        disabledBG
                                    placeholder = "New Chapter name"
                                }
                            }
                        }
                    }
                }
                toggleArea(item != null) {
                    if (actItem != null) {
                        val divStyle = """
                            resize: vertical;
                            min-height: 10em;
                            overflow-y: scroll;
                            border: 1px solid;
                            height: 10em;
                        """.trimIndent()

                        table {
                            id = "resultTable"
                            tr {
                                th {
                                    width = "20%"
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
                                    div {
                                        style = divStyle
                                        val headerMap = actItem.headers?.toMultimap()

                                        if (headerMap.isNullOrEmpty())
                                            +R["noData", ""]
                                        else
                                            table {
                                                thead {
                                                    tr {
                                                        th {
                                                            resizableCol
                                                            +"Key"
                                                        }
                                                        th { +"Value" }
                                                    }
                                                }
                                                tbody {
                                                    wordBreak_word
                                                    headerMap.forEach { (t, u) ->
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
                            }

                            tr {
                                th {
                                    +"Body"
                                    val isBase64 = actItem.headers.orDefault.contentType.orDefault
                                        .match(ContentType.Image.Any)
                                    if (isBase64)
                                        infoText("[Base64]")
                                }
                                td {
                                    textArea {
                                        style = "width: -webkit-fill-available;$divStyle"
                                        readonlyBG
                                        readonly = true
                                        +actItem.body.orEmpty()
                                    }
                                    unsafeScript {
                                        +"beautifyField(getScriptElem().previousElementSibling);"
                                    }
                                }
                            }
                        }
                    } else +R["noData", ""]
                }
            }
        }
    }

    fun Parameters.asResponseCall(): ResponseResultOf<String> {
        val method = this["reqMethod"].orEmpty().toUpperCase()
        val isLocalhostCall = get("useLocalhost") != null
        val url = get("reqUrl").orEmpty().ensureHttpPrefix.let {
            if (isLocalhostCall) {
                it.toHttpUrlOrNull().reHost("0.0.0.0").rePort(Ports.mock).toString()
            } else it
        }
        val params = this["reqQuery"]
            .toPairs()?.toList()
        val headers = this["reqHeaders"].toPairs()
            ?.map { it.first to it.second }?.toMutableList()
            ?.apply {
                if (isLocalhostCall) add(("localhost" to "true"))
            }?.toTypedArray()

        val body = this["reqBody"]

        var request = when (HttpMethod.parse(method)) {
            HttpMethod.Post -> url.httpPost(params)
            else -> url.httpGet(params)
        }

        if (headers != null)
            request = request.header(*headers)

        if (!body.isNullOrBlank())
            request = request.body(body)

        return request.responseString()
    }
}
