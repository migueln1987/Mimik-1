package networkRouting

import TapeCatalog
import VCRConfig
import helpers.RandomHost
import helpers.attractors.RequestAttractorBit
import helpers.getFolders
import helpers.isTrue
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
import tapeItems.BlankTape
import helpers.attractors.RequestAttractors
import mimikMockHelpers.InteractionUseStates

@Suppress("RemoveRedundantQualifierName")
class TapeRouting(path: String) : RoutingContract(path) {

    private val tapeCatalog by lazy { TapeCatalog.Instance }
    private val randomHost = RandomHost()

    private val subDirectoryDefault = "[ Default Directory ]"
    private val subDirectoryCustom = "[ Custom Directory ]"

    private val queryParamKey = "QueryParamKey"
    private val queryParamValue = "QueryParamValue"
    private val queryParamOpt = "QueryParamOpt"

    enum class RoutePaths(val path: String) {
        ALL("all"),
        EDIT("edit"),
        DELETE("delete"),
        ACTION("action"),
        CREATE("create");
    }

    val RoutePaths.asSubPath
        get() = this@TapeRouting.path + "/" + this.path

    override fun init(route: Routing) {
        route.route(path) {
            all
            action
            edit
            delete
            create
            get { call.respondRedirect(RoutePaths.ALL.asSubPath) }
        }
    }

    private val Route.all
        get() = route(RoutePaths.ALL.path) {
            get { call.respondHtml { getViewAllPage() } }
            post { call.respondRedirect(RoutePaths.ALL.path) }
        }

    private val Route.action
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

    private val Route.edit
        get() = route(RoutePaths.EDIT.path) {
            get {
                call.respondHtml {
                    if (call.parameters.contains("tape")) {
                        if (call.parameters.contains("chapter")) {
                            getEditChapterPage(call.parameters)
                        } else
                            getEditTapePage(call.parameters)
                    }
                }
            }

            post { call.respondRedirect(path) }
        }

    private val Route.delete
        get() = route(RoutePaths.DELETE.path) {
            get {
                tapeCatalog.tapes.firstOrNull { it.tapeName == call.parameters["tape"] }
                    ?.also { tape ->
                        val chapterName = call.parameters["chapter"]
                        if (chapterName == null) {
                            tapeCatalog.tapes.remove(tape)
                            if (tape.file?.exists().isTrue())
                                tape.file?.delete()
                        } else {
                            tape.chapters.removeIf { it.chapterName == chapterName }
                            call.respondRedirect(RoutePaths.EDIT.path)
                            return@get
                        }
                    }

                call.respondRedirect(RoutePaths.ALL.path)
            }

            post {
                call.respondText("delete page")
            }
        }

    private val Route.create
        get() = route(RoutePaths.CREATE.path) {
            get { call.respondHtml { getCreateTape() } }
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

            "SaveViewAllTapes" -> {
                data.saveToTape()
                respondRedirect(RoutePaths.ALL.path)
                return
            }

            else -> Unit
        }

