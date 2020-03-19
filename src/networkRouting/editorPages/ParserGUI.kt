package networkRouting.editorPages

import helpers.RandomHost
import kotlinx.html.*

fun FlowContent.ParserEditor() {
    val instanceID = RandomHost().value

    table {
        width = "100%"
        thead {
            tr {
                th {
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
                        id = "eeee1"
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

                        br()

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
