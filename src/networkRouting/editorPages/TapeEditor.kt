package networkRouting.editorPages

import R
import VCRConfig
import com.google.gson.Gson
import helpers.*
import helpers.attractors.RequestAttractors
import io.ktor.http.Parameters
import kotlinx.html.*
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.RecordedInteractions
import tapeItems.BlankTape

object TapeEditor : EditorModule() {
    fun HTML.getAllTapesPage() {
        body {
            setupStyle()
            BreadcrumbNav()

            getForm(action = TapeRouting.RoutePaths.EDIT.path) {
                hiddenInput(name = "tape")
                inputButton {
                    style = """
                        width: 20em;
                        height: 4em;
                    """.trimIndent()
                    +"Create new tape"
                }
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
                        th(classes = "center") {
                            a {
                                href = "edit?tape=${t.name}"
                                +t.name
                            }
                        }

                        td {
                            if (t.file?.exists().isTrue()) {
                                p { +"File path: ${t.file?.path}" }
                                p { +"File size: ${t.file?.fileSize()}" }
                            } else {
                                postForm(
                                    action = TapeRouting.RoutePaths.ACTION.path,
                                    encType = FormEncType.multipartFormData
                                ) {
                                    hiddenInput(name = "tape") { value = t.name }

                                    inputButton(name = "Action") {
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
                                action = TapeRouting.RoutePaths.ACTION.path,
                                encType = FormEncType.multipartFormData
                            ) {
                                hiddenInput(name = "tape") { value = t.name }
                                p {
                                    inputButton(name = "Action") {
                                        value = "Edit"
                                        +value
                                    }
                                }
                                p {
                                    inputButton(name = "Action") {
                                        onClick = """
                                            if (confirm(${R.getProperty("viewTapeRemoveInfo")}))
                                                submit();
                                            else
                                                type="button";
                                        """.trimIndent()
                                        value = "Remove"
                                        +value
                                    }
                                }

                                if (t.file?.exists() == true)
                                    p {
                                        inputButton(name = "Action") {
                                            onClick = """
                                                if (confirm(${R.getProperty("deleteInfo").format("tape")}))
                                                    submit();
                                                else
                                                    type = "button";
                                            """.trimIndent()
                                            value = "Delete"
                                            +value
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
     * Create and Edit page for a single tape
     */
    fun HTML.getTapePage(params: Parameters) {
        val pData = params.toActiveEdit

        val randomVal = randomHost.value
        val randomValStr = randomHost.valueAsChars
        val currentPath = VCRConfig.getConfig.tapeRoot.get().path

        val folders = mutableListOf(subDirectoryDefault)
            .apply { addAll(VCRConfig.getConfig.tapeRoot.get().getFolders()) }

        head {
            script {
                unsafe {
                    +"""
                    function setRoutingUrlStates(url) {
                        parsedUrl.innerText = preVerifyURL(url);
                        var isDisabled = parsedUrl.innerText.startsWith("{");

                        allowPassthrough.disabled = isDisabled;
                        var disableChapters = !isDisabled && allowPassthrough.checked

                        SaveNewCalls.disabled = disableChapters;
                        SaveAddChapters.disabled = disableChapters;
                    }
                    """.trimIndent().appendLines(JS.all)
                }
            }
        }

        body {
            setupStyle()

            BreadcrumbNav(pData)

            if (pData.loadTape_Failed)
                p { +"No tape with the name \"${pData.expectedTapeName}\" was found." }

            h1 {
                +(if (pData.newTape) "New %s" else "%s Editor").format("Tape")
            }

            form(encType = FormEncType.multipartFormData) {
                hiddenInput(name = "afterAction") { id = name }

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
                                        if (value.trim().length > 0)
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
                                        if (selectedIndex == 0)
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

                            infoText("Where the tape will be saved to")
                        }
                    }

                    tr {
                        th { +"Name" }
                        td {
                            val tapeNameAction = if (pData.expectedTapeName != null)
                                "nameReset.hidden = tapeName.value == tapeName.placeholder;"
                            else ""

                            div {
                                textInput(name = "tapeName") {
                                    disableEnterKey
                                    id = name
                                    placeholder = pData.hardTapeName(randomVal.toString())
                                    value = placeholder
                                    onKeyUp = tapeNameAction
                                }

                                text(" ")
                                if (pData.newTape) {
                                    inputButton(type = ButtonType.button) {
                                        onClick =
                                            "tapeName.value = $randomVal;$tapeNameAction"
                                        +"Use generated number"
                                    }

                                    inputButton(type = ButtonType.button) {
                                        onClick =
                                            "tapeName.value = \"$randomValStr\";$tapeNameAction"
                                        +"Use generated char string"
                                    }
                                }

                                if (pData.expectedTapeName != null)
                                    inputButton(type = ButtonType.button) {
                                        id = "nameReset"
                                        hidden = true
                                        onClick = """
                                            tapeName.value = tapeName.placeholder;
                                            nameReset.hidden = true;
                                        """.trimIndent()
                                        +"Reset"
                                    }
                            }

                            pData.tape?.file?.also { file ->
                                if (file.exists()) {
                                    br()
                                    text("Tape located at: ")
                                    i { text(file.toString()) }
                                }
                            }

                            infoText("Tape name. Example: 'General' becomes '/General.json'") {
                                it.hidden = true
                            }

                            pData.tape?.also {
                                br()
                                +"Size: %s".format(
                                    Gson().toJsonTree(it).fileSize()
                                )
                            }
                        }
                    }

                    tr {
                        th { +"Routing URL" }
                        td {
                            textInput(name = "RoutingUrl") {
                                disableEnterKey
                                id = name
                                if (pData.newTape) {
                                    placeholder = "Example: http://google.com"
                                    value = ""
                                } else {
                                    pData.tape?.also {
                                        placeholder = it.routingUrl.orEmpty()
                                        value = it.routingUrl.orEmpty()
                                    }
                                }
                                size = "${placeholder.length + 20}"

                                onLoad = "setRoutingUrlStates(value)"
                                onKeyUp = "setRoutingUrlStates(value)"
                            }

                            if (!pData.newTape) {
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
                            div {
                                style = "margin-top: 6px"
                                text("Parsed url: ")
                                i {
                                    a {
                                        id = "parsedUrl"
                                        +when (val url = pData.tape?.routingUrl?.trim()) {
                                            null -> "{ empty }"
                                            "" -> "" // isBlank()
                                            else -> url
                                        }
                                    }
                                }
                            }

                            br()
                            infoText("tapeRoutingUrlInfo")
                        }
                    }

                    tr {
                        th { +"Request Attractors" }
                        td {
                            makeToggleButton("reqView")

                            div {
                                id = "reqView"
                                br()

                                infoText("attrInfo", arrayOf("tape"))
                                table {
                                    tr {
                                        th {
                                            style = "width: 20%" // minimum-ish size + some padding
                                            +"Path"
                                        }
                                        td {
                                            textInput(name = "filterPath") {
                                                disableEnterKey
                                                val path =
                                                    pData.tape?.attractors?.routingPath?.value.orEmpty()

                                                placeholder = if (path.isBlank())
                                                    "sub/path/here" else path
                                                value = path
                                            }
                                        }
                                    }

                                    pData.tape?.attractors?.also { attr ->
                                        addMatcherRow(attr.queryParamMatchers) {
                                            it.matcherName = "Parameter"
                                        }

                                        addMatcherRow(attr.queryHeaderMatchers) {
                                            it.matcherName = "Header"
                                        }

                                        addMatcherRow(attr.queryBodyMatchers) {
                                            it.matcherName = "Body"
                                            it.valueIsBody = true
                                        }
                                    }
                                }

                                br()
                                infoText("attrFlagOpt")
                                infoText("attrFlagExt")
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
                                    value = pData.tape?.alwaysLive.isTrue().toString()
                                    onChange = """
                                        setIsDisabled(SaveNewCalls, checked)
                                        setIsDisabled(ChapterData, checked)
                                        setIsDisabled(SaveAddChapters, checked)
                                    """.trimIndent()
                                }
                                infoText("tapeCallPassthroughInfo")
                                br()

                                text("Allow new recordings by filters -")
                                checkBoxInput(name = "SaveNewCalls") {
                                    id = "SaveNewCalls"
                                    disabled = true
                                    value = pData.tape?.isWritable.isTrue().toString()
                                }
                                infoText("tapeSaveNewCallsInfo")
                                infoText("Adding mocks are unaffected.")

                                if (!pData.newTape) {
                                    br()
                                    br()
                                    makeToggleButton("ChapterData")
                                }

                                div {
                                    id = "ChapterData"
                                    displayTapeChapters(pData)
                                }

                                if (pData.newTape) {
                                    br()
                                    postButton(name = "Action") {
                                        formAction = TapeRouting.RoutePaths.ACTION.path
                                        id = "SaveAddChapters"
                                        value = "SaveTape"
                                        onClick = "afterAction.value = 'newChapter';"
                                        +"Save and add Tape Chapters"
                                    }
                                }
                            }
                        }
                    }

                    tr {
                        th { +"Save Options" }
                        td {
                            div {
                                if (!pData.newTape) {
                                    hiddenInput(name = "name_pre") {
                                        value = pData.hardTapeName("")
                                    }

                                    if (pData.tape?.file?.exists().isTrue().not()) {
                                        hiddenInput(name = "tape") { value = pData.hardTapeName() }
                                        hiddenInput(name = "resumeEdit") { value = "true" }

                                        postButton(name = "Action") {
                                            formAction = TapeRouting.RoutePaths.ACTION.path
                                            value = "SaveToHardTape"
                                            +"Save tape as a hard tape"
                                        }

                                        infoText(
                                            "tapeSaveAsHard",
                                            arrayOf(currentPath)
                                        )
                                    }
                                } else {
                                    text("Save tape to file -")
                                    checkBoxInput(name = "hardtape")
                                    infoText("tapeSaveHardInfo")
                                }

                                br()
                                postButton(name = "Action") {
                                    formAction = TapeRouting.RoutePaths.ACTION.path
                                    value = "SaveTape"
                                    onClick = "submitCheck(tapeName);"
                                    +"Save"
                                }

                                br()

                                postButton(name = "Action") {
                                    formAction = TapeRouting.RoutePaths.ACTION.path
                                    value = "SaveTape"
                                    onClick = """
                                        submitCheck(tapeName);
                                        afterAction.value = 'addNew';
                                    """.trimIndent()
                                    +"Save and Add Another"
                                }
                                br()

                                postButton(name = "Action") {
                                    formAction = TapeRouting.RoutePaths.ACTION.path
                                    id = "SaveViewAllTapes"
                                    value = "SaveTape"
                                    onClick = """
                                        submitCheck(tapeName);
                                        afterAction.value = 'allTapes';
                                    """.trimIndent()
                                    +"Save and goto View Tapes"
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

    private fun FlowContent.displayTapeChapters(data: ActiveData) {
        val tape = data.tape ?: return
        br()
        hiddenInput(name = "tape") { value = tape.name }
        postForm(action = TapeRouting.RoutePaths.EDIT.path) {
            hiddenInput(name = "chapter")
            button { +"Create new chapter" }
        }

        if (tape.chapters.isEmpty())
            return

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
                    addChapterRow(data, mock)
                }
            }
        }

        infoText("Uses: Setting the value as '-1' will make it non-limited")
    }

    private fun TBODY.addChapterRow(data: ActiveData, chap: RecordedInteractions) {
        val tape = data.hardTapeName()
        tr {
            td {
                a {
                    href = data.hrefEdit(hChapter = chap.name)
                    +chap.name
                }
            }

            td { +(chap.alwaysLive ?: false).toString() }

            td {
                chap.attractors?.also { attr ->
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
                    id = chap.name + "_usesEnabled"
                    checked = MockUseStates.isEnabled(chap.mockUses)
                }
                br()

                numberInput {
                    id = chap.name + "_usesValue"
                    min = MockUseStates.ALWAYS.state.toString()
                    max = Int.MAX_VALUE.toString()
                    value = (if (chap.mockUses == MockUseStates.DISABLE.state)
                        MockUseStates.ALWAYS.state else chap.mockUses).toString()
                }
            }

            td {
                form {
                    hiddenInput(name = "tape") { value = tape }
                    hiddenInput(name = "chapter") { value = chap.name }

                    getButton {
                        formAction = TapeRouting.RoutePaths.EDIT.path
                        +"Edit"
                    }

                    br()
                    br()

                    getButton {
                        formAction = TapeRouting.RoutePaths.DELETE.path
                        onClick = """
                            if (confirm('${R.getProperty("deleteInfo").format("chapter")}'))
                                submit();
                            else
                                type = "button";
                            """
                        +"Delete"
                    }
                }
            }
        }
    }
}