        when (data["Action"]) {
            "SaveToHardTape" -> {
                tapeCatalog.tapes
                    .firstOrNull { it.tapeName == data["tape"] }
                    ?.saveFile()
                respondRedirect(RoutePaths.ALL.path)
            }

            "Edit" -> {
                respondRedirect {
                    path(path, RoutePaths.EDIT.path)
                    parameters.apply {
                        data.filterNot { it.key == "Action" }
                            .forEach { (t, u) -> append(t, u) }
                    }
                }
            }

            "Delete" -> {
                respondRedirect {
                    path(path, RoutePaths.DELETE.path)
                    val filterKeys = listOf("tape", "chapter")
                    parameters.apply {
                        data.asSequence()
                            .filter { filterKeys.contains(it.key) }
                            .forEach { (t, u) -> append(t, u) }
                    }
                }
            }

            else -> respondRedirect(RoutePaths.ALL.path)
        }
    }

    private fun Map<String, String>.saveToTape(): BlankTape {
        return BlankTape.Builder { tape ->
            // subDirectory = get("SubDirectory")?.trim()
            tape.tapeName = get("TapeName")?.trim() ?: randomHost.value.toString()
            tape.routingURL = get("RoutingUrl")?.trim()
            tape.allowLiveRecordings = get("SaveRecordings") == "on"

            tape.attractors = RequestAttractors { attr ->
                get("RoutingPath")?.trim()?.also { path ->
                    attr.routingPath?.value = path
                }

                if (keys.any { it.startsWith(queryParamKey) }) {
                    val keys = asSequence()
                        .filter { it.value.isNotBlank() }
                        .filter { it.key.startsWith(queryParamKey) }
                        .associate { it.key.removePrefix(queryParamKey) to it.value }

                    val values = asSequence()
                        .filter { it.key.startsWith(queryParamValue) }
                        .associate { it.key.removePrefix(queryParamValue) to it.value }

                    val optionals = asSequence()
                        .filter { it.key.startsWith(queryParamOpt) }
                        .associate { it.key.removePrefix(queryParamOpt) to it.value }

                    attr.queryParamMatchers = keys.keys.map { key ->
                        RequestAttractorBit {
                            it.value = values.getValue(key)
                            it.optional = optionals.getOrDefault(key, "") == "on"
                        }
                    }
                }
            }
        }.build()
            .also {
                tapeCatalog.tapes.add(it)
                if (get("hardtape") == "on") it.saveFile()
            }
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
                        .tb_25wide {
                            width: 25%
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
                            if (t.file?.exists().isTrue()) {
                                p { +"File path: ${t.file?.path}" }
                                p { +"File size: ${t.file?.length()} bytes" }
                            } else {
                                postForm(
                                    action = RoutePaths.ACTION.path,
                                    encType = FormEncType.multipartFormData
                                ) {
                                    hiddenInput(name = "tape") { value = t.tapeName }
                                    button(name = "Action") {
                                        value = "SaveToHardTape"
                                        text("Save tape as a hard tape")
                                    }
                                }
                            }
                            p { +"Recordings: ${t.chapters.size}" }

                            if (t.chapters.isNotEmpty()) {
                                p {
                                    val recAlways =
                                        t.chapters.count {
                                            it.mockUses == InteractionUseStates.ALWAYS.state
                                        }
                                    val recDisabled =
                                        t.chapters.count {
                                            it.mockUses == InteractionUseStates.DISABLE.state
                                        }
                                    val recMemory =
                                        t.chapters.count { it.mockUses > 0 }
                                    val recExpired =
                                        t.chapters.count {
                                            it.mockUses == InteractionUseStates.DISABLEDMOCK.state
                                        }

                                    table {
                                        tr {
                                            if (recAlways > 0)
                                                th(classes = "tb_25wide") { +"Always" }
                                            if (recDisabled > 0)
                                                th(classes = "tb_25wide") { +"Disabled" }
                                            if (recMemory > 0)
                                                th(classes = "tb_25wide") { +"In-Memory" }
                                            if (recExpired > 0)
                                                th(classes = "tb_25wide") { +"In-Memory (Expired)" }
                                        }
                                        tr {
                                            if (recAlways > 0)
                                                td(classes = "tb_25wide") { text(recAlways) }
                                            if (recDisabled > 0)
                                                td(classes = "tb_25wide") { text(recDisabled) }
                                            if (recMemory > 0)
                                                td(classes = "tb_25wide") { text(recMemory) }
                                            if (recExpired > 0)
                                                td(classes = "tb_25wide") { text(recExpired) }
                                        }
                                    }
                                }
                            }

                            br()

                            if (t.isUrlValid) {
                                p { +"Routing URL: ${t.httpRoutingUrl!!}" }
                            } else {
                                p { +"Routing URL: [ Invalid ]" }
                            }

                            if (!t.attractors?.routingPath?.value.isNullOrBlank()) {
                                p { +"Routing Path: ${t.attractors?.routingPath}" }
                            }

                            if (t.attractors?.queryParamMatchers?.isNotEmpty().isTrue()) {
                                p { +"Routing Query: ${t.attractors?.queryParamMatchers?.size}" }
                            }
                        }

                        td {
                            postForm(
                                action = RoutePaths.ACTION.path,
                                encType = FormEncType.multipartFormData
                            ) {
                                hiddenInput(name = "tape") { value = t.tapeName }
                                p {
                                    submitInput(name = "Action") {
                                        value = "Edit"
                                        disabled = true
                                    }
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
        val randomValStr = randomHost.valueAsChars
        val currentPath = VCRConfig.getConfig.tapeRoot.get().path

        val folders = mutableListOf(subDirectoryDefault)
            .apply { addAll(VCRConfig.getConfig.tapeRoot.get().getFolders()) }

        body {
            setupStyle()
            script {
                unsafe {
                    raw(
                        """
                            var queryParamID = 0
                            function addNewParamFilter() {
                                var newrow = FilterParamTable.insertRow(FilterParamTable.rows.length-1);
                                
                                var newKey = newrow.insertCell(0);
                                var newKeyInput = createNewInput("$queryParamKey", queryParamID);
                                newKey.appendChild(newKeyInput);
                                
                                var newValue = newrow.insertCell(1);
                                var newValueInput = createNewInput("$queryParamValue", queryParamID);
                                newValue.appendChild(newValueInput);
                                
                                var newIsOptional = newrow.insertCell(2)
                                var newIsOptionalInput = createOptionalCheckbox("$queryParamOpt", queryParamID)
                                newIsOptional.appendChild(newIsOptionalInput);
                                queryParamID++
                                
                                var deleteBtn = newrow.insertCell(3);
                                deleteBtn.appendChild(createDeleteBtn());
                            }
                            
                            function createNewInput(fieldType, fieldID) {
                                var inputField = document.createElement("input");
                                inputField.name = fieldType + fieldID;
                                inputField.type = "text";
                                return inputField;
                            }
                            
                             function createOptionalCheckbox(fieldType, fieldID) {
                                var inputField = document.createElement("input");
                                inputField.name = fieldType + fieldID;
                                inputField.type = "checkbox";
                                inputField.text = "Optional";
                                return inputField;
                            }
                            
                            function createDeleteBtn() {
                                var deleteBtn = document.createElement("button");
                                deleteBtn.type = "button";
                                deleteBtn.innerText = "Delete";
                                deleteBtn.setAttribute('onclick', 'this.parentNode.parentNode.remove()');
                                return deleteBtn;
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
                        th { +"Sub directory" }
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
                                }

                                text(" ")

                                button(type = ButtonType.button) {
                                    onClick = "TapeName.value = \"$randomVal\""
                                    +"Use generated number"
                                }

                                button(type = ButtonType.button) {
                                    onClick = "TapeName.value = \"$randomValStr\""
                                    +"Use generated char string"
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
                                    onKeyUp = "SaveRecordings.disabled = value.length == 0"
                                }
                            }
                            br()
                            div(classes = "infoText") {
                                +R.getProperty("tapeRoutingUrlInfo")
                            }
                        }
                    }

                    tr {
                        th { +"Save options" }
                        td {
                            div {
                                text("Allow new recordings by filters -")
                                checkBoxInput(name = "SaveRecordings") {
                                    id = "SaveRecordings"
                                    disabled = true
                                    value = "true"
                                }
                                div(classes = "infoText") {
                                    text(R.getProperty("tapeSaveNewCallsInfo"))
                                }
                                div(classes = "infoText") {
                                    text("Adding mocks are unaffected.")
                                }
                            }
                            br()
                            div {
                                text("Save tape to file -")
                                checkBoxInput(name = "hardtape") {}
                                div(classes = "infoText") {
                                    +"Checked: Creating this tape will also save the tape to file."
//                                +R.getProperty("tapeRoutingUrlInfo")
                                }
                            }
                        }
                    }

                    tr {
                        th { +"Request Attractors" }
                        td {
                            div(classes = "infoText") {
                                +R.getProperty("tapeAttractorsInfo")
                            }
                            br()
                            table {
                                tr {
                                    th { +"Path" }
                                    td {
                                        textInput(name = "RoutingPath") {
                                            placeholder = "sub/path/here"
                                        }
                                    }
                                }

                                tr {
                                    th { +"Parameter" }
                                    td {
                                        table {
                                            thead {
                                                tr {
                                                    th { +"Key" }
                                                    th { +"Value" }
                                                    th { +"Optional" }
                                                    th { +"" }
                                                }
                                            }
                                            tbody {
                                                // QueryTableBody
                                                id = "FilterParamTable"
                                                tr {
                                                    td {
                                                        button(type = ButtonType.button) {
                                                            onClick = "addNewParamFilter()"
                                                            +"Add new Filter"
                                                        }
                                                    }
                                                }
                                            }
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
                                value = "SaveViewAllTapes"
                                id = "SaveViewAllTapes"
                                +"Save and goto View Tapes"
                            }
                            button(name = "CreateTape", classes = "btn_50wide") {
                                value = "SaveAddChapters"
                                id = "SaveAddChapters"
                                +"Save and add Tape Chapters"
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
        val activeTape = tapeCatalog.tapes
            .firstOrNull { it.tapeName == params["tape"] }
        val activeChapter = activeTape?.chapters
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
