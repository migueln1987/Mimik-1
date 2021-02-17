package mimik.networkRouting.editorPages

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinUtils.appendLines
import kotlinUtils.isTrue
import kotlinUtils.isValidJSON
import kotlinUtils.uppercaseFirstLetter
import kotlinx.html.*
import okhttp3.RequestData
import okhttp3.ResponseData
import okhttp3.hostPath
import okhttp3.toParameters

object NetworkDataEditor : EditorModule() {
    fun HTML.dataEditor(params: Parameters) {
        val pData = params.toActiveEdit

        head {
            unsafeScript {
                +"""
                    function toParsedUrl(url) {
                        parsedUrl.innerText = preVerifyURL(url);
                    }
                """.trimIndent().appendLines(JS.all)
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
                            width = "15%"
                            +"Network type"
                        }
                        td { +networkType }
                    }

                    val isRequest = pData.expectedNetworkType == "request"

                    when (isRequest) {
                        true -> {
                            val nData = pData.networkData as? RequestData
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
                                        placeholder = nData?.httpUrl?.hostPath ?: R.getProperty("urlPlaceholderExample")
                                        value = nData?.httpUrl?.hostPath.orEmpty()
                                        size = "${placeholder.length + 20}"
                                        onKeyUp = "toParsedUrl(value)"
                                    }

                                    unsafeScript {
                                        +"""
                                            requestUrl.addEventListener('paste', (event) => {
                                                var paste = getPasteResult(event);
                                                
                                                var query = extractQueryFromURL(paste);
                                                if (query.length > 0) {
                                                    reqQuery.value = query;
                                                    requestUrl.value = preVerifyURL(paste);
                                                    toParsedUrl(paste);
                                                    event.preventDefault();
                                                }
                                            });
                                        """.trimIndent()
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
                                                    else -> if (url.isValidJSON)
                                                        url else "{ no match }"
                                                }
                                            }
                                        }
                                    }
                                    unsafeScript { +"toParsedUrl(requestUrl.value);" }
                                }
                            }

                            tr {
                                th { +"Query" }
                                td {
                                    tooltipText("Info", "genKVDataField")
                                    br()
                                    paramTextArea(nData?.httpUrl.toParameters) {
                                        name = "reqQuery"
                                        id = name
                                    }
                                }
                            }
                        }

                        false -> {
                            val nData = pData.networkData as? ResponseData
                            tr {
                                th { +"Code" }
                                td {
                                    select {
                                        name = "responseCode"
                                        id = name
                                        onChange = "responseCodeDes.selectedIndex = responseCode.selectedIndex;"

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
                                        onChange = "responseCode.selectedIndex = responseCodeDes.selectedIndex;"
                                        HttpStatusCode.allStatusCodes.forEach {
                                            option {
                                                if (nData?.code == it.value)
                                                    selected = true
                                                +it.description
                                            }
                                        }
                                    }

                                    unsafeScript {
                                        +"responseCodeDes.selectedIndex = responseCode.selectedIndex;"
                                    }
                                }
                            }
                        }
                    }

                    tr {
                        th { +"Headers" }
                        td {
                            tooltipText("Info", "genKVDataField")
                            br()
                            headerTextArea(pData.networkData?.headers) {
                                name = "netHeaders"
                                id = name
                            }

                            unsafeScript {
                                +"""
                                    netHeaders.addEventListener('paste', (event) => {
                                        var paste = getPasteResult(event);
                                        var selEnd = netHeaders.selectionEnd || paste.length;
                                        
                                        netHeaders.value = paste.replace(/ *(?<!\d): */g, ' : ')
                                            .replace(/^\s*$(?:\r\n?|\n)?/gm, '');
                                        netHeaders.selectionStart = selEnd;
                                        netHeaders.selectionEnd = selEnd;
                                        event.preventDefault();
                                    });
                                """.trimIndent()
                            }
                        }
                    }

                    tr {
                        th { +"Body" }
                        td {
                            table {
                                tr {
                                    th {
                                        resizableCol
                                        +"Data"
                                    }
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
                                        unsafeScript {
                                            +"""
                                                networkBody.addEventListener('paste', (event) => {
                                                    var paste = getPasteResult(event);
                                                    var selEnd = networkBody.selectionEnd || paste.length;
                                                    
                                                    var formatted = prettyJson(paste);
                                                    if (formatted != paste) {
                                                        networkBody.value = formatted;
                                                        selEnd.selectionStart = selEnd;
                                                        selEnd.selectionEnd = selEnd;
                                                        networkBody.style.height = (networkBody.scrollHeight - 4) + 'px';
                                                        event.preventDefault();
                                                    }
                                                });
                                                formatParentFieldWidth(networkBody);
                                                beautifyField(networkBody);
                                            """.trimIndent()
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
                            if (isRequest) {
                                tooltipText(
                                    "Parse Request to attractors: ",
                                    "genReqParseAttr"
                                )
                                checkBoxInput(name = "parseAttractors") {
                                    checked = pData.chapter?.attractors?.isInitial.isTrue
                                }
                                linebreak()
                            }
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
