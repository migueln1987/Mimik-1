package networkRouting.editorPages

import R
import helpers.infoText
import helpers.isTrue
import helpers.makeToggleButton
import helpers.tooltipText
import io.ktor.http.Parameters
import kotlinx.html.*
import mimikMockHelpers.MockUseStates

object ChapterEditor : EditorModule() {
    /**
     * Page to edit individual chapters in a tape
     */
    fun HTML.getChapterPage(params: Parameters) {
        val pData = params.toActiveEdit

        head {
            script { unsafe { +JS.all } }
        }

        body {
            setupStyle()
            BreadcrumbNav(pData)

            br()
            if (pData.loadTape_Failed)
                p {
                    +"No tape with the name \"${pData.expectedTapeName}\" was found."
                    br()
                }

            if (pData.loadChap_Failed)
                p {
                    +"No chapter with the name \"${pData.expectedChapName}\" was found."
                    br()
                }

            h1 { +(if (pData.newChapter) "New Chapter" else "Chapter Editor") }

            form(encType = FormEncType.multipartFormData) {
                table {

                    tr {
                        th {
                            style = "width: 15%"
                            +"Name"
                        }
                        td {
                            val nameAction = if (pData.expectedChapName != null)
                                "nameReset.hidden = setName.value == setName.placeholder;"
                            else ""
                            val chapNameAction = "setName.value = '%s';%s"

                            div {
                                textInput(name = "name") {
                                    disableEnterKey
                                    id = "setName"
                                    placeholder = pData.hardChapName(randomHost.valueAsUUID)
                                    value = placeholder
                                    size = "${randomHost.valueAsUUID.length + 10}"
                                    onKeyUp = nameAction
                                }

                                text(" ")
                                if (pData.newChapter) {
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

                                if (pData.expectedChapName != null)
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
                                                    pData.chapter?.attractors?.routingPath?.value
                                                        ?: ""

                                                placeholder = if (path.isBlank())
                                                    "sub/path/here" else path
                                                value = path
                                            }
                                        }
                                    }

                                    pData.chapter?.attractors?.also { attr ->
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
                        th { +"Uses" }
                        td {
                            text("Enabled - ")
                            checkBoxInput(name = "usesEnabled") {
                                checked = MockUseStates.isEnabled(
                                    pData.chapter?.mockUses
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
                                value = when (val uses = pData.chapter?.mockUses) {
                                    null -> MockUseStates.ALWAYS.state
                                    else -> uses
                                }.toString()
                            }

                            br()
                            br()
                            tooltipText(
                                "Live - ",
                                "chapLiveInfo"
                            )
                            checkBoxInput(name = "useLive") {
                                checked = pData.chapter?.alwaysLive.isTrue()
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
                                                if (pData.chapter?.requestData != null) {
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
                                                    if (pData.chapter?.requestData == null) {
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
                                displayInteractionData(pData.chapter?.requestData)
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
                                                    checked = pData.chapter?.awaitResponse.isTrue()
                                                    if (pData.chapter?.awaitResponse == null)
                                                        disabled = true
                                                    onClick = """
                                                        if (checked && ${pData.chapter?.awaitResponse.isTrue()})
                                                            if (confirm('${R.getProperty("chapAwaitConfirm")}')) 
                                                                return true;
                                                            else
                                                                return false;
                                                    """.trimIndent()
                                                }
                                            }

                                            if (pData.chapter?.responseData != null) {
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
                                                    if (pData.chapter?.requestData == null) {
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
                                displayInteractionData(pData.chapter?.responseData)
                            }
                        }
                    }

                    tr {
                        th { +"Save Options" }
                        td {
                            hiddenInput(name = "name_pre") { value = pData.hardChapName("") }
                            hiddenInput(name = "tape") { value = pData.hardTapeName("") }
                            hiddenInput(name = "afterAction") { id = name }

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
