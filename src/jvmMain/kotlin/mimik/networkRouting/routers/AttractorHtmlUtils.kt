package mimik.networkRouting.routers

import R
import kotlinx.appendLines
import kotlinx.isNotTrue
import kotlinx.isTrue
import kotlinx.orFalse
import kotlinx.html.*
import mimik.helpers.attractors.RequestAttractorBit
import mimik.networkRouting.routers.JsUtils.disableEnterKey
import java.util.*

data class TableQueryMatcher(
    /**
     * What name of matcher this is representing.
     * ex: Parameter, Header, or Body
     */
    var matcherName: String = "",
    /**
     * Uses a expandable field instead of a single line
     */
    var valueIsBody: Boolean = false
) {
    val nameShort
        get() = matcherName.take(2).uppercase()

    private val filterKey = "filter"
    private val filterPrefix
        get() = "$filterKey$nameShort"

    val tableId
        get() = "${filterPrefix}_Table"
    val rowID
        get() = "${filterPrefix}_ID"
    val rowValueName
        get() = "${filterPrefix}_Value"
    val rowOptName
        get() = "${filterPrefix}_Opt"
    val rowExceptName
        get() = "${filterPrefix}_Except"
    val allowAny_ID
        get() = "${filterPrefix}_allowAny"

    var allowAnyCheckState: Boolean? = null
}

object AttractorHtmlUtils {
    /**
     * Adds a row based on the input [tableInfo].
     * Attractor info from [bit] is pre-pended if able to.
     */
    fun TABLE.addMatcherRow(
        bit: List<RequestAttractorBit>?,
        tableInfo: (TableQueryMatcher) -> Unit
    ) {
        val info = TableQueryMatcher().also(tableInfo)

        tr {
            th { +info.matcherName }
            td {
                toggleArea {
                    addMatcherRowData(bit, info)
                }
            }
        }
    }

    /**
     * Adds a table to this row which allows editing of the [bit] data
     * If [bit] has data, then it is also added
     */
    fun FlowContent.addMatcherRowData(
        bit: List<RequestAttractorBit>?,
        info: TableQueryMatcher
    ) {
        val isAllowAny = bit?.any { it.allowAllInputs.isTrue }.orFalse || info.allowAnyCheckState.orFalse

        tooltipText("Allow any input: ", "allowAnyInputInput")
        checkBoxInput(name = info.allowAny_ID) {
            checked = isAllowAny
            onChange = """
                if (this.checked) 
                    ${info.tableId}.style.color = "#A0A0A0";
                else
                    ${info.tableId}.style.color = "";
            """.trimIndent()
        }
        linebreak()

        table {
            id = info.tableId
            if (isAllowAny)
                appendStyles("color: #A0A0A0")

            unsafeScript {
                +"""
                    var ${info.rowID} = 0;
                    function addNew${info.nameShort}Filter(expandableField) {
                        var newrow = ${info.tableId}.insertRow(${info.tableId}.rows.length-1);

                        var filterValue = newrow.insertCell(0);
                        var valueInput = createTextInput("${info.rowValueName}", ${info.rowID}, ${info.valueIsBody});
                        formatParentFieldWidth(valueInput);
                        filterValue.append(valueInput);

                        var filterFlags = newrow.insertCell(1);
                        var isOptionalInput = createCheckbox("${info.rowOptName}", ${info.rowID});
                        filterFlags.append(isOptionalInput);
                        filterFlags.append(" Optional");
                        filterFlags.append(document.createElement("br"));

                        var isExceptInput = createCheckbox("${info.rowExceptName}", ${info.rowID});
                        filterFlags.append(isExceptInput);
                        filterFlags.append(" Except");
                        ${info.rowID}++;

                        var actionBtns = newrow.insertCell(2);
                        actionBtns.append(createDeleteBtn(newrow));

                        if (expandableField) {
                            actionBtns.append(document.createElement("br"));
                            actionBtns.append(document.createElement("br"));
                            var formatBtn = createBtn("Beautify Body");
                            formatBtn.onclick = function() { beautifyField(valueInput) };
                            actionBtns.append(formatBtn);
                        }
                    }
                    """.trimIndent()
                    .appendLines(
                        JsUtils.Functions.FormatParentFieldWidth_func.value
                    )
            }

            thead {
                tr {
                    th { +"Value" }
                    th { +"Flags" }
                    th { +"Actions" }
                }
            }
            tbody {
                bit?.filter { it.allowAllInputs.isNotTrue }
                    ?.forEachIndexed { index, bit ->
                        appendBit(bit, index, info)
                    }

                tr {
                    td {
                        colSpan = "3"
                        button(type = ButtonType.button) {
                            onClick = "addNew${info.nameShort}Filter(${info.valueIsBody});"
                            +"Add new Filter"
                        }
                    }
                }
            }
        }
    }

    fun TBODY.appendBit(bit: RequestAttractorBit, count: Int, info: TableQueryMatcher) {
        tr {
            id = bit.hashCode().toString()
            val fieldName = "${info.rowValueName}${R["loadFlag", ""]}$count"

            td {
                if (info.valueIsBody)
                    textArea {
                        disableEnterKey
                        name = fieldName
                        id = name
                        placeholder = bit.hardValue
                        +placeholder
                    }
                else
                    textInput(name = fieldName) {
                        disableEnterKey
                        id = name
                        placeholder = bit.hardValue
                        value = placeholder
                    }

                unsafeScript {
                    +"formatParentFieldWidth($fieldName);".let {
                        if (info.valueIsBody)
                            it + "beautifyField($fieldName);" else it
                    }
                }
                // todo; reset value button
            }

            td {
                checkBoxInput(name = "${info.rowOptName}${R["loadFlag", ""]}$count") {
                    checked = bit.optional.orFalse
                }
                text(" Optional")
                br()

                checkBoxInput(name = "${info.rowExceptName}${R["loadFlag", ""]}$count") {
                    checked = bit.except.orFalse
                }
                text(" Except")
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

                if (info.valueIsBody) {
                    linebreak()
                    button(type = ButtonType.button) {
                        onClick = "beautifyField($fieldName);"
                        +"Beautify Body"
                    }
                }
            }
        }
    }
}
