package com.fiserv.mimik.networkRouting

import com.fiserv.mimik.TapeCatalog
import com.fiserv.mimik.VCRConfig
import com.fiserv.mimik.helpers.RandomHost
import com.fiserv.mimik.helpers.getFolders
import com.fiserv.mimik.tapeItems.BlankTape
import com.fiserv.mimik.tapeItems.RequestAttractors
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.Parameters
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.request.* // ktlint-disable no-wildcard-imports
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.* // ktlint-disable no-wildcard-imports
import kotlinx.html.* // ktlint-disable no-wildcard-imports
import okhttp3.HttpUrl

@Suppress("RemoveRedundantQualifierName")
class TapeRouting(path: String) : RoutingContract(path) {

    private val tapeCatalog = TapeCatalog.Instance
    private val randomHost = RandomHost()

    private val subDirectoryDefault = "[ Default Directory ]"
    private val subDirectoryCustom = "[ Custom Directory ]"

    companion object {
        internal var selfPath = ""
    }

    init {
        selfPath = path
    }

    enum class RoutePaths(private val value: String) {
        ALL("all"),
        EDIT("edit"),
        DELETE("delete"),
        ACTION("action"),
        CREATE("create");

        val path: String
            get() = "$selfPath/$value"
    }

    override fun init(route: Routing) {
        route.apply {
            all
            action
            edit
            delete
            create
        }
    }

    private val Routing.all
        get() = apply {
            get(RoutePaths.ALL.path) {
                call.respondHtml { getViewAllPage() }
            }
            post(RoutePaths.ALL.path) {
                call.respondRedirect(RoutePaths.ALL.path)
            }
        }

    private val Routing.action
        get() = post(RoutePaths.ACTION.path) {
            if (call.request.isMultipart()) {
                val values = call.receiveMultipart()
                    .readAllParts().asSequence()
                    .filterIsInstance<PartData.FormItem>()
                    .filterNot { it.name.isNullOrBlank() }
                    .map { it.name!! to it.value }
                    .toMap()

                call.processData(values)
            } else
                call.respondRedirect(RoutePaths.ALL.path)
        }

    private val Routing.edit
        get() = apply {
            val path = RoutePaths.EDIT.path
            get(path) {
                call.respondHtml {
                    if (call.parameters.contains("tape")) {
                        if (call.parameters.contains("chapter")) {
                            getEditChapterPage(call.parameters)
                        } else
                            getEditTapePage(call.parameters)
                    }
                }
            }

            post(path) { call.respondRedirect(path) }
        }

    private val Routing.delete
        get() = apply {
            get(RoutePaths.DELETE.path) {

                val tapeName = call.parameters["tape"]
                if (tapeName != null) {
                    val tape = tapeCatalog.tapes
                        .firstOrNull { it.tapeName == tapeName }
                    val chapterName = call.parameters["chapter"]
                    if (chapterName == null)
                        tapeCatalog.tapes.remove(tape)
                    else {
                        tape?.tapeChapters?.removeIf {
                            // todo; remove tape chapter
                            false
                        }
                    }
                }
                call.respondRedirect(RoutePaths.ALL.path)
            }
            post(RoutePaths.DELETE.path) {
                call.respondText("delete page")
            }
        }

    private val Routing.create
        get() = apply {
            get(RoutePaths.CREATE.path) {
                call.respondHtml { getCreateTape() }
            }
        }

