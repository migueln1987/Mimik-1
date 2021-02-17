package networkRouting.editorPages

import R
import helpers.parser.P4Command
import kotlinx.html.*
import kotlin.math.absoluteValue

class seqContentIDs(val data: P4Command) {
    val dataID: Int
        get() = data.hashCode().absoluteValue

    /**
     * "Final" string of the sequence
     */
    val titleView: String
        get() = "viewTx_$dataID"

    /**
     * "Editing" string of the sequence
     */
    val titleEdit: String
        get() = "editTx_$dataID"

    val editCancelBtn: String
        get() = "editCancelBtn_$dataID"
    val saveBtn: String
        get() = "saveBtn_$dataID"
    val deleteBtn: String
        get() = "deleteBtn_$dataID"

    /**
     * Editing sequence controls view
     */
    val editingView: String
        get() = "editContent_$dataID"
}

fun DIV.kotlinUIEditor(testGroups: ArrayList<ArrayList<P4Command>>) {

//        seqGroups.forEach { seqList ->
    testGroups.forEach { seqList ->
        val groupID = seqList.hashCode().absoluteValue
        val listLevelID = "Level1_$groupID"
        val toggleBtn = "toggle_$groupID"
        unsafeScript {
            +seqList.map {
                val rootId = it.hashCode().absoluteValue
                """
                        data['$rootId'] = {
                            parent: '$groupID',
                            newParent: -1,
                            newIndex: -1,
                            data: new p4Parser(${it.asJSObject})
                        };
                        """.trimIndent()
            }
                .fold(StringBuilder()) { a, b -> a.appendLine(b) }
                .toString()
        }

        table(classes = "sjs_group-item nested-1") {
            border { raw = "0" }
            display = DisplayFlags.table

            thead {
                tr {
                    visibility = VisibilityFlags.collapse
                    th { width = "1em" }
                    th()
                }
            }

            tr {
                SequenceHeader(toggleBtn, groupID)
            }

            tr {
                td {
                    colSpan = "2"
                    padding { all = "0" }
                    background { color = "inherit" }

                    div {
                        id = "test_$listLevelID"
                        display = DisplayFlags.none
                        padding { all = "0" }
                        background { color = "inherit" }

                        div(classes = "sjs_noDrag nested-2") {
                            id = listLevelID
                            attributes["idCode"] = "$groupID"
                            background { color = "#f4f4f4" }
                            padding { all = "0.4em" }
                            border_i3D("gray")
                            border { bottom { width = "0" } }
                            margin { bottom = "0" }

                            seqList.forEachIndexed { _, seq ->
                                SequenceBody(seq)
                            }
                        }

                        newSeqCommand()
                    }
                    unsafeScript {
                        +"enableToggleArea($toggleBtn, test_$listLevelID); enableSortLevel($listLevelID);"
//                            +"enableToggleArea($toggleBtn, $listLevelID); enableSortLevel($listLevelID);"
                    }
                }
            }
        }
    }
}

fun TR.SequenceHeader(toggleBtn: String, groupID: Int) {
    td {
        background { color = "inherit" }
        padding { all = "0" }
        div(classes = "sjs_handle") {
            padding { horizontal = "8px" }
            +":::"
        }
    }

    td {
        background { color = "inherit" }
        padding { all = "0" }
        button(type = ButtonType.button, classes = "inline collapsible") {
            id = toggleBtn
            +"Sequence $groupID"
        }
    }
}

fun DIV.SequenceBody(seq: P4Command) {
    val seqData = seqContentIDs(seq)
    val itemID = "item_${seqData.dataID}"

    div {
        id = itemID
        attributes["idCode"] = "${seqData.dataID}"
        border_3D()
        margin { vertical = "4px" }

        table(classes = "sjs_group-item nested-2") {
            padding { all = "0" }
            styleProxy("display", "table")
            styleProxy("table-layout", "fixed")

            thead {
                tr {
                    visibility = VisibilityFlags.collapse
                    th { width = "1em" }
                    th()
                    th { width = "2.5em" }
                }
            }

            tr {
                td {
                    background { color = "unset" }
                    div(classes = "sjs_handle") { +":::" }
                }

                td {
                    background { color = "unset" }
                    styleProxy("overflow", "hidden")
                    styleProxy("text-overflow", "ellipsis")

                    div {
                        id = seqData.titleView
                        +seq.toString()
                    }
                    div {
                        id = seqData.titleEdit
                        display = DisplayFlags.none
                        styleProxy("color", "white")
                        +seq.toString()
                    }
                }

                td {
                    background { color = "unset" }
                    styleProxy("position", "absolute")
                    styleProxy("right", "0")

                    button(type = ButtonType.button) {
                        id = seqData.deleteBtn
                        display = DisplayFlags.none
                        onClick = """
                            if (confirm(${R["deleteInfo", ""].format("Sequence Command")}))
                                alert("Removing element: $itemID");
                            else
                                type = "button";
                        """.trimIndent()
                        +"Delete"
                    }

                    button(type = ButtonType.button) {
                        id = seqData.saveBtn
                        display = DisplayFlags.none
                        onClick = "${seqData.titleView}.innerHTML =  ${seqData.titleEdit}.innerHTML"
                        +"Save"
                    }

                    button(type = ButtonType.button) {
                        id = seqData.editCancelBtn
                        onClick = "toggleEditButton(${seqData.dataID})"
                        +"Edit"
                    }
                }
            }
        }

        div {
            id = seqData.editingView
            display = DisplayFlags.none
            ParserEditor(seqData.dataID)
        }
    }
}

