package networkRouting.editorPages

import helpers.RandomHost
import helpers.isTrue
import io.ktor.http.Parameters
import kotlinx.html.* // ktlint-disable no-wildcard-imports
import mimikMockHelpers.MockUseStates

object ChapterEditor : EditorModule() {
    /**
     * Page to edit individual chapters in a tape
     */
    fun HTML.getChapterPage(params: Parameters) {
        val activeTape = tapeCatalog.tapes
            .firstOrNull { it.name == params["tape"] }

        val activeChapter = activeTape?.chapters
            ?.firstOrNull { it.name == params["chapter"] }

        val newTape = activeTape == null
        val newChapter = activeChapter == null

        val expectedTapeName = params["tape"] ?: RandomHost().value.toString()
        val expectedChapterName = params["chapter"]
            ?.let { if (it.isBlank()) null else it }

        head {
            script { unsafe { +js.all } }
        }

        body {
            setupStyle()

            getForm(action = TapeRouting.RoutePaths.ALL.path) {
                button { +"...View All tapes" }
                if (!newTape) {
                    +" "
                    hiddenInput(name = "tape") { value = expectedTapeName }
                    button {
                        formAction = TapeRouting.RoutePaths.EDIT.path
                        +"..to Parent tape"
                    }
                }
            }

            br()
            if (newTape)
                p {
                    +"No tape with the name \"${params["tape"]}\" was found."
                    br()
                }

            if (newChapter && !expectedChapterName.isNullOrBlank())
                p {
                    +"No chapter with the name \"${params["chapter"]}\" was found."
                    br()
                }

            h1 { +(if (newChapter) "New Chapter" else "Chapter Editor") }

            form(encType = FormEncType.multipartFormData) {
                table {

                    tr {
                        th {
                            style = "width: 15%"
                            +"Name"
                        }
                        td {
                            val nameAction = if (expectedChapterName != null)
                                "nameReset.hidden = setName.value == setName.placeholder;"
                            else ""
                            val chapNameAction = "setName.value = '%s';%s"

                            div {
                                textInput(name = "name") {
                                    disableEnterKey
                                    id = "setName"
                                    if (activeChapter == null) {
                                        placeholder = expectedChapterName ?: randomHost.valueAsUUID
                                        value = expectedChapterName ?: randomHost.valueAsUUID
                                    } else {
                                        placeholder = activeChapter.name
                                        value = activeChapter.name
                                    }
                                    size = "${randomHost.valueAsUUID.length + 10}"
                                    onKeyUp = nameAction
                                }

                                text(" ")
                                if (activeChapter == null) {
                                    button(type = ButtonType.button) {
                                        onClick = chapNameAction.format(
                                            randomHost.value,
                                            nameAction
                                        )
                                        +"Use generated number"
                                    }

                                    button(type = ButtonType.button) {
                                        onClick = chapNameAction.format(
                                            randomHost.valueAsChars,
                                            nameAction
                                        )
                                        +"Use generated string"
                                    }

                                    button(type = ButtonType.button) {
                                        onClick = chapNameAction.format(
                                            randomHost.valueAsUUID,
                                            nameAction
                                        )
                                        +"Use generated UUID"
                                    }
                                }

                                if (expectedChapterName != null)
                                    button(type = ButtonType.button) {
                                        id = "nameReset"
                                        hidden = true
                                        onClick = """
                                            ChapName.value = ChapName.placeholder;
                                            nameReset.hidden = true;
                                        """.trimIndent()
                                        +"Reset"
                                    }
                            }
                        }
                    }

                    tr {
                        th { +"Request Attractors" }
                        td {
                            makeToggleButton("reqView")

                            div {
                                id = "reqView"
                                br()

                                infoText("attrInfo", arrayOf("chapter"))
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
                                                    activeChapter?.attractors?.routingPath?.value
                                                        ?: ""

                                                placeholder = if (path.isBlank())
                                                    "sub/path/here" else path
                                                value = path
                                            }
                                        }
                                    }

                                    addMatcherRow(activeChapter?.attractors?.queryParamMatchers) {
                                        it.matcherName = "Parameter"
                                    }

                                    addMatcherRow(activeChapter?.attractors?.queryHeaderMatchers) {
                                        it.matcherName = "Header"
                                    }

                                    addMatcherRow(activeChapter?.attractors?.queryBodyMatchers) {
                                        it.matcherName = "Body"
                                        it.valueIsBody = true
                                    }
                                }

                                br()
                                infoText("attrFlagOpt")
                                infoText("attrFlagExt")
                            }
                        }
                    }

