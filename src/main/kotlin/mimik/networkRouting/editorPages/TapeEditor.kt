package mimik.networkRouting.editorPages

import R
import com.google.gson.Gson
import mimik.helpers.attractors.RequestAttractors
import io.ktor.http.Parameters
import javaUtils.io.fileSize
import javaUtils.io.foldersPaths
import kotlinUtils.*
import kotlinx.html.*
import mimik.helpers.*
import mimik.mockHelpers.MockUseStates
import mimik.mockHelpers.RecordedInteractions
import mimik.tapeItems.BaseTape
import kotlin.math.absoluteValue
import kotlin.math.max

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
                            appendStyles("width: 15%")
                            a {
                                href = "edit?tape=${t.name}"
                                +t.name
                            }
                        }

                        td {
                            if (t.file?.exists().isTrue) {
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

                            p {
                                +"Routing URL: %s".format(
                                    when {
                                        t.routingUrl == null -> "{ no routing url }"
                                        t.isValidURL -> t.httpRoutingUrl!!
                                        else -> "{ Invalid }"
                                    }
                                )
                            }

                            displayTapeAttrInfo(t.attractors)
                            br()
                            displayTapeRecInfo(t)
                        }

                        td {
                            appendStyles("width: 15%")
                            postForm(
                                action = TapeRouting.RoutePaths.ACTION.path,
                                encType = FormEncType.multipartFormData
                            ) {
                                hiddenInput(name = "tape") { value = t.name }
                                hiddenInput(name = "afterAction") {
                                    id = name
                                    disabled = true
                                }

                                p {
                                    inputButton(name = "Action") {
                                        value = "Edit"
                                        +value
                                    }
                                }
                                p {
                                    postButton(name = "Action") {
                                        value = "Clone"
                                        +"Clone"
                                    }

                                    postButton(name = "Action") {
                                        value = "Clone"
                                        onClick = "afterAction.disabled=false; afterAction.value='edit';"
                                        +"Clone & Edit"
                                    }
                                }
                                p {
                                    inputButton(name = "Action") {
                                        onClick = """
                                            if (confirm(${R.getProperty("viewTapeRemoveInfo")}))
                                                submit();
                                            else
                                                type = "button";
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
        val randomValStr = randomHost.valueAsChars()
        val root = tapeCatalog.config.tapeRoot.get()
        val currentPath = root.path

        val folders = mutableListOf(subDirectoryDefault)
            .apply { addAll(root.foldersPaths) }
        head {
            unsafeScript {
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

        body {
            setupStyle()
            BreadcrumbNav(pData)

            if (!pData.newTape) {
                val newestTime = max(
                    pData.tape?.file?.lastModified() ?: 0,
                    pData.tape?.modifiedDate?.time ?: 0
                ).toString()
                refreshWatchWindow(pData.tape?.file) {
                    listOf(
                        ("type" to "tape"),
                        ("age" to newestTime),
                        ("name" to pData.hardTapeName())
                    )
                }
            }

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

                                    folders.forEachIndexed { index, path ->
                                        option {
                                            label = path
                                            value = when (index) {
                                                0 -> ""
                                                else -> path
                                            }
                                        }
                                    }
                                }
                            }

                            infoText("Where the tape will be saved to")
                        }
                    }

                    tr {
                        th {
                            width = "20%"
                            +"Name"
                        }
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

                            infoText(
                                "Tape name. Example: 'General' becomes '/General.json'"
                            ) { hidden = true }

                            pData.tape?.also {
                                br()
                                +"Size: %s".format(
                                    Gson().toJsonTree(it).fileSize()
                                )
                            }
                        }
                    }

                    tr {
                        th {
                            +"Routing URL"
                            tooltipText("?", "tapeRoutingUrlInfo")
                        }
                        td {
                            textInput(name = "RoutingUrl") {
                                disableEnterKey
                                id = name
                                if (pData.newTape) {
                                    placeholder = R.getProperty("urlPlaceholderExample")
                                    value = ""
                                } else {
                                    pData.tape?.also {
                                        placeholder = it.routingUrl.orEmpty()
                                        value = it.routingUrl.orEmpty()
                                    }
                                }
                                size = "${placeholder.length + 20}"
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
                                            else -> if (url.isValidURL)
                                                url else "{ no match }"
                                        }
                                    }
                                }
                            }
                        }
                    }

                    tr {
                        th { +"Request Attractors" }
                        td {
                            toggleArea {
                                infoText("attrInfo", "tape")
                                table {
                                    tr {
                                        th {
                                            width = "20%" // minimum-ish size + some padding
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

                                    pData.tape?.attractors.also { attr ->
                                        addMatcherRow(attr?.queryMatchers) {
                                            it.matcherName = "Query"
                                        }

                                        addMatcherRow(attr?.headerMatchers) {
                                            it.matcherName = "Header"
                                        }

                                        addMatcherRow(attr?.bodyMatchers) {
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
                                tooltipText(
                                    "Allow call pass-through ",
                                    "tapeCallPassthroughInfo"
                                )
                                checkBoxInput(name = "allowPassthrough") {
                                    id = "allowPassthrough"
                                    disabled = true
                                    checked = pData.tape?.alwaysLive.isTrue
                                    onChange = """
                                        setIsDisabled(SaveNewCalls, checked)
                                        if (document.getElementById('ChapterData'))
                                            if (checked)
                                                ChapterData.classList.add("opacity50");
                                            else
                                                ChapterData.classList.remove("opacity50");
                                        setIsDisabled(SaveAddChapters, checked)
                                    """.trimIndent()
                                }

                                tooltipText("?", "tapeCallPassthroughHint")
                                linebreak()

                                tooltipText(
                                    "Allow new recordings by filters -",
                                    "tapeSaveNewCallsInfo"
                                )
                                checkBoxInput(name = "SaveNewCalls") {
                                    id = "SaveNewCalls"
                                    disabled = true
                                    checked = pData.tape?.isWritable.isTrue
                                }

                                if (!pData.newTape) {
                                    linebreak()
                                    toggleArea(contentId = "ChapterData") {
                                        displayTapeChapters(pData)
                                    }
                                }

                                if (pData.newTape) {
                                    linebreak()
                                    postButton(name = "Action") {
                                        formAction = TapeRouting.RoutePaths.ACTION.path
                                        id = "SaveAddChapters"
                                        value = "SaveTape"
                                        onClick = "afterAction.value = 'newChapter';"
                                        +"Save and add Tape Chapters"
                                    }
                                } else
                                    div { id = "SaveAddChapters" }

                                unsafeScript { +"setRoutingUrlStates(RoutingUrl.value)" }
                            }
                        }
                    }

                    tr {
                        th { +"Save Options" }
                        td {
                            div {
                                if (pData.newTape) {
                                    tooltipText("Save tape to file -", "tapeSaveHardInfo")
                                    checkBoxInput(name = "hardtape")
                                    linebreak()
                                } else {
                                    hiddenInput(name = "name_pre") {
                                        value = pData.hardTapeName("")
                                    }

                                    if (pData.tape?.file?.exists().isNotTrue) {
                                        hiddenInput(name = "tape") { value = pData.hardTapeName() }
                                        hiddenInput(name = "resumeEdit") { value = "true" }

                                        postButton(name = "Action") {
                                            formAction = TapeRouting.RoutePaths.ACTION.path
                                            value = "SaveToHardTape"
                                            +"Save tape as a hard tape"
                                        }

                                        infoText("tapeSaveAsHard", currentPath)
                                        linebreak()
                                    }
                                }

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
    private fun FlowContent.displayTapeRecInfo(tape: BaseTape) {
        if (tape.chapters.isEmpty()) {
            p { +"{ no chapters }" }
            return
        }

        p {
            table {
                thead {
                    tr {
                        td(classes = "center") { +"Chapters" }
                        th { +"Total" }
                        th { +"Live" }
                        th { +"Mock" }
                        th { +"Await" }
                    }
                }
                tbody {
                    tr {
                        th { +"Enabled" }
                        td { text(tape.chapters.count { MockUseStates.isEnabled(it.mockUses) }) }
                        td {
                            +tape.chapters.count {
                                MockUseStates.isEnabled(it.mockUses) &&
                                    it.alwaysLive.orFalse
                            }.toString()
                        }
                        td {
                            +tape.chapters.count {
                                MockUseStates.isEnabled(it.mockUses) &&
                                    !it.awaitResponse
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
                        td { text(tape.chapters.count { MockUseStates.isDisabled(it.mockUses) }) }
                        td {
                            +tape.chapters.count {
                                MockUseStates.isDisabled(it.mockUses) &&
                                    it.alwaysLive.orFalse
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
                    td { +attr.queryMatchers?.count().toString() }
                    td { +attr.headerMatchers?.count().toString() }
                }
            }
        }
    }

    private fun FlowContent.displayTapeChapters(data: ActiveData) {
        val tape = data.tape ?: return
        hiddenInput(name = "tape") { value = tape.name }
        postForm(action = TapeRouting.RoutePaths.EDIT.path) {
            hiddenInput(name = "chapter") {
                id = name
                disabled = true
            }
            button {
                onClick = "chapter.disabled = false;"
                +"Create new chapter"
            }
        }

        if (tape.chapters.isEmpty())
            return
        linebreak()

        unsafeScript {
            +"""
                function setupHoverStat(row, hvItem, hvContent){
                    hvContent.style.height = row.cells[0].clientHeight + 'px';

                    hvItem.onmouseenter = function() {
                        hvContent.style.left = row.cells[0].getBoundingClientRect().right + 'px';
                        hvContent.style.display = "unset";
                        setTimeout(function() {
                            hvContent.style.opacity = 1.0;
                        }, 10);
                    }
                    hvItem.onmouseleave = function() {
                        hvContent.style.opacity = 0;
                        hvContent.style.display = "none";
                    }
                }
            """.trimIndent()
        }
        table {
            thead {
                tr {
                    th {
                        width = "24%"
                        +"Name"
                    }
                    th { +"Mock type" }
                    th { +"Attractors" }
                    th { +"Uses" }
                    th {
                        colSpan = "2"
                        +""
                    }
                }
            }
            tbody {
                tape.chapters.forEach { mock ->
                    addChapterRow(data, mock)
                }
            }
        }
    }

    private fun TBODY.addChapterRow(data: ActiveData, chap: RecordedInteractions) {
        val tape = data.hardTapeName()
        val chapID = chap.hashCode().absoluteValue

        val chapRow = "chapRow_$chapID"
        val namePreview = "namePrev_$chapID"
        val itemPrev = "itemPrev_$chapID"

        tr {
            id = chapRow
            td {
                a {
                    id = namePreview
                    href = data.hrefEdit(hChapter = chap.name)
                    +chap.name
                }
            }

            td {
                +when {
                    chap.alwaysLive.orFalse -> "Live"
                    chap.awaitResponse -> "Await"
                    else -> "Recorded"
                }
            }

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
                                    +(attr.queryMatchers
                                        ?.count { it.value != null }).toString()
                                }
                                td {
                                    +(attr.headerMatchers
                                        ?.count { it.value != null }).toString()
                                }
                                td {
                                    +(attr.bodyMatchers
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

                val usesTypes = chap.name + "_usesType"
                numberInput {
                    id = chap.name + "_usesValue"
                    min = MockUseStates.ALWAYS.state.toString()
                    max = Int.MAX_VALUE.toString()
                    value = (if (chap.mockUses == MockUseStates.DISABLE.state)
                        MockUseStates.ALWAYS.state else chap.mockUses).toString()
                    onChange = """
                        if (value == -1)
                            $usesTypes.innerText = "'Always on'";
                            else $usesTypes.innerText = "'Limited'";
                    """.trimIndent()
                }
                br()
                a {
                    id = usesTypes
                    +"'%s'".format(
                        if (chap.mockUses == -1)
                            "Always on" else "Limited"
                    )
                }
            }

            td {
                form(encType = FormEncType.multipartFormData) {
                    hiddenInput(name = "tape") { value = tape }
                    hiddenInput(name = "chapter") { value = chap.name }
                    hiddenInput(name = "afterAction") {
                        id = name
                        disabled = true
                    }

                    getButton {
                        formAction = TapeRouting.RoutePaths.EDIT.path
                        +"Edit"
                    }

                    linebreak()
                    postButton(name = "Action") {
                        formAction = TapeRouting.RoutePaths.ACTION.path
                        value = "Clone"
                        +"Clone"
                    }

                    postButton(name = "Action") {
                        formAction = TapeRouting.RoutePaths.ACTION.path
                        value = "Clone"
                        onClick = "afterAction.disabled=false; afterAction.value='edit';"
                        +"Clone & Edit"
                    }

                    linebreak()

                    getButton {
                        formAction = TapeRouting.RoutePaths.DELETE.path
                        onClick = """
                            if (confirm(${R.getProperty("deleteInfo").format("chapter")}))
                                submit();
                            else
                                type = "button";
                            """
                        +"Delete"
                    }
                }
            }

            td {
                style = "display: contents;"
                div {
                    id = itemPrev
                    style = """
                            position: absolute;
                            overflow-y: hidden;
                            transition: opacity 0.2s;
                            right: 28px;
                            background: aliceblue;
                            opacity: 0;
                            display: none
                        """.trimIndent()
                    table {
                        style = "border-width: 3px;"
                        tr {
                            th {
                                width = "13%"
                                +"URL"
                            }
                            td { +(chap.requestData?.url ?: noData) }
                        }
                        tr {
                            th { +"Response" }
                            td {
                                +when {
                                    chap.alwaysLive.isTrue -> "Live"
                                    chap.awaitResponse -> "Await"
                                    else -> (chap.responseData?.code ?: 0).toString()
                                }
                            }
                        }
                        tr {
                            th { +"_" }
                            td { +"_" }
                        }
                    }
                }
                unsafeScript {
                    +"setupHoverStat($chapRow, $namePreview, $itemPrev);"
                }
//                script {
//                    unsafe {
//                        +"""
//                            $itemPrev.style.height = $chapRow.cells[0].clientHeight + 'px';
//
//                            $namePreview.onmouseenter = function() {
//                                $itemPrev.style.left = $chapRow.cells[0].getBoundingClientRect().right + 'px';
//                                $itemPrev.style.opacity = 1.0;
//                            }
//                            $namePreview.onmouseleave = function() {
//                                $itemPrev.style.opacity = 0;
//                            }
//                            """.trimIndent()
//                    }
//                }
            }
        }
    }
}