    /**
     * Processes the POST "/Action" call
     */
    private suspend fun ApplicationCall.processData(data: Map<String, String>) {
        when (data["CreateTape"]) {
            "SaveAddChapters" -> {
                val newTape = data.saveToTape()
                tapeCatalog.tapes.add(newTape)
                respondRedirect {
                    encodedPath = RoutePaths.EDIT.path
                    parameters.append("Tape", newTape.tapeName)
                }
                return
            }

            "SaveViewTapes" -> {
                val newTape = data.saveToTape()
                tapeCatalog.tapes.add(newTape)
                respondRedirect(RoutePaths.ALL.path)
                return
            }

            else -> Unit
        }

        when (data["Action"]) {
            "Edit" -> {
                respondRedirect {
                    path(RoutePaths.EDIT.path.drop(1))
                    parameters.apply {
                        data.filterNot { it.key == "Action" }
                            .forEach { (t, u) -> append(t, u) }
                    }
                }
            }

            "Delete" -> {
                respondRedirect {
                    encodedPath = RoutePaths.DELETE.path
                    data.filter {
                        listOf(
                            "tape",
                            "chapter"
                        ).contains(it.key)
                    }.forEach { (t, u) ->
                        parameters.append(t, u)
                    }
                }
            }

            else -> respondRedirect(RoutePaths.ALL.path)
        }
    }

    private fun Map<String, String>.saveToTape(): BlankTape {
        return BlankTape.Builder() {
            subDirectory = get("SubDirectory")?.trim()
            tapeName = get("TapeName")?.trim() ?: randomHost.value.toString()
            attractors = RequestAttractors() {
                routingPath = get("RoutingPath")?.trim()
            }
            routingURL = get("RoutingUrl")?.trim()
        }.build()
    }

    private val CommonAttributeGroupFacade.disableEnterKey: Unit
        get() {
            onKeyDown = """
                return event.key != 'Enter';
            """.trimIndent()
        }

    private fun FlowOrMetaDataContent.setupStyle() {
        style {
            unsafe {
                raw(
                    """
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
                        .btn_50wide {
                            width: 50%
                        }
                        .infoText {
                            font-size: 14px;
                            color: #555
                        }
                        """.trimIndent()
                )
            }
        }
    }

