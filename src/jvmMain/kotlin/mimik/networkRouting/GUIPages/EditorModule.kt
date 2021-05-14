@file:Suppress("ClassName", "KDocUnresolvedReference", "SpellCheckingInspection", "PropertyName", "FunctionName")

package mimik.networkRouting.GUIPages

import R
import mimik.tapeItems.MimikContainer
import io.ktor.http.*
import kotlinx.*
import kotlinx.collections.toArrayList
import kotlinx.html.*
import mimik.helpers.*
import mimik.helpers.attractors.RequestAttractorBit
import mimik.helpers.attractors.RequestAttractors
import mimik.helpers.lzma.LZMA_Decode
import mimik.helpers.parser.P4Command
import mimik.helpers.parser.Parser_v4
import mimik.mockHelpers.*
import mimik.tapeItems.BaseTape
import mimik.networkRouting.routers.JsUtils.disableEnterKey
import mimik.networkRouting.routers.TableQueryMatcher
import mimik.networkRouting.routers.editorPages.ActiveData
import okhttp3.Headers
import okhttp3.NetworkData
import okhttp3.RequestData
import okhttp3.ResponseData
import java.util.*

/**
 * Abstract class which editing pages are built from.
 *
 * Contains helper functions/ properties for an editor page
 */
abstract class EditorModule {
    val tapeCatalog get() = MimikContainer.tapeCatalog

    companion object {
        val randomHost = RandomHost()
    }

    enum class Libs_CSS(private val init: String) {
        Sortable(
            """
                .sjs_ghost {
                  opacity: .5;
                  background: #C8EBFB;
                }

                .sjs_group {
                  padding-left: 0;
                  margin-bottom: 0;
                }

                .sjs_group-item {
                  position: relative;
                  display: block;
                  padding: .75rem 0.5rem;
                  margin-bottom: -1px;
                  background-color: #fff;
                  border: 1px solid rgba(0, 0, 0, .125);
                }

                .nested-sortable,
                .nested-1,
                .nested-2,
                .nested-3 {
                  margin-top: 4px;
                  margin-bottom: 4px;
                }

                .nested-1 {
                  background-color: #e6e6e6;
                }

                .nested-2 {
                  background-color: #cccccc;
                }

                .nested-3 {
                  background-color: #b3b3b3;
                }

                .sjs_col {
                  flex-basis: 0;
                  flex-grow: 1;
                  max-width: 100%;
                }

                .sjs_row {
                  display: -ms-flexbox;
                  display: flex;
                  flex-wrap: wrap;
                  margin-right: -15px;
                  margin-left: -15px;
                }

                .sjs_handle {
                  display: inline;
                  cursor: row-resize;
                }

                .sjs_noDrag {}

                .inline {
                  display: inline;
                }
            """
        );

        val value: String
            get() = init.trimIndent()
    }

    /**
     * Retrieves data from the [Parameters] for the current Tape/ Chapter
     */
    val Parameters.toActiveEdit
        get() = ActiveData(this)

    fun BODY.BreadcrumbNav(params: Parameters = Parameters.Empty) =
        BreadcrumbNav(params.toActiveEdit)

    fun BODY.BreadcrumbNav(data: ActiveData) {
        div(classes = "breadcrumb") {
            div(classes = "subnav") {
                a(classes = "navHeader") {
                    href = data.hrefMake()
                    +"All Tapes"
                }
                if (tapeCatalog.tapes.isNotEmpty())
                    div(classes = "subnav-content") {
                        style = "left: 1em;"
                        tapeCatalog.tapes.forEach {
                            a {
                                href = data.hrefMake(tape = it.name)
                                +it.name
                            }
                        }
                    }
            }

            if (!data.newTape) {
                div(classes = "subnav") {
                    a(classes = "navHeader") {
                        href = data.hrefMake(tape = data.hardTapeName())
                        +"Tape (%s)".format(data.hardTapeName())
                    }

                    data.tape?.chapters?.also { chap ->
                        if (chap.isEmpty()) return@also
                        div(classes = "subnav-content") {
                            style = "left: 5.7em;"
                            chap.forEach {
                                a {
                                    href = data.hrefMake(
                                        tape = data.hardTapeName(),
                                        chapter = it.name
                                    )
                                    +it.name
                                }
                            }
                        }
                    }
                }
            }

            data.chapter?.also { ch ->
                div(classes = "subnav") {
                    a(classes = "navHeader") {
                        href = data.hrefMake(
                            tape = data.hardTapeName(),
                            chapter = data.hardChapName()
                        )
                        +ch.name
                    }
                }

                if (data.expectedNetworkType.isNotBlank())
                    div(classes = "subnav") {
                        a(classes = "navHeader") {
                            +data.expectedNetworkType
                        }
                    }
            }

            // Append a `down caret` to headers which have children
            unsafeScript {
                +"""
                    Array.from(document.getElementsByClassName('navHeader')).forEach(
                        function(item) {
                            var next = item.nextElementSibling;
                            if (next != null && next.classList.contains('subnav-content'))
                                item.classList.add('caret-down');
                        }
                    );
                """.trimIndent()
            }
        }
        br()
    }

