package networkRouting.editorPages

import helpers.appendLines
import helpers.uppercaseFirstLetter
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.html.*
import mimikMockHelpers.Requestdata
import mimikMockHelpers.Responsedata

object NetworkDataEditor : EditorModule() {
    fun HTML.dataEditor(params: Parameters) {
        val pData = params.toActiveEdit

        head {
            script {
                unsafe {
                    +"""
                    function regexUrl(url) {
                        parsedUrl.innerText = preVerifyURL(url);
                    }
                """.trimIndent().appendLines(JS.all)
                }
            }
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

            val networkType = pData.expectedNetworkType.uppercaseFirstLetter()
            h1 { +(if (pData.newNetworkData) "New %s" else "%s Editor").format(networkType) }

            form(
                encType = FormEncType.multipartFormData,
                action = TapeRouting.RoutePaths.ACTION.path
            ) {
                hiddenInput(name = "tape") { value = pData.hardTapeName() }
                hiddenInput(name = "chapter") { value = pData.hardChapName() }
                hiddenInput(name = "network") { value = pData.expectedNetworkType }
                hiddenInput(name = "afterAction") { id = name }

                table {
                    tr {
                        th {
                            style = "width: 15%"
                            +"Network type"
                        }
                        td { +networkType }
                    }

                    when (pData.expectedNetworkType) {
                        "request" -> {
                            val nData = pData.networkData as? Requestdata
                            tr {
                                th { +"Method" }
                                td {
                                    select {
                                        name = "requestMethod"
                                        id = name
                                        HttpMethod.DefaultMethods.forEach {
                                            option {
                                                if (nData?.method?.toUpperCase() == it.value)
                                                    selected = true
                                                +it.value.toLowerCase().uppercaseFirstLetter()
                                            }
                                        }
                                    }
                                }
                            }

                            tr {
                                th { +"Url" }
                                td {
                                    textInput(name = "requestUrl") {
                                        disableEnterKey
                                        id = name
                                        placeholder = nData?.url.orEmpty()
                                        value = placeholder
                                        size = "${placeholder.length + 20}"
                                        onLoad = "regexUrl(value)"
                                        onKeyUp = "regexUrl(value)"
                                    }

                                    br()
                                    div {
                                        style = "margin-top: 6px"
                                        text("Parsed url: ")
                                        i {
                                            a {
                                                id = "parsedUrl"
                                                +when (val url = nData?.url) {
                                                    null -> "{ empty }"
                                                    "" -> "" // isBlank()
                                                    else -> url
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "response" -> {
                            val nData = pData.networkData as? Responsedata
                            tr {
                                th { +"Code" }
                                td {
                                    select {
                                        name = "responseCode"
                                        id = name
                                        onChange = """
                                                responseCodeDes.selectedIndex = responseCode.selectedIndex;
                                            """.trimIndent()

                                        HttpStatusCode.allStatusCodes.forEach {
                                            option {
                                                if ((nData?.code ?: 200) == it.value)
                                                    selected = true
                                                +it.value.toString()
                                            }
                                        }
                                    }

                                    +" Description: "

                                    select {
                                        id = "responseCodeDes"
                                        onChange = """
                                                responseCode.selectedIndex = responseCodeDes.selectedIndex;
                                            """.trimIndent()
                                        HttpStatusCode.allStatusCodes.forEach {
                                            option {
                                                if (nData?.code == it.value)
                                                    selected = true
                                                +it.description
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    tr {
                        th { +"Headers" }
                        td { addHeaderTable(pData.networkData?.headers) }
                    }

                    tr {
                        th { +"Body" }
                        td {
                            table {
                                tr {
                                    th { +"Data" }
                                    th { +"Actions" }
                                }
                                tr {
                                    td {
                                        textArea {
                                            name = "networkBody"
                                            id = name
                                            onKeyPress = "keypressNewlineEnter(networkBody);"
                                            +pData.networkData?.body.orEmpty()
                                        }
                                        script {
                                            unsafe {
                                                +"""
                                                    formatParentFieldWidth(networkBody);
                                                    beautifyField(networkBody);
                                                """.trimIndent()
                                            }
                                        }
                                    }

                                    td {
                                        button(type = ButtonType.button) {
                                            onClick = "beautifyField(networkBody);"
                                            +"Beautify Body"
                                        }
                                    }
                                }
                            }
                        }
                    }

                    tr {
                        th { +"Save" }
                        td {
                            postButton(name = "Action") {
                                value = "SaveNetworkData"
                                +"Save"
                            }
                            br()
                            postButton(name = "Action") {
                                value = "SaveNetworkData"
                                onClick = "afterAction.value = 'viewChapter';"
                                +"Save & goto Chapter"
                            }
                        }
                    }
                }
            }
        }
    }
}