    private fun HTML.getViewAllPage() {
        body {
            setupStyle()

            getForm(action = RoutePaths.CREATE.path) {
                button { +"Create new tape" }
            }
            br()

            if (tapeCatalog.tapes.isEmpty()) {
                h2 { +"No tapes were found." }
                h3 {
                    +"Click 'Create new tape' or route API calls through here to create tapes"
                }
                return@body
            }

            tapeCatalog.tapes.forEach { t ->
                table {
                    tr {
                        th { +t.tapeName }

                        td {
                            p { +"File path: ${t.file?.path}" }
                            p { +"Recordings: ${t.tapeChapters.size}" }

                            val routingUrl = t.HttpRoutingUrl
                            val isInvalidRouting = !t.routingUrl.isNullOrBlank() &&
                                    routingUrl == null
                            if (isInvalidRouting) {
                                p { +"Routing URL: [ Invalid ]" }
                            } else {
                                if (routingUrl != null)
                                    p { +"Routing URL: $routingUrl" }
                            }
                            if (!t.attractors?.routingPath.isNullOrBlank()) {
                                p { +"Routing Path: ${t.attractors?.routingPath}" }
                            }
                        }

                        td {
                            postForm(
                                action = RoutePaths.ACTION.path,
                                encType = FormEncType.multipartFormData
                            ) {
                                hiddenInput(name = "tape") { value = t.tapeName }
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

    private fun HTML.getCreateTape() {
        val randomVal = randomHost.nextRandom()
        val currentPath = VCRConfig.getConfig.tapeRoot.get().path

        val folders = mutableListOf(subDirectoryDefault)
            .apply { addAll(VCRConfig.getConfig.tapeRoot.get().getFolders()) }


        body {
            setupStyle()
            script {
                unsafe {
                    raw(
                        """
                            function updateSaveBtns() {
                                var isDisabled = !RoutingUrl.value.trim();
                                SaveAddChapters.disabled = isDisabled;
                                SaveViewTapes.disabled = isDisabled;
                            }
                        """.trimIndent()
                    )
                }
            }

            getForm(action = RoutePaths.ALL.path) {
                button { +"Back to View Tapes" }
            }

            br()

            postForm(
                action = RoutePaths.ACTION.path,
                encType = FormEncType.multipartFormData
            ) {
                disableEnterKey

                table {
                    tr {
                        th { +"Sub directory (optional)" }
                        td {
                            div {
                                textInput(name = "SubDirectory") {
                                    id = "SubDirectory"
                                    placeholder = "/$currentPath"
                                    value = ""
                                    onKeyUp = """
                                         definedSubDirectory.selectedIndex = 0;
                                        if(value.trim().length > 0) 
                                            definedSubDirectory.options[0].label = "$subDirectoryCustom";
                                        else
                                            definedSubDirectory.options[0].label = "$subDirectoryDefault";
                                        """.trimIndent()
                                }
                                +" "
                                select {
                                    name = "definedSubDirectory"
                                    id = "definedSubDirectory"
                                    onChange = """
                                        if(selectedIndex == 0)
                                            SubDirectory.value = "";
                                        else 
                                            SubDirectory.value = value;
                                        """.trimIndent()

                                    folders.forEachIndexed { index, s ->
                                        option {
                                            label = s
                                            value = when (index) {
                                                0 -> ""
                                                else -> s
                                            }
                                        }
                                    }
                                }
                            }
                            br()
                            div(classes = "infoText") {
                                +"Where the tape will be saved to"
                            }
                        }
                    }

                    tr {
                        th { +"Tape name" }
                        td {
                            div {
                                textInput(name = "TapeName") {
                                    id = "TapeName"
                                    placeholder = randomVal.toString()
                                    value = randomVal.toString()
                                    onKeyUp = "updateSaveBtns();"
                                }
                            }
                            br()
                            div(classes = "infoText") {
                                +"Tape name. Example: 'General' becomes '/General.json'"
                            }
                        }
                    }

                    tr {
                        th { +"Routing url" }
                        td {
                            div {
                                textInput(name = "RoutingUrl") {
                                    id = "RoutingUrl"
                                    placeholder = "Example: http://google.com"
                                    size = "${placeholder.length + 20}"
                                    value = ""
                                    onKeyUp = "updateSaveBtns();"
                                }
                            }
                            br()
                            div(classes = "infoText") {
                                +"The live URl which this tape will connect to, to get data"
                            }
                        }
                    }

                    tr {
                        th { +"Request Attractors (optional)" }
                        td {
                            div(classes = "infoText") {
                                +"Setting attractor values will allow new API calls to be added to this tape"
                            }
                            br()
                            table {
                                tr {
                                    th { +"Routing Path" }
                                    td {
                                        textInput(name = "RoutingPath") {
                                            placeholder = "sub/path/here"
                                            onKeyUp =
                                                "SaveViewTapes.hidden = value.trim().length == 0"
                                        }
                                    }
                                }
                            }
                        }
                    }

                    tr {
                        td()
                        td {
                            button(name = "CreateTape", classes = "btn_50wide") {
                                value = "SaveAddChapters"
                                id = "SaveAddChapters"
                                disabled = true
                                +"Save and add tape chapters"
                            }
                            button(name = "CreateTape", classes = "btn_50wide") {
                                value = "SaveViewTapes"
                                id = "SaveViewTapes"
                                hidden = true
                                disabled = true
                                +"Save and view tapes"
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
    private fun HTML.getEditTapePage(params: Parameters) {
    }

    /**
     * Page to edit individual chapters in a tape
     */
    private fun HTML.getEditChapterPage(params: Parameters) {
        val activeTape = tapeCatalog.tapes_old
            .firstOrNull { it.tapeName == params["tape"] }
        val activeChapter = activeTape?.tapeChapters
            ?.firstOrNull { it.chapterName == params["chapter"] }

        if (activeChapter == null) {
            body {
                h1 { +"Unable to process the request." }
                br()
                form {
                    postButton {
                        formAction = RoutePaths.ALL.path
                        +"..back to View tapes"
                    }
                    postButton {
                        formAction = RoutePaths.ALL.path
                        +"back to Edit tape"
                    }
                }
            }
            return
        }

        body {
            h1 {
                text(
                    "This page is intentionally left blank. " +
                            "Waiting for the \"Edit Chapter\" html page"
                )
            }
        }
    }
}