                    tr {
                        th { +"Uses" }
                        td {
                            text("Enabled - ")
                            checkBoxInput(name = "usesEnabled") {
                                checked = MockUseStates.isEnabled(
                                    activeChapter?.mockUses
                                        ?: MockUseStates.ALWAYS.state
                                )
                            }

                            br()
                            br()
                            tooltipText(
                                "Usages: ",
                                "usageInfo"
                            )
                            numberInput {
                                min = MockUseStates.ALWAYS.state.toString()
                                max = Int.MAX_VALUE.toString()
                                value = when (activeChapter?.mockUses) {
                                    null -> MockUseStates.ALWAYS.state
                                    else -> activeChapter.mockUses
                                }.toString()
                            }

                            br()
                            br()
                            tooltipText(
                                "Live - ",
                                "chapLiveInfo"
                            )
                            checkBoxInput(name = "useLive") {
                                checked = activeChapter?.alwaysLive.isTrue()
                                onInput = """
                                        alert("todo; enable/ disable Response data + clear Response data")
                                    """.trimIndent()
                            }
                        }
                    }

                    tr {
                        th {
                            text("Request")
                            infoText("(optional, not used for incoming calls)")
                        }
                        td {
                            makeToggleButton("requestDataDiv")

                            div {
                                id = "requestDataDiv"
                                table {
                                    style = "width: auto;"
                                    thead {
                                        tr {
                                            th {
                                                colSpan = "5"
                                                +"Actions"
                                            }
                                        }
                                    }
                                    tbody {
                                        tr {
                                            td {
                                                style = "padding: 0.4em 2em;"
                                                if (activeChapter?.requestData != null) {
                                                    button(type = ButtonType.button) {
                                                        onClick = "requestInput.value = '';"
                                                        +"Clear Request"
                                                    }

                                                    +" "
                                                    button(type = ButtonType.button) {
                                                        onClick = "beautifyField(requestBody);"
                                                        +"Beautify Body"
                                                    }
                                                }

                                                +" "
                                                button(type = ButtonType.button) {
                                                    if (activeChapter?.requestData == null) {
                                                        onClick =
                                                            "alert('Insert {create New Request}')"
                                                        +"Create"
                                                    } else {
                                                        onClick =
                                                            "alert('Insert {Edit Request}')"
                                                        +"Edit"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                br()
                                if (activeChapter?.requestData == null)
                                    +"{ no data }"
                                else
                                    displayInteractionData(activeChapter.requestData)
                            }
                        }
                    }

                    tr {
                        th { +"Response" }
                        td {
                            makeToggleButton("responseDataDiv")

                            div {
                                id = "responseDataDiv"
                                table {
                                    style = "width: auto;"
                                    thead {
                                        tr {
                                            th {
                                                colSpan = "5"
                                                +"Actions"
                                            }
                                        }
                                    }
                                    tbody {
                                        tr {
                                            td {
                                                style = "text-align: center;"
                                                tooltipText(
                                                    "Await status",
                                                    "chapAwaitInfo"
                                                )
                                                br()
                                                checkBoxInput(name = "responseAwait") {
                                                    checked = activeChapter?.awaitResponse ?: true
                                                    if (activeChapter?.awaitResponse == null)
                                                        disabled = true
                                                    onClick = """
                                                        if (checked && ${activeChapter?.awaitResponse.isTrue()})
                                                            if (confirm('${R.getProperty("chapAwaitConfirm")}')) 
                                                                return true;
                                                            else
                                                                return false;
                                                    """.trimIndent()
                                                }
                                            }

                                            if (activeChapter?.responseData != null) {
                                                td {
                                                    button(type = ButtonType.button) {
                                                        onClick = "requestInput.value = '';"
                                                        +"Clear Request"
                                                    }
                                                }

                                                td {
                                                    button(type = ButtonType.button) {
                                                        onClick = "beautifyField(requestBody);"
                                                        +"Beautify Body"
                                                    }
                                                }
                                            }

                                            td {
                                                button(type = ButtonType.button) {
                                                    if (activeChapter?.requestData == null) {
                                                        onClick =
                                                            "alert('Insert {create New Response}')"
                                                        +"Create"
                                                    } else {
                                                        onClick =
                                                            "alert('Insert {Edit Response}')"
                                                        +"Edit"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                br()
                                if (activeChapter?.responseData == null)
                                    +"{ no data }"
                                else
                                    displayInteractionData(activeChapter.responseData)
                            }
                        }
                    }

                    tr {
                        th { +"Save Options" }
                        td {
                            hiddenInput(name = "name_pre") {
                                value = expectedChapterName ?: ""
                            }
                            hiddenInput(name = "tape") { value = expectedTapeName }
                            hiddenInput(name = "afterAction") {
                                id = name
                                value = ""
                            }

                            div {
                                postButton(name = "Action") {
                                    formAction = TapeRouting.RoutePaths.ACTION.path
                                    value = "SaveChapter"
                                    onClick = "submitCheck(); afterAction.value = 'resume';"
                                    +"Save"
                                }
                            }

                            div {
                                p {
                                    postButton(name = "Action") {
                                        formAction = TapeRouting.RoutePaths.ACTION.path
                                        value = "SaveChapter"
                                        onClick = "submitCheck(); afterAction.value = 'new';"
                                        +"Save and Add Another"
                                    }
                                }
                            }

                            div {
                                postButton(name = "Action") {
                                    formAction = TapeRouting.RoutePaths.ACTION.path
                                    value = "SaveChapter"
                                    onClick = "submitCheck();"
                                    +"Save and goto Tape"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