    /**
     * Displays the input [data], or displays "{ no data }" if it's null
     */
    fun FlowContent.displayInteractionData(data: NetworkData?) {
        if (data == null) {
            +R["noData", ""]
            return
        }

        div {
            id = when (data) {
                is RequestData -> "requestDiv"
                is ResponseData -> "responseDiv"
                else -> ""
            }
            table {
                thead {
                    tr {
                        th {
                            resizableCol
                            appendStyles("width: 15%")
                            +"Specific"
                        }
                        th {
                            resizableCol
                            appendStyles("width: 40%")
                            +"Headers"
                        }
                        th { +"Body" }
                    }
                }
                tbody {
                    wordBreak_word
                    tr {
                        tapeDataRow(data)
                    }
                }
            }
        }
    }

    private fun TR.tapeDataRow(data: NetworkData) {
        td {
            style = "vertical-align: top;"
            div {
                when (data) {
                    is RequestData -> {
                        text("Method: \n${data.method}")
                        br()
                        +"Url: "
                        a {
                            val url = data.url
                            href = url.orEmpty()
                            +when (url) {
                                null, "" -> R["noData", ""]
                                else -> url
                            }
                        }
                    }
                    is ResponseData -> {
                        text("Code: \n${data.code}")
                        infoText(
                            "Status - '%s'",
                            HttpStatusCode.fromValue(data.code ?: 200).description
                        )
                    }
                    else -> text(R["noData", ""])
                }
            }
        }

        td {
            style = "vertical-align: top;"
            val divID = "resizingTapeData_${data.hashCode()}"
            val headers = data.headers

            if (headers == null || headers.size == 0)
                +R["noData", ""]
            else {
                div {
                    id = divID
                    style = """
                        margin-bottom: 1em;
                        resize: vertical;
                        overflow: auto;
                        min-height: 5em;
                        background-color: #DDD;
                    """.trimIndent()

                    val tableID = "header_${data.hashCode()}"
                    table {
                        id = tableID
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
                            headers.toMultimap().forEach { (t, u) ->
                                u.forEach {
                                    tr {
                                        td { +t }
                                        td { +it }
                                    }
                                }
                            }
                        }
                    }

                    unsafeScript {
                        +"$divID.style.maxHeight = ($tableID.scrollHeight + 14) + 'px';"
                    }
                }
            }
        }

