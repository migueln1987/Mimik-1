package networkRouting.editorPages

import R
import helpers.RandomHost
import io.ktor.http.*
import kotlinx.html.*
import mimikMockHelpers.NetworkData
import mimikMockHelpers.RequestData
import mimikMockHelpers.Responsedata

abstract class EditorModule {
    val tapeCatalog get() = MimikContainer.tapeCatalog

    companion object {
        val randomHost = RandomHost()
    }

    enum class Libs_CSS(private val init: String) {
        Sortable(
            """
                .sjs_ghost {
                  opacity: .5;
                  background: #C8EBFB;
                }
            
                .sjs_group {
                  padding-left: 0;
                  margin-bottom: 0;
                }
            
                .sjs_group-item {
                  position: relative;
                  display: block;
                  padding: .75rem 0.5rem;
                  margin-bottom: -1px;
                  background-color: #fff;
                  border: 1px solid rgba(0, 0, 0, .125);
                }
            
                .nested-sortable,
                .nested-1,
                .nested-2,
                .nested-3 {
                  margin-top: 4px;
                  margin-bottom: 4px;
                }
            
                .nested-1 {
                  background-color: #e6e6e6;
                }
            
                .nested-2 {
                  background-color: #cccccc;
                }
            
                .nested-3 {
                  background-color: #b3b3b3;
                }
            
                .sjs_col {
                  flex-basis: 0;
                  flex-grow: 1;
                  max-width: 100%;
                }
            
                .sjs_row {
                  display: -ms-flexbox;
                  display: flex;
                  flex-wrap: wrap;
                  margin-right: -15px;
                  margin-left: -15px;
                }
            
                .sjs_handle {
                  display: inline;
                  cursor: row-resize;
                }
            
                .sjs_noDrag {}
            
                .inline {
                  display: inline;
                }
            """
        );

        val value: String
            get() = init.trimIndent()
    }

    /**
     * Retrieves data from the [Parameters] for the current Tape/ Chapter
     */
    val Parameters.toActiveEdit
        get() = ActiveData(this)

    fun BODY.BreadcrumbNav(params: Parameters = Parameters.Empty) =
        BreadcrumbNav(params.toActiveEdit)

    fun BODY.BreadcrumbNav(data: ActiveData) {
        div(classes = "breadcrumb") {
            div(classes = "subnav") {
                a(classes = "navHeader") {
                    href = data.hrefMake()
                    +"All Tapes"
                }
                if (tapeCatalog.tapes.isNotEmpty())
                    div(classes = "subnav-content") {
                        style = "left: 1em;"
                        tapeCatalog.tapes.forEach {
                            a {
                                href = data.hrefMake(tape = it.name)
                                +it.name
                            }
                        }
                    }
            }

            if (!data.newTape) {
                div(classes = "subnav") {
                    a(classes = "navHeader") {
                        href = data.hrefMake(tape = data.hardTapeName())
                        +"Tape (%s)".format(data.hardTapeName())
                    }

                    data.tape?.chapters?.also { chap ->
                        if (chap.isEmpty()) return@also
                        div(classes = "subnav-content") {
                            style = "left: 5.7em;"
                            chap.forEach {
                                a {
                                    href = data.hrefMake(
                                        tape = data.hardTapeName(),
                                        chapter = it.name
                                    )
                                    +it.name
                                }
                            }
                        }
                    }
                }
            }

            data.chapter?.also { ch ->
                div(classes = "subnav") {
                    a(classes = "navHeader") {
                        href = data.hrefMake(
                            tape = data.hardTapeName(),
                            chapter = data.hardChapName()
                        )
                        +ch.name
                    }
                }

                if (data.expectedNetworkType.isNotBlank())
                    div(classes = "subnav") {
                        a(classes = "navHeader") {
                            +data.expectedNetworkType
                        }
                    }
            }

            // Append a `down caret` to headers which have children
            unsafeScript {
                +"""
                    Array.from(document.getElementsByClassName('navHeader')).forEach(
                        function(item) {
                            var next = item.nextElementSibling;
                            if (next != null && next.classList.contains('subnav-content'))
                                item.classList.add('caret-down');
                        }
                    );
                """.trimIndent()
            }
        }
        br()
    }

    /**
     * Displays the input [data], or displays "{ no data }" if it's null
     */
    fun FlowContent.displayInteractionData(data: NetworkData?) {
        if (data == null) {
            +R["noData", ""]
            return
        }

        div {
            id = when (data) {
                is RequestData -> "requestDiv"
                is Responsedata -> "responseDiv"
                else -> ""
            }
            table {
                thead {
                    tr {
                        th {
                            resizableCol
                            appendStyles("width: 15%")
                            +"Specific"
                        }
                        th {
                            resizableCol
                            appendStyles("width: 40%")
                            +"Headers"
                        }
                        th { +"Body" }
                    }
                }
                tbody {
                    wordBreak_word
                    tr {
                        tapeDataRow(data)
                    }
                }
            }
        }
    }

    private fun TR.tapeDataRow(data: NetworkData) {
        td {
            style = "vertical-align: top;"
            div {
                when (data) {
                    is RequestData -> {
                        text("Method: \n${data.method}")
                        br()
                        +"Url: "
                        a {
                            val url = data.url
                            href = url.orEmpty()
                            +when (url) {
                                null, "" -> R["noData", ""]
                                else -> url
                            }
                        }
                    }
                    is Responsedata -> {
                        text("Code: \n${data.code}")
                        infoText(
                            "Status - '%s'",
                            HttpStatusCode.fromValue(data.code ?: 200).description
                        )
                    }
                    else -> text(R["noData", ""])
                }
            }
        }

        td {
            style = "vertical-align: top;"
            val divID = "resizingTapeData_${data.hashCode()}"
            val headers = data.headers

            if (headers == null || headers.size == 0)
                +R["noData", ""]
            else {
                div {
                    id = divID
                    style = """
                        margin-bottom: 1em;
                        resize: vertical;
                        overflow: auto;
                        min-height: 5em;
                        background-color: #DDD;
                    """.trimIndent()

                    val tableID = "header_${data.hashCode()}"
                    table {
                        id = tableID
                        thead {
                            tr {
                                th {
                                    resizableCol
                                    +"Key"
                                }
                                th { +"Value" }
                            }
                        }
                        tbody {
                            wordBreak_word
                            headers.toMultimap().forEach { (t, u) ->
                                u.forEach {
                                    tr {
                                        td { +t }
                                        td { +it }
                                    }
                                }
                            }
                        }
                    }

                    unsafeScript {
                        +"$divID.style.maxHeight = ($tableID.scrollHeight + 14) + 'px';"
                    }
                }
            }
        }

        td {
            style = """
                vertical-align: top;
                text-align: center;
            """.trimIndent()

            val bodyData = data.body
            if (bodyData == null) {
                +R["noData", ""]
                return@td
            }

            if (data.isImage)
                infoText("[Base64]")

            val areaID = when (data) {
                is RequestData -> "requestBody"
                is Responsedata -> "responseBody"
                else -> ""
            }

            textArea {
                id = areaID
                readonly = true
                style = """
                    margin-bottom: 1em;
                    resize: vertical;
                    width: 100%;
                    min-height: 5em;
                    background-color: #EEE;
                """.trimIndent()
                readonly = true
                +bodyData
            }

            unsafeScript { +"beautifyField($areaID);" }
        }
    }
}
