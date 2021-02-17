@file:Suppress("ClassName", "KDocUnresolvedReference", "SpellCheckingInspection", "PropertyName", "FunctionName")

package networkRouting

import R
import helpers.*
import helpers.attractors.RequestAttractorBit
import helpers.attractors.RequestAttractors
import helpers.lzma.LZMA_Decode
import helpers.parser.P4Command
import helpers.parser.Parser_v4
import kotlinx.html.*
import mimikMockHelpers.*
import networkRouting.JsUtils.disableEnterKey
import okhttp3.Headers
import tapeItems.BaseTape
import java.util.Date

abstract class EditorModule_b {
    val tapeCatalog get() = MimikContainer.tapeCatalog

    companion object {
        val randomHost = RandomHost()
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