fun DIV.newSeqCommand() {
    div {
        background { color = "#f4f4f4" }
        padding { all = "0.5em" }
        border_i3D("gray")
        border { top { width = "0" } }
        button(type = ButtonType.button) { +"Add new command" }
    }
}

fun FlowContent.ParserEditor(dataID: Int) {

    val condFieldID_Off = "parser_condOff_$dataID"
    val condFieldID_On = "parser_condOn_$dataID"

    val actFieldID_Off = "parser_actOff_$dataID"
    val actFieldID_On = "parser_actOn_$dataID"

    table {
        width = "100%"
        thead {
            tr {
                th {
                    width = "20%"
                    tooltipText(
                        "Conditional",
                        "Determines pre/ post actions of a source's results"
                    )

                    checkBoxInput {
                        onClick = """
                            if (checked) {
                                $condFieldID_On.style.display = 'inline';
                                $condFieldID_Off.style.display = 'none';
                            } else {
                                $condFieldID_On.style.display = 'none';
                                $condFieldID_Off.style.display = 'inline';
                            }
                        """.trimIndent()
                    }
                }

                th {
                    tooltipText(
                        "Source",
                        "What is the context of this step"
                    )
                }

                th {
                    tooltipText(
                        "Action",
                        "Processing with the source content"
                    )

                    checkBoxInput {
                        onClick = """
                            if (checked) {
                                $actFieldID_On.style.display = 'inline';
                                $actFieldID_Off.style.display = 'none';
                            } else {
                                $actFieldID_On.style.display = 'none';
                                $actFieldID_Off.style.display = 'inline';
                            }
                        """.trimIndent()
                    }
                }
            }
        }

        tbody {
            tr {
                td {
                    div {
                        id = condFieldID_Off
                        tooltipText(
                            "Disabled",
                            "This step's end state will not affect other steps"
                        )
                    }

                    div {
                        id = condFieldID_On
                        display = DisplayFlags.none

                        group {
                            tooltipText(
                                "Optional",
                                "True; further steps will still run even if this one doesn't pass.\n" +
                                    "False: The conditional must pass for this and future lines to run."
                            )
                            checkBoxInput {
                                checked = true
                            }
                        }

                        linebreak()

                        group {
                            tooltipText(
                                "Requirement: ",
                                "What kind of state must the source have to Pass"
                            )

                            select {
                                option {
                                    selected = true
                                    +"None"
                                }
                                option { +"True" }
                                option { +"False" }
                            }
                        }
                    }
                }

                td {
                    table {
                        border { raw = "0" }
                        thead {
                            tr {
                                td {
                                    width = "45%"
                                    padding { all = "0" }
                                }
                                td { padding { all = "0" } }
                            }
                        }

                        SourceRows(dataID)
                    }
                }

                td {
                    div {
                        id = actFieldID_Off
                        tooltipText(
                            "Disabled",
                            "This step will not produce any actions"
                        )
                    }

                    div {
                        id = actFieldID_On
                        display = DisplayFlags.none

                        +"Content for 'Actions'"
                    }
                }
            }
        }
    }
}

