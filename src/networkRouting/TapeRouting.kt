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
import helpers.removePrefixs
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.RecordedInteractions

@Suppress("RemoveRedundantQualifierName")
class TapeRouting(path: String) : RoutingContract(path) {

    private val tapeCatalog by lazy { TapeCatalog.Instance }
    private val randomHost = RandomHost()

    private val subDirectoryDefault = "[ Default Directory ]"
    private val subDirectoryCustom = "[ Custom Directory ]"

    private val filterParamValue = "filterParamValue"
    private val filterParamOpt = "filterParamOpt"
    private val filterParamExcept = "filterParamExcept"
    private val loadFlag = "_load_"

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
                            getTapePage(call.parameters)
                    }
                }
            }

            post { call.respondRedirect(path) }
        }

    private val Route.delete
        get() = route(RoutePaths.DELETE.path) {
            get {
                tapeCatalog.tapes.firstOrNull { it.name == call.parameters["tape"] }
                    ?.also { tape ->
                        val chapterName = call.parameters["chapter"]
                        if (chapterName == null) {
                            tapeCatalog.tapes.remove(tape)
                            if (tape.file?.exists().isTrue())
                                tape.file?.delete()
                        } else {
                            tape.chapters.removeIf { it.name == chapterName }
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
            get { call.respondHtml { getTapePage(call.parameters) } }
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
                    parameters.append("Tape", newTape.name)
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
            "SaveTape" -> {
                data.saveToTape()
                respondRedirect {
                    path(RoutePaths.EDIT.asSubPath)
                    parameters.apply {
                        append("tape", data["tape"] ?: "")
                    }
                }
            }

            "SaveToHardTape" -> {
                val foundTape = tapeCatalog.tapes
                    .firstOrNull { it.name == data["tape"] }
                    ?.also { it.saveFile() }

                respondRedirect {
                    if (foundTape != null && data["resumeEdit"] == "true") {
                        path(RoutePaths.EDIT.asSubPath)
                        parameters.apply {
                            append("tape", data["tape"] ?: "")
                        }
                    } else path(RoutePaths.ALL.asSubPath)
                }
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

            "Remove" -> {
                tapeCatalog.tapes.firstOrNull { it.name == parameters["tape"] }
                    ?.also { tape ->
                        parameters["chapter"]?.also { chap ->
                            tape.chapters.removeIf { it.name == chap }
                            respondRedirect(RoutePaths.EDIT.path)
                            return
                        } ?: tapeCatalog.tapes.remove(tape)
                    }

                respondRedirect(RoutePaths.ALL.path)
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
            // subDirectory = get("Directory")?.trim()
            tape.tapeName = get("TapeName")?.trim() ?: randomHost.value.toString()
            tape.routingURL = get("RoutingUrl")?.trim()
            tape.allowLiveRecordings = get("SaveNewCalls") == "on"

            tape.attractors = RequestAttractors { attr ->
                get("filterPath")?.trim()?.also { path ->
                    if (path.isNotEmpty())
                        attr.routingPath?.value = path
                }

                if (keys.any { it.startsWith(filterParamValue) }) {
                    val values = asSequence()
                        .filter { it.value.isNotBlank() }
                        .filter { it.key.startsWith(filterParamValue) }
                        .associateBy(
                            { it.key.removePrefixs(filterParamValue, loadFlag) },
                            { it.value }
                        )

                    val optionals = asSequence()
                        .filter { it.key.startsWith(filterParamOpt) }
                        .associateBy(
                            { it.key.removePrefixs(filterParamOpt, loadFlag) },
                            { it.value }
                        )

                    val excepts = asSequence()
                        .filter { it.key.startsWith(filterParamExcept) }
                        .associateBy(
                            { it.key.removePrefixs(filterParamExcept, loadFlag) },
                            { it.value }
                        )

                    attr.queryParamMatchers = values.keys.map { key ->
                        RequestAttractorBit {
                            it.value = values.getValue(key)
                            it.optional = optionals.getOrDefault(key, "") == "on"
                            it.except = excepts.getOrDefault(key, "") == "on"
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
                        .center{ text-align: center; }
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
                        th(classes = "center") { +t.name }

                        td {
                            if (t.file?.exists().isTrue()) {
                                p { +"File path: ${t.file?.path}" }
                                p { +"File size: ${t.file?.length()} bytes" }
                            } else {
                                postForm(
                                    action = RoutePaths.ACTION.path,
                                    encType = FormEncType.multipartFormData
                                ) {
                                    hiddenInput(name = "tape") { value = t.name }
                                    button(name = "Action") {
                                        value = "SaveToHardTape"
                                        +"Save tape as a hard tape"
                                    }
                                }
                            }

                            displayTapeRecInfo(t)

                            br()

                            p {
                                +"Routing URL: %s".format(
                                    when {
                                        t.routingUrl == null -> "{ no routing url }"
                                        t.isUrlValid -> t.httpRoutingUrl!!
                                        else -> "{ Invalid }"
                                    }
                                )
                            }
                            displayTapeAttrInfo(t.attractors)
                        }

                        td {
                            postForm(
                                action = RoutePaths.ACTION.path,
                                encType = FormEncType.multipartFormData
                            ) {
                                hiddenInput(name = "tape") { value = t.name }
                                p {
                                    submitInput(name = "Action") { value = "Edit" }
                                }
                                p {
                                    submitInput(name = "Action") {
                                        onClick = """
                                            if(confirm(${R.getProperty("viewTapeRemoveInfo")}))
                                                submit();
                                            else
                                                type="button";
                                        """.trimIndent()
                                        value = "Remove"
                                    }
                                }

                                if (t.file?.exists() == true)
                                    p {
                                        submitInput(name = "Action") {
                                            onClick = """
                                            if(confirm(${R.getProperty("viewTapeDeleteInfo")}))
                                                submit();
                                            else
                                                type="button";
                                        """.trimIndent()
                                            value = "Delete"
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
     * Renders the [total, live, limited, mock] recordings of this [tape] as a table.
     * Note: Nothing is rendered if there are no chapter data
     */
    private fun FlowContent.displayTapeRecInfo(tape: BlankTape) {
        if (tape.chapters.isEmpty()) {
            p { +"{ no chapters }" }
            return
        }

        p {
            table {
                thead {
                    tr {
                        td()
                        th { +"Total" }
                        th { +"Live" }
                        th { +"Mock" }
                        th { +"Await" }
                    }
                }
                tbody {
                    tr {
                        th { +"Enabled" }
                        td { +tape.chapters.count { MockUseStates.isEnabled(it.mockUses) } }
                        td {
                            +tape.chapters.count {
                                MockUseStates.isEnabled(it.mockUses) &&
                                        it.alwaysLive ?: false
                            }.toString()
                        }
                        td {
                            +tape.chapters.count {
                                MockUseStates.isEnabled(it.mockUses) &&
                                        !MockUseStates.isLimitedMock(it.mockUses)
                            }.toString()
                        }
                        td {
                            +tape.chapters.count {
                                MockUseStates.isEnabled(it.mockUses) &&
                                        it.awaitResponse
                            }.toString()
                        }
                    }

                    tr {
                        th { +"Disabled" }
                        td { +tape.chapters.count { MockUseStates.isDisabled(it.mockUses) } }
                        td {
                            +tape.chapters.count {
                                MockUseStates.isDisabled(it.mockUses) &&
                                        it.alwaysLive ?: false
                            }.toString()
                        }
                        td {
                            +tape.chapters.count {
                                MockUseStates.isDisabled(it.mockUses) &&
                                        !MockUseStates.isLimitedMock(it.mockUses)
                            }.toString()
                        }
                        td {
                            +tape.chapters.count {
                                MockUseStates.isDisabled(it.mockUses) &&
                                        it.awaitResponse
                            }.toString()
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.displayTapeAttrInfo(attr: RequestAttractors?) {
        if (attr == null || !attr.hasData) {
            p { +"{ no attractors }" }
            return
        }

        table {
            thead {
                tr {
                    td(classes = "center") { +"Attractors" }
                    th { +"Route" }
                    th { +"Param" }
                    th { +"Header" }
                }
            }

            tbody {
                tr {
                    th { +"Count" }
                    td {
                        +when (attr.routingPath?.value?.trim()) {
                            null, "" -> "null"
                            else -> "1"
                        }
                    }
                    td { +attr.queryParamMatchers?.count().toString() }
                    td { +attr.queryHeaderMatchers?.count().toString() }
                }
            }
        }
    }

    /**
     * Create and Edit page for a single tape
     */
    private fun HTML.getTapePage(params: Parameters) {
        val randomVal = randomHost.nextRandom()
        val randomValStr = randomHost.valueAsChars
        val currentPath = VCRConfig.getConfig.tapeRoot.get().path

        val folders = mutableListOf(subDirectoryDefault)
            .apply { addAll(VCRConfig.getConfig.tapeRoot.get().getFolders()) }

        val tape = tapeCatalog.tapes
            .firstOrNull { it.name == params["tape"] }

        val expectedTape = params["tape"] != null
        val haveTape = tape != null
        val expectedTapeName = params["tape"]

        head {
            script {
                unsafe {
                    raw(
                        """
                            function preVerifyURL(url) {
                                if(url == null || url.length == 0)
                                    return "{ empty }";
                                var regex = /^((?:[a-z]{3,9}:\/\/)?(?:(?:(?:\d{1,3}\.){3}\d{1,3})|(?:[\w.-]+\.(?:[a-z]{2,10}))))/i;
                                var match = url.match(regex);
                                if(match == null) 
                                    return "{ no match }"; else return match[0];
                            }
                            
                            var queryParamID = 0;
                            function addNewParamFilter() {
                                var newrow = FilterParamTable.insertRow(FilterParamTable.rows.length-1);
                                
                                var filterValue = newrow.insertCell(0);
                                var valueInput = createTextInput("$filterParamValue", queryParamID);
                                filterValue.appendChild(valueInput);
                                
                                var filterFlags = newrow.insertCell(1);
                                var isOptionalInput = createCheckbox("$filterParamOpt", queryParamID);
                                filterFlags.appendChild(isOptionalInput);
                                filterFlags.append(" Optional");
                                filterFlags.appendChild(document.createElement("br"));

                                var isExceptInput = createCheckbox("$filterParamExcept", queryParamID);
                                filterFlags.appendChild(isExceptInput);
                                filterFlags.append(" Except");
                                queryParamID++;
                                
                                var actionBtns = newrow.insertCell(2);
                                actionBtns.appendChild(createDeleteBtn());
                            }
                            
                            function disableEnter(field) {
                                field.onkeydown = function(e) {return event.key != 'Enter';};
                            }
                            
                            function createTextInput(fieldType, fieldID) {
                                var inputField = document.createElement("input");
                                inputField.name = fieldType + fieldID;
                                disableEnter(inputField);
                                return inputField;
                            }
                            
                            function createCheckbox(fieldType, fieldID) {
                                var inputField = document.createElement("input");
                                inputField.name = fieldType + fieldID;
                                inputField.type = "checkbox";
                                return inputField;
                            }
                            
                            function createDeleteBtn() {
                                var deleteBtn = document.createElement("button");
                                deleteBtn.type = "button";
                                deleteBtn.innerText = "Delete";
                                deleteBtn.setAttribute('onclick', 'this.parentNode.parentNode.remove()');
                                return deleteBtn;
                            }
                            
                            function setRoutingUrlStates(url) {
                                parsedUrl.innerText = preVerifyURL(url);
                                var isDisabled = parsedUrl.innerText.startsWith("{");
                                
                                allowPassthrough.disabled = isDisabled;
                                var disableChapters = !isDisabled && allowPassthrough.checked
                                
                                SaveNewCalls.disabled = disableChapters;
                                SaveAddChapters.disabled = disableChapters;
                            }
                            
                            function setIsDisabled(divObj, newState) {
                                try {
                                    divObj.disabled = newState;
                                } catch (E) {}
                                
                                for (var x = 0; x < divObj.childNodes.length; x++) {
                                    setIsDisabled(divObj.childNodes[x], newState);
                                }
                            }
                        """.trimIndent()
                    )
                }
            }
        }

        body {
            setupStyle()

            getForm(action = RoutePaths.ALL.path) {
                button { +"..View All tapes" }
            }

            br()
            if (expectedTape && !haveTape)
                p { +"No tape with the name \"${params["tape"]}\" was found." }

            h1 { +(if (haveTape) "Tape Editor" else "New Tape") }

            form(encType = FormEncType.multipartFormData) {
                table {
                    tr {
                        hidden = true
                        th { +"Directory" }
                        td {
                            div {
                                textInput(name = "Directory") {
                                    id = "Directory"
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
                        th { +"Name" }
                        td {
                            val tapeNameAction = if (expectedTapeName != null)
                                "nameReset.hidden = tapeName.value == tapeName.placeholder;"
                            else ""

                            div {
                                textInput(name = "name") {
                                    disableEnterKey
                                    id = "TapeName"
                                    if (tape == null) {
                                        placeholder = expectedTapeName ?: randomVal.toString()
                                        value = expectedTapeName ?: randomVal.toString()
                                    } else {
                                        placeholder = tape.name
                                        value = if (tape.hasNameSet) tape.name else ""
                                    }
                                    onKeyUp = tapeNameAction
                                }

                                text(" ")
                                if (tape == null) {
                                    button(type = ButtonType.button) {
                                        onClick = "TapeName.value = $randomVal;$tapeNameAction"
                                        +"Use generated number"
                                    }

                                    button(type = ButtonType.button) {
                                        onClick =
                                            "TapeName.value = \"$randomValStr\";$tapeNameAction"
                                        +"Use generated char string"
                                    }
                                }

                                if (expectedTapeName != null)
                                    button(type = ButtonType.button) {
                                        id = "nameReset"
                                        hidden = true
                                        onClick = """
                                            tapeName.value = tapeName.placeholder;
                                            nameReset.hidden = true;
                                        """.trimIndent()
                                        +"Reset"
                                    }
                            }

                            if (tape != null && tape.file?.exists() == true) {
                                br()
                                text("Tape located at: ")
                                i {
                                    text(tape.file?.toString() ?: "{ error }")
                                }
                            }

                            div(classes = "infoText") {
                                hidden = true
                                +"Tape name. Example: 'General' becomes '/General.json'"
                            }
                        }
                    }

                    tr {
                        th { +"Routing URL" }
                        td {
                            textInput(name = "RoutingUrl") {
                                disableEnterKey
                                id = "RoutingUrl"
                                if (tape == null) {
                                    placeholder = "Example: http://google.com"
                                    value = ""
                                } else {
                                    placeholder = tape.routingUrl ?: ""
                                    value = tape.routingUrl ?: ""
                                }
                                size = "${placeholder.length + 20}"

                                onLoad = "setRoutingUrlStates(value)"
                                onKeyUp = "setRoutingUrlStates(value)"
                            }

                            if (tape != null) {
                                text(" ")
                                button {
                                    id = "urlValReset"
                                    hidden = true
                                    onClick = """
                                        RoutingUrl.value = RoutingUrl.placeholder;
                                        urlResetCheck();
                                    """.trimIndent()
                                    +"Reset"
                                }
                            }

                            br()
                            p {
                                text("Parsed url: ")
                                i {
                                    a {
                                        id = "parsedUrl"
                                        if (tape != null && !tape.routingUrl.isNullOrBlank())
                                            text(tape.routingUrl ?: "")
                                        else text("{ empty }")
                                    }
                                }
                            }

                            div(classes = "infoText") {
                                +R.getProperty("tapeRoutingUrlInfo")
                            }
                        }
                    }

                    tr {
                        th { +"Request Attractors" }
                        td {
                            div(classes = "infoText") {
                                +R.getProperty("tapeAttrInfo")
                            }
                            br()
                            table {
                                tr {
                                    th { +"Path" }
                                    td {
                                        textInput(name = "filterPath") {
                                            disableEnterKey
                                            var path = ""
                                            if (tape != null)
                                                path = tape.attractors?.routingPath?.value ?: ""

                                            placeholder = if (path.isBlank())
                                                "sub/path/here" else path
                                            value = path
                                        }
                                    }
                                }

                                tr {
                                    th { +"Parameter" }
                                    td {
                                        table {
                                            thead {
                                                tr {
                                                    th { +"Value" }
                                                    th { +"Flags" }
                                                    th { +"Actions" }
                                                }
                                            }
                                            tbody {
                                                // QueryTableBody
                                                id = "FilterParamTable"

                                                if (tape != null)
                                                    tape.attractors?.queryParamMatchers
                                                        ?.forEachIndexed { index, bit ->
                                                            appendBit(bit, index)
                                                        }

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

                                        div(classes = "infoText") {
                                            br()
                                            text(R.getProperty("tapeAttrFlagOpt"))
                                            br()
                                            text(R.getProperty("tapeAttrFlagExt"))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    tr {
                        th { +"Chapters" }
                        td {
                            div {
                                text("Allow call pass-through ")
                                checkBoxInput(name = "allowPassthrough") {
                                    id = "allowPassthrough"
                                    disabled = true
                                    if (tape != null)
                                        value = (tape.alwaysLive ?: false).toString()
                                    onChange = """
                                        setIsDisabled(SaveNewCalls, checked)
                                        setIsDisabled(ChapterData, checked)
                                        setIsDisabled(SaveAddChapters, checked)
                                    """.trimIndent()
                                }
                                div(classes = "infoText") {
                                    text(R.getProperty("tapeCallPassthroughInfo"))
                                }
                                br()

                                text("Allow new recordings by filters -")
                                checkBoxInput(name = "SaveNewCalls") {
                                    id = "SaveNewCalls"
                                    disabled = true
                                    if (tape != null)
                                        value = tape.isWritable.toString()
                                }
                                div(classes = "infoText") {
                                    text(R.getProperty("tapeSaveNewCallsInfo"))
                                    text("Adding mocks are unaffected.")
                                }
                            }

                            div {
                                id = "ChapterData"
                                if (tape != null) {
                                    br()
                                    displayTapeChapters(tape)
                                }
                            }

                            if (!expectedTape && !haveTape) {
                                br()
                                postButton(name = "CreateTape") {
                                    id = "SaveAddChapters"
                                    onClick = """
                                        type = "button";
                                        alert('Insert Create Chapter action');
                                    """.trimIndent()
                                    +"Save and add Tape Chapters"
                                }
                            }
                        }
                    }

                    tr {
                        th { +"Save Options" }
                        td {
                            div {
                                id = "Saveoptions"

                                if (tape != null) {
                                    if (tape.file?.exists() != true) {
                                        hiddenInput(name = "tape") { value = tape.name }
                                        hiddenInput(name = "resumeEdit") { value = "true" }

                                        postButton(name = "Action") {
                                            formAction = RoutePaths.ACTION.path
                                            value = "SaveToHardTape"
                                            +"Save tape as a hard tape"
                                        }
                                        div(classes = "infoText") {
                                            +" - Saves this tape to the '/$currentPath' directory "
                                        }

                                        br()
                                        br()
                                    }

                                    postButton(name = "Action") {
                                        formAction = RoutePaths.ACTION.path
                                        value = "SaveTape"
                                        +"Save tape data"
                                    }
                                } else {
                                    text("Save tape to file -")
                                    checkBoxInput(name = "hardtape") {}
                                    div(classes = "infoText") {
                                        +R.getProperty("tapeSaveHardInfo")
                                    }
                                    br()

                                    postButton(name = "CreateTape") {
                                        formAction = RoutePaths.ACTION.path
                                        id = "SaveViewAllTapes"
                                        value = "SaveViewAllTapes"
                                        +"Save and goto View Tapes"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DIV.displayTapeChapters(tape: BlankTape) {
        if (tape.chapters.isEmpty()) {
            getForm(action = RoutePaths.CREATE.path) {
                hiddenInput(name = "tape") { value = tape.name }
                hiddenInput(name = "chapter") { value = "" }
                button {
                    type = ButtonType.button
                    onClick = "alert('Insert Create Chapter action')"
                    +"Create new chapter"
                }
            }
        } else {
            table {
                thead {
                    tr {
                        th { +"Name" }
                        th { +"Pass-through" }
                        th { +"Attractors" }
                        th { +"Uses" }
                        th { +"" }
                    }
                }
                tbody {
                    tape.chapters.forEach { mock ->
                        addChapterRow(tape.name, mock)
                    }
                }
            }
            br()

            div(classes = "infoText") {
                +"Uses: Setting the value as '-1' will make it non-limited"
            }
        }
    }

    private fun TBODY.addChapterRow(tape: String, mock: RecordedInteractions) {
        tr {
            td { +mock.name }

            td { +(mock.alwaysLive ?: false).toString() }

            td {
                mock.attractors?.also { attr ->
                    table {
                        thead {
                            tr {
                                th { +"Routing" }
                                th { +"Param" }
                                th { +"Header" }
                                th { +"Body" }
                            }
                        }
                        tbody {
                            tr {
                                td { +(attr.routingPath?.value != null).toString() }
                                td {
                                    +(attr.queryParamMatchers
                                        ?.count { it.value != null }).toString()
                                }
                                td {
                                    +(attr.queryHeaderMatchers
                                        ?.count { it.value != null }).toString()
                                }
                                td {
                                    +(attr.queryBodyMatchers
                                        ?.count { it.value != null }).toString()
                                }
                            }
                        }
                    }
                } ?: text("{ none }")
            }

            td {
                text("Enabled: ")
                checkBoxInput {
                    id = mock.name + "_usesEnabled"
                    checked = MockUseStates.isEnabled(mock.mockUses)
                }
                br()

                numberInput {
                    id = mock.name + "_usesValue"
                    min = MockUseStates.ALWAYS.state.toString()
                    max = Int.MAX_VALUE.toString()
                    value = (if (mock.mockUses == MockUseStates.DISABLE.state)
                        MockUseStates.ALWAYS.state else mock.mockUses).toString()
                }
            }

            td {
                getForm(action = RoutePaths.EDIT.path) {
                    hiddenInput(name = "tape") { value = tape }
                    hiddenInput(name = "chapter") { value = mock.name }
                    button {
                        type = ButtonType.button
                        onClick = "alert('Insert Edit Chapter action')"
                        +"Edit"
                    }
                }
                br()

                getForm(action = RoutePaths.DELETE.path) {
                    hiddenInput(name = "tape") { value = tape }
                    hiddenInput(name = "chapter") { value = mock.name }
                    button {
                        type = ButtonType.button
                        onClick = "alert('Insert Delete Chapter action')"
                        +"Delete"
                    }
                }
            }
        }
    }

    private fun TBODY.appendBit(bit: RequestAttractorBit, count: Int) {
        tr {
            id = bit.hashCode().toString()
            td {
                textInput(name = "$filterParamValue$loadFlag$count") {
                    disableEnterKey
                    placeholder = bit.hardValue
                    value = bit.hardValue
                }

                // todo; reset value button
            }

            td {
                checkBoxInput(name = "$filterParamOpt$loadFlag$count") {
                    checked = bit.optional ?: false
                }
                text(" Optional")
                br()

                checkBoxInput(name = "$filterParamExcept$loadFlag$count") {
                    checked = bit.except ?: false
                }
                text(" Except")
            }

            td {
                button {
                    onClick = "this.parentNode.parentNode.remove()"
                    +"Delete"
                }

                button {
                    onClick = "this.parentNode.parentNode.remove()"
                    disabled = true
                    +"Clone"
                }
            }
        }
    }

    /**
     * Page to edit individual chapters in a tape
     */
    private fun HTML.getEditChapterPage(params: Parameters) {
        val activeTape = tapeCatalog.tapes
            .firstOrNull { it.name == params["tape"] }
        val activeChapter = activeTape?.chapters
            ?.firstOrNull { it.name == params["chapter"] }

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
            getForm(action = RoutePaths.ALL.path) {
                button { +"..View All tapes" }
            }

            getForm(action = RoutePaths.EDIT.path) {
                hiddenInput(name = "tape") { value = activeTape.name }
                button { +"View Active tape" }
            }

            br()
        }
    }
}