        td {
            style = """
                vertical-align: top;
                text-align: center;
            """.trimIndent()

            val bodyData = data.body
            if (bodyData == null) {
                +R["noData", ""]
                return@td
            }

            if (data.isImage)
                infoText("[Base64]")

            val areaID = when (data) {
                is RequestData -> "requestBody"
                is ResponseData -> "responseBody"
                else -> ""
            }

            textArea {
                id = areaID
                readonly = true
                style = """
                    margin-bottom: 1em;
                    resize: vertical;
                    width: 100%;
                    min-height: 5em;
                    background-color: #EEE;
                """.trimIndent()
                readonly = true
                +bodyData
            }

            unsafeScript { +"beautifyField($areaID);" }
        }
    }

    private fun Map<String, String>.filterToAttractors(): RequestAttractors? {
        return RequestAttractors { attr ->
            attr.routingPath = get("filterPath")?.trim()?.let { path ->
                if (path.isBlank()) null else RequestAttractorBit(path)
            }

            if (keys.any { it.startsWith(R["filterKey", ""]) }) {
                attr.queryMatchers = filterFindData("Query")
                attr.headerMatchers = filterFindData("Header")
                attr.bodyMatchers = filterFindData("Body")
            }
        }.let {
            if (it.hasData) it else null
        }
    }

    /**
     * Saves key/value data to a tape. Reusing tape is retrieved using "name_pre".
     */
    fun Map<String, String>.saveToTape(): BaseTape {
        var isNewTape = true
        val nowTape = tapeCatalog.tapes.firstOrNull { it.name == get("name_pre") }
            ?.also { isNewTape = false }

        val modTape = BaseTape.reBuild(nowTape) { tape ->
            // subDirectory = get("Directory")?.trim()
            val hostValue = get("hostValue")?.toIntOrNull() ?: RandomHost().value
            tape.tapeName = get("tapeName")?.trim() ?: hostValue.toString()
            tape.routingURL = get("RoutingUrl")?.trim()
            if (tape.routingURL.isNullOrBlank())
                tape.routingURL = null

            tape.attractors = filterToAttractors()

            tape.alwaysLive = if (tape.isValidURL && get("allowPassthrough") == "on") true else null
            tape.allowNewRecordings = if (get("SaveNewCalls") == "on") true else null
        }
        modTape.modifiedDate = Date()

        if (isNewTape) {
            tapeCatalog.tapes.add(modTape)
            if (get("hardtape") == "on") modTape.saveFile()
        } else
            modTape.saveIfExists()

        return modTape
    }

    private fun parseSeqData(inputData: String?): List<SeqActionObject> {
        if (inputData.isNullOrEmpty()) return listOf()

        data class itemData(val ID: String, val Data: String, val ZData: String? = null)

        data class seqDataClass(
            val hostOrder: Map<String, Int>,
            val hosts: Map<String, Map<String, Int>>,
            val items: ArrayList<itemData>
        )

        val parseData = inputData.fromJson<seqDataClass>() ?: return listOf()

        // empty ZData = no change
        // "Missing sub-source" + ZData = save new data
        // Commands[ID].not contain newCommand.ID = remove Commands[ID]
        val objItems = parseData.items.map {
            Pair(
                it.ID.toInt(),
                it.Data.fromBase64.let { data ->
                    if (data.startsWith("Invalid") || data.startsWith("Missing"))
                        LZMA_Decode(it.ZData.orEmpty()).fromJson<P4Command?>()?.also { cmd ->
                            cmd.source_name = cmd.source_name?.fromBase64
                            cmd.source_match = cmd.source_match?.fromBase64
                            cmd.act_match = cmd.act_match?.fromBase64
                            cmd.act_name = cmd.act_name?.fromBase64
                        }
                    else
                        Parser_v4.parseToCommand(data)
                }
            )
        }.filterNot { it.second.isNull() }.map { it.first to it.second!! }.toMap()

        val hosts = parseData.hosts
            .mapKeys { it.key.toInt() }
            .mapValues { (hostID, ItemIDs) ->
                SeqActionObject { newObj ->
                    newObj.ID = hostID
                    newObj.Commands = ItemIDs.map { (id, idx) -> idx to objItems[id.toInt()] }
                        .sortedBy { it.first }.mapNotNull { it.second }.toArrayList()
                }
            }

        return parseData.hostOrder.map { (id, idx) -> idx to id.toInt() }
            .sortedBy { it.first }.mapNotNull { hosts[it.second] }
    }

    fun Map<String, String>.saveChapter(tape: BaseTape): RecordedInteractions {
        val modChap = tape.chapters
            .firstOrNull { it.name == get("name_pre") }
            ?: let { tape.createNewInteraction() }

        modChap.also { chap ->
            chap.chapterName = get("nameChap")
            chap.attractors = filterToAttractors()
            chap.cachedCalls.clear()
            chap.seqActions = parseSeqData(get("seqData")).toArrayList()

            chap.mockUses = get("usesCount")?.toIntOrNull() ?: MockUseStates.ALWAYS.state
            if (get("usesEnabled") != "on")
                chap.mockUses = MockUseStates.asDisabled(chap.mockUses)

            chap.alwaysLive = if (get("useLive") == "on") true else null

            if (get("clearRequest") == "on")
                chap.requestData = null

            if (get("responseAwait") == "on")
                chap.responseData = null

            chap.modifiedDate = Date()
        }

        tape.saveIfExists()

        return modChap
    }

    private fun Map<String, String>.filterFindData(queryName: String): List<RequestAttractorBit>? {
        val mKey = TableQueryMatcher(queryName)

        val values = asSequence()
            .filter { it.value.isNotBlank() }
            .filter { it.key.startsWith(mKey.rowValueName) }
            .associateBy(
                {
                    val keyRange = it.key.lastIndexRange { "(${R["loadFlag", ""]})?\\d+" }
                    it.key.substring(keyRange, "")
                },
                { it.value }
            ).filter { it.key.isNotEmpty() }

        val optionals = asSequence()
            .filter { it.key.startsWith(mKey.rowOptName) }
            .associateBy(
                { it.key.removePrefix(mKey.rowOptName) },
                { it.value }
            )

        val excepts = asSequence()
            .filter { it.key.startsWith(mKey.rowExceptName) }
            .associateBy(
                { it.key.removePrefix(mKey.rowExceptName) },
                { it.value }
            )

        val results = values.keys.map { key ->
            RequestAttractorBit {
                it.value = values.getValue(key)
                it.optional = if (optionals.getOrDefault(key, "") == "on") true else null
                it.except = if (excepts.getOrDefault(key, "") == "on") true else null

                val allowAll = allTrue(
                    it.value == ".*",
                    it.optional.isNotTrue,
                    it.except.isNotTrue
                )

                if (allowAll) {
                    it.clearState()
                    it.allowAllInputs = true
                }
            }
        }
        if (get(mKey.allowAny_ID) == "on") {
            return results.toMutableList().apply {
                add(0, RequestAttractorBit { it.allowAllInputs = true })
            }
        }

        return if (results.isEmpty()) null else results
    }

    fun FlowContent.addHeaderTable(headers: Headers?) {
        table {
            id = "headerTable"

            unsafeScript {
                +"""
                    var headerID = 0;
                    function addNewHeaderRow() {
                        var newrow = headerTable.insertRow(headerTable.rows.length-1);

                        var keyCol = newrow.insertCell(0);
                        var keyInput = createTextInput("header_key_", headerID);
                        keyCol.append(keyInput);
                        
                        var valCol = newrow.insertCell(1);
                        var valueInput = createTextInput("header_value_", headerID);
                        valCol.append(valueInput);
                        headerID++;
                        
                        var actionBtns = newrow.insertCell(2);
                        actionBtns.append(createDeleteBtn(newrow));
                        actionBtns.append(" ");
                        var cloneBtn = createBtn("Clone");
                        cloneBtn.disabled = true;
                        actionBtns.append(cloneBtn);
                    }
                    """.trimIndent()
            }

            thead {
                tr {
                    th { +"Key" }
                    th { +"Value" }
                    th { +"Actions" }
                }
            }
            tbody {
                var headInc = 0
                headers?.toMultimap()?.forEach { (t, u) ->
                    u.forEach {
                        appendheader(t to it, headInc)
                        headInc++
                    }
                }

                tr {
                    td {
                        colSpan = "3"
                        button(type = ButtonType.button) {
                            onClick = "addNewHeaderRow();"
                            +"Add new Header"
                        }
                    }
                }
            }
        }
    }

    fun TBODY.appendheader(head: Pair<String, String>, idStep: Int) {
        tr {
            td {
                textInput(name = "header_key_load_$idStep") {
                    disableEnterKey
                    placeholder = head.first
                    value = placeholder
                }
            }

            td {
                textInput(name = "header_value_load_$idStep") {
                    disableEnterKey
                    placeholder = head.second
                    value = placeholder
                }
            }

            td {
                button {
                    onClick = "this.parentNode.parentNode.remove();"
                    +"Delete"
                }

                text(" ")

                button {
                    disabled = true
                    +"Clone"
                }
            }
        }
    }
}