private fun TABLE.SourceRows(dataID: Int) {
    val sourceRootType = "srcType_$dataID"
    val source_RSubCell = "srcRTypecell_$dataID"
    val source_RSub = "srcRType_$dataID"
    val source_VSubCell = "srcVTypeCell_$dataID"
    val source_VSub = "srcVType_$dataID"

    val source_iNameCell = "srcINameCell_$dataID"
    val source_iNameOff = "srcINameOff_$dataID"
    val source_iNameOn = "srcINameOn_$dataID"
    val source_iName = "srcIName_$dataID"
    val source_iMatchCell = "srcIMatchCell_$dataID"
    val source_iMatchOff = "srcIMatchOff_$dataID"
    val source_iMatchOn = "srcIMatchOn_$dataID"
    val source_iMatch = "srcIMatch_$dataID"

    tr {
        td {
            tooltipText(
                "Root source type",
                "What data this command will interact with"
            )
        }

        td {
            select {
                id = sourceRootType
                onChange = """
                    $source_RSubCell.style.display = 'none';
                    $source_VSubCell.style.display = 'none';
                    $source_iNameCell.style.display = 'none';
                    $source_iMatchCell.style.display = 'none';
                    switch(selectedIndex) {
                      case 1:
                      case 2:
                        $source_RSubCell.style.display = '';
                        break;
                      case 3:
                        $source_VSubCell.style.display = '';
                        break;
                      case 4:
                        break;
                    }
                """.trimIndent()

                option {
                    selected = true
                    +"None"
                }
                option { +"Request" }
                option { +"Request" }
                option { +"Variable" }
                option { +"Uses" }
            }
        }
    }

    tr {
        id = source_RSubCell
        display = DisplayFlags.none

        td {
            tooltipText(
                "HTML sub-source type",
                "What data this command will interact with"
            )
        }

        td {
            select {
                id = source_RSub
                onChange = """
                    $source_iNameCell.style.display = 'none';
                    $source_iMatchCell.style.display = 'none';
                    switch(selectedIndex) {
                      case 1: 
                        $source_iNameCell.style.display = '';
                        $source_iMatchCell.style.display = ''
                        break;
                      case 2:
                        $source_iMatchCell.style.display = '';
                        break;
                    }
                    """.trimIndent()
                option {
                    selected = true
                    +"None"
                }
                option { +"Head" }
                option { +"Body" }
            }
        }
    }

    tr {
        id = source_VSubCell
        display = DisplayFlags.none

        td {
            tooltipText(
                "Variable scope level",
                "Scope area of the search."
            )
        }
        td {
            select {
                id = source_VSub
                option {
                    selected = true
                    +"0: self"
                }
                option { +"1: Chapter" }
                option { +"2: Test Bounds" }
            }
        }
    }

    tr {
        id = source_iNameCell
        display = DisplayFlags.none

        td {
            checkBoxInput {
                onClick = """
                    if(checked){
                      $source_iNameOff.style.display = 'none';
                      $source_iNameOn.style.display = '';
                    } else {
                      $source_iNameOff.style.display = '';
                      $source_iNameOn.style.display = 'none';
                    }
                """.trimIndent()
            }

            tooltipText(
                "Source index",
                "Item Index within the source item."
            )
        }

        td {
            id = source_iNameOff
            tooltipText(
                "Disabled",
                "No items within the source will be referenced."
            )
        }

        td {
            id = source_iNameOn
            display = DisplayFlags.none

            textInput {
                id = source_iName
                width = "100%"
                placeholder = "Reference item"
            }
        }
    }

    tr {
        id = source_iMatchCell
        display = DisplayFlags.none

        td {
            checkBoxInput {
                onClick = """
                    if(checked){
                      $source_iMatchOff.style.display = 'none';
                      $source_iMatchOn.style.display = '';
                    } else {
                      $source_iMatchOff.style.display = '';
                      $source_iMatchOn.style.display = 'none';
                    }
                """.trimIndent()
            }

            tooltipText(
                "Source match",
                "Item matcher to act on the source."
            )
        }

        td {
            id = source_iMatchOff
            tooltipText(
                "Disabled",
                "No matcher will act on the source."
            )
        }

        td {
            id = source_iMatchOn
            display = DisplayFlags.none

            textInput {
                id = source_iMatch
                width = "100%"
                placeholder = "Matcher action"
            }
        }
    }

    tr {
        td {
            select {
                onChange = """
                   switch(selectedIndex){
                     case 1:
                       editTx_$dataID.innerText = "aaa";
                       break;
                     case 2:
                       editTx_$dataID.innerText = "bbb";
                       break;
                     case 3:
                       editTx_$dataID.innerText = "ccc";
                       break;
                   }
               """.trimIndent()
                option { +"none" }
                option { +"aaa" }
                option { +"bbb" }
                option { +"ccc" }
            }
        }
    }
}
