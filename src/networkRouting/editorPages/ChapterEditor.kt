package networkRouting.editorPages

import R
import com.google.gson.Gson
import helpers.*
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

            form(
                encType = FormEncType.multipartFormData,
                action = TapeRouting.RoutePaths.ACTION.path
            ) {
                hiddenInput(name = "tape") { value = pData.hardTapeName() }
                hiddenInput(name = "chapter") { value = pData.hardChapName() }
                hiddenInput(name = "network") {
                    id = name
                    disabled = true
                }

                val isLive = pData.chapter?.alwaysLive.isTrue()
                table {
                    tr {
                        th {
                            style = "width: 15%"
                            +"Name"
                        }
                        td {
                            val nameAction = if (pData.expectedChapName != null)
                                "nameReset.hidden = nameChap.value == nameChap.placeholder;"
                            else ""
                            val chapNameAction = "nameChap.value = '%s';%s"

                            div {
                                textInput(name = "nameChap") {
                                    disableEnterKey
                                    id = name
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

                            pData.chapter?.also {
                                br()
                                +"Size: %s".format(
                                    Gson().toJsonTree(it).fileSize()
                                )
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

                                infoText("attrInfo", "chapter")
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
                                                    pData.chapter?.attractors?.routingPath?.value.orEmpty()

                                                placeholder = if (path.isBlank())
                                                    "sub/path/here" else path
                                                value = path
                                            }
                                        }
                                    }

                                    pData.chapter?.attractors.also { attr ->
                                        addMatcherRow(attr?.queryParamMatchers) {
                                            it.matcherName = "Parameter"
                                        }

                                        addMatcherRow(attr?.queryHeaderMatchers) {
                                            it.matcherName = "Header"
                                        }

                                        addMatcherRow(attr?.queryBodyMatchers) {
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

                            linebreak()
                            tooltipText(
                                "Usages: ",
                                "usageInfo"
                            )
                            numberInput(name = "usesCount") {
                                min = MockUseStates.ALWAYS.state.toString()
                                max = Int.MAX_VALUE.toString()
                                value = when (val uses = pData.chapter?.mockUses) {
                                    null -> MockUseStates.ALWAYS.state
                                    else -> uses
                                }.toString()
                            }

                            linebreak()
                            tooltipText(
                                "Live - ",
                                "chapLiveInfo"
                            )
                            checkBoxInput(name = "useLive") {
                                checked = isLive
                                onClick = """
                                        if (checked) {
                                            requestDataDiv.classList.add('opacity50');
                                            responseDataDiv.classList.add('opacity50');
                                        } else {
                                            requestDataDiv.classList.remove('opacity50');
                                            responseDataDiv.classList.remove('opacity50');
                                        }
                                    """.trimIndent()
                            }
                        }
                    }

                    tr {
                        th {
                            a {
                                if (!pData.newChapter)
                                    href = pData.hrefEdit(hNetwork = "request")
                                +"Request"
                            }
                            infoText("(optional, not used for incoming calls)")
                        }
                        td {
                            makeToggleButton("requestDataDiv")

                            div(classes = if (isLive) "opacity50" else "") {
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
                                                style = if (pData.chapter?.requestData == null)
                                                    "padding: 0.4em 2em;"
                                                else
                                                    "padding: 0.2em 1em;"
                                                getButton {
                                                    formAction = TapeRouting.RoutePaths.EDIT.path
                                                    onClick = """
                                                        network.value = 'request';
                                                        network.disabled = false;
                                                    """.trimIndent()
                                                    if (pData.chapter?.requestData == null)
                                                        +"Create"
                                                    else
                                                        +"Edit"
                                                }
                                            }

                                            if (pData.chapter?.requestData != null) {
                                                td {
                                                    style = "text-align: center;"
                                                    tooltipText(
                                                        "Clear Request",
                                                        "chapReqClear"
                                                    )
                                                    br()
                                                    checkBoxInput(name = "clearRequest") {
                                                        onClick = """
                                                            if (checked)
                                                                requestDiv.classList.add('opacity50');
                                                            else
                                                                requestDiv.classList.remove('opacity50');
                                                        """.trimIndent()
                                                    }
                                                }

//                                                +" "
//                                                button(type = ButtonType.button) {
//                                                    onClick = "beautifyField(requestBody);"
//                                                    +"Beautify Body"
//                                                }
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
                        th {
                            a {
                                if (!pData.newChapter)
                                    href = pData.hrefEdit(hNetwork = "response")
                                +"Response"
                            }
                        }
                        td {
                            makeToggleButton("responseDataDiv")

                            div(classes = if (isLive) "opacity50" else "") {
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
                                                style = "padding: 0.2em 1em;"
                                                getButton {
                                                    formAction = TapeRouting.RoutePaths.EDIT.path
                                                    onClick = """
                                                        network.value = 'response';
                                                        network.disabled = false;
                                                    """.trimIndent()
                                                    if (pData.chapter?.responseData == null)
                                                        +"Create"
                                                    else
                                                        +"Edit"
                                                }
                                            }

                                            td {
                                                hiddenInput(name = "ref") {
                                                    value = "%s%s".format(
                                                        pData.hardTapeName(),
                                                        pData.hardChapName()
                                                    ).hashCode().toString()
                                                }
                                                getButton {
                                                    formAction = "../" + DataGen.RoutePaths.Response.asSubPath
                                                    disabled = pData.chapter == null
                                                    +"Generate"
                                                }
                                            }

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
                                                        if (checked && ${pData.chapter?.awaitResponse.isFalse()})
                                                            if (confirm(${R.getProperty("chapAwaitConfirm")})) {
                                                                responseDiv.classList.add('opacity50');
                                                                return true;
                                                            } else return false;
                                                        else
                                                            responseDiv.classList.remove('opacity50');
                                                    """.trimIndent()
                                                }
                                            }

                                            if (pData.chapter?.responseData != null) {
//                                                +" "
//                                                td {
//                                                    button(type = ButtonType.button) {
//                                                        onClick = """
//                                                            responseInput.value = '';
//                                                        """.trimIndent()
//                                                        +"Clear Response"
//                                                    }
//                                                }
//
//                                                td {
//                                                    if (pData.chapter?.responseData?.body != null)
//                                                        button(type = ButtonType.button) {
//                                                            onClick = "beautifyField(responseBody);"
//                                                            +"Beautify Body"
//                                                        }
//                                                }
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
                            hiddenInput(name = "afterAction") { id = name }

                            div {
                                postButton(name = "Action") {
                                    value = "SaveChapter"
                                    onClick = "submitCheck(nameChap);"
                                    +"Save"
                                }
                            }

                            div {
                                p {
                                    postButton(name = "Action") {
                                        value = "SaveChapter"
                                        onClick = "submitCheck(nameChap); afterAction.value = 'newChapter';"
                                        +"Save and Add Another"
                                    }
                                }
                            }

                            div {
                                postButton(name = "Action") {
                                    value = "SaveChapter"
                                    onClick = "submitCheck(nameChap); afterAction.value = 'parentTape';"
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
