package networkRouting.editorPages

import helpers.RandomHost
import helpers.parser.P4Command
import kotlinx.html.*
import kotlin.math.absoluteValue

fun FlowContent.SequenceViewer(seqGroups: ArrayList<ArrayList<P4Command>>) {
    val testGroups = arrayListOf(
        arrayListOf(
            P4Command(),
            P4Command(),
            P4Command()
        ),
        arrayListOf(
            P4Command(),
            P4Command(),
            P4Command(),
            P4Command()
        )
    )

    val Sortable_root = """
            Sortable.create(level_root, {
              group: 'level_root',
              filter: '.sjs_noDrag',
              ghostClass: 'sjs_ghost',
              animation: 100
            });
        """.trimIndent()

    fun sortableLevel1(name: String): String {
        return """
            Sortable.create(${name}, {
                group: 'level_1',
                handle: '.sjs_handle',
                ghostClass: 'sjs_ghost',
                animation: 100
            });
        """.trimIndent()
    }

    fun setupToggle(togBtn: String, togDiv: String) {
        val isExpanded = false
        val rngName = "toggles_${RandomHost().value}"
        unsafeScript {
            +"""
                $togBtn.onclick = function() { toggleView($togBtn, $togDiv); }
                setupToggleButtonTarget($togDiv);
                toggleView($togBtn,$togDiv);
            """.trimIndent()
        }
    }

    script(src = "../assets/libs/v4Parser.js") {}

    div(classes = "sjs_group sjs_col nested-sortable") {
        id = "level_root"

//        seqGroups.forEach { seqList ->
        testGroups.forEach { seqList ->
            val listID = seqList.hashCode().absoluteValue
            val listId = "Level1_${seqList.hashCode().absoluteValue}"
            val toggleBtn = "toggle_$listID"

            table(classes = "sjs_group-item nested-1") {
                border { raw = "0" }
                width = "auto"
                tr {
                    td {
                        width = "1px"
                        background { color = "inherit" }
                        padding { all = "0" }
                        div(classes = "sjs_handle") {
                            padding { horizontal = "8px" }
                            +":::"
                        }
                    }
                    td {
                        width = "100%"
                        background { color = "inherit" }
                        padding { all = "0" }
                        button(type = ButtonType.button, classes = "inline collapsible") {
                            id = toggleBtn
                            +"Sequence $listID"
                        }
                    }
                }
                tr {
                    td {
                        colSpan = "2"
                        padding { all = "0" }
                        div(classes = "sjs_group sjs_col sjs_noDrag") {
                            id = listId
                            background { color = "unset" }
                            padding { all = "4px" }
                            border {
                                all {
                                    color = "gray"
                                    width = "2px"
                                }
                            }
                            display = DisplayFlags.none

                            seqList.forEach { seq ->
                                val editID = "edit_${listID}_${seq.hashCode()}"
                                val titleStringID = "string_${listID}_${seq.hashCode()}"
                                val contentID = "content_${listID}_${seq.hashCode()}"

                                table(classes = "sjs_group-item nested-2") {
                                    width = "auto"
                                    padding { all = "0px" }
                                    tr {
                                        td {
                                            width = "1px"
                                            background { color = "unset" }
                                            div(classes = "sjs_handle") { +":::" }
                                        }
                                        td {
                                            width = "100%"
                                            background { color = "unset" }
                                            id = titleStringID
                                            div { +seq.toString() }
                                        }
                                        td {
                                            id = editID
                                            width = "1px"
                                            background { color = "unset" }
                                            button(type = ButtonType.button) {
                                                onClick = """
                                                    $titleStringID.style.visibility = 'hidden';
                                                    $editID.style.visibility = 'hidden';
                                                    $contentID.style.visibility = 'visible';
                                                """.trimIndent()
                                                +"Edit"
                                            }
                                        }
                                    }
                                    tr {
                                        id = contentID
                                        visibility = VisibilityFlags.collapse
                                        td {
                                            +"Testing"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                setupToggle(toggleBtn, listId)
                unsafeScript { +sortableLevel1(listId) }
            }
        }
    }

    unsafeScript { +Sortable_root }
}

fun FlowContent.ParserEditor() {
    val instanceID = RandomHost().value
    val indexID = 0

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
                            if(checked) {
                                eeee1.style.display = 'inline';
                                eeee2.style.display = 'none';
                            } else {
                                eeee1.style.display = 'none';
                                eeee2.style.display = 'inline';
                            }
                        """.trimIndent()
                    }
                }

                th {
                    tooltipText(
                        "Source",
                        "What is the context of this step"
                    )
                    br()
                }

                th {
                    tooltipText(
                        "Action",
                        "Processing with the source content"
                    )
                }
            }
        }


        tbody {
            tr {
                td {
                    div {
                        id = "parser_cond_${instanceID}_"
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

                    div {
                        id = "eeee2"
                        tooltipText(
                            "Disabled",
                            "This step's end state will not affect other steps"
                        )
                    }
                }

                td {
                    text("test 2")
                }

                td {
                    text("test 3")
                }
            }
        }
    }
}
