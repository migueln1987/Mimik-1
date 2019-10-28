package networkRouting.editorPages

import TapeCatalog
import helpers.*
import helpers.attractors.RequestAttractorBit
import helpers.attractors.RequestAttractors
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.html.*
import mimikMockHelpers.Networkdata
import mimikMockHelpers.RecordedInteractions
import mimikMockHelpers.Requestdata
import mimikMockHelpers.Responsedata
import okhttp3.Headers
import tapeItems.BlankTape

abstract class EditorModule {
    val tapeCatalog by lazy { TapeCatalog.Instance }
    val subDirectoryDefault = "[ Default Directory ]"
    val subDirectoryCustom = "[ Custom Directory ]"

    private val filterKey = "filter"
    val loadFlag = "_load_"

    companion object {
        val randomHost = RandomHost()
    }

    @Suppress("unused", "EnumEntryName")
    enum class JS(private val init: String) {
        DisableEnter_func(
            """
                function disableEnter(field) {
                    field.onkeydown = function() {return event.key != 'Enter';};
                }
                """
        ),

        PreVerifyURL_func(
            """
                function preVerifyURL(url) {
                    if (url == null || url.length == 0)
                        return "{ empty }";
                    var regex = /^((?:[a-z]{3,9}:\/\/)?(?:(?:(?:\d{1,3}\.){3}\d{1,3})|(?:[\w.-]+\.(?:[a-z]{2,10}))))/i;
                    var match = url.match(regex);
                    if (match == null)
                        return "{ no match }"; else return match[0];
                }
                """
        ),

        PrettyJson(
            """
                function prettyJson(uglyText) {
                    if (uglyText.trim().length < 1) return uglyText;
                    try {
                        var obj = JSON.parse(uglyText);
                        return JSON.stringify(obj, undefined, 4);
                    }
                    catch(err) {
                        return uglyText;
                    }
                }

                function beautifyField(field) {
                    field.value = prettyJson(field.value);
                    field.style.height = (field.scrollHeight - 4) + 'px';
                }
                """
        ),

        SetIsDisabled_func(
            """
                function setIsDisabled(divObj, newState) {
                    try {
                        divObj.disabled = newState;
                    } catch (E) {}
    
                    for (var x = 0; x < divObj.childNodes.length; x++) {
                        setIsDisabled(divObj.childNodes[x], newState);
                    }
                }
                """
        ),

        FormatParentFieldWidth_func(
            """
                function formatParentFieldWidth(field) {
                    field.style.width = "100%";
    
                    var isEditing = false;
                    function adjustFullWidth() {
                        field.onmousedown = function() { isEditing = true; }
                        field.onmouseup = function() { isEditing = false; }
                        field.onmousemove = function() {
                            if (isEditing) {
                                field.parentElement.width = field.clientWidth;
                            }
                        }
                    }
                    adjustFullWidth();
                    new ResizeObserver(adjustFullWidth).observe(field);
                }
                """
        ),

        CreateTextInput_func(
            """
                function createTextInput(fieldType, fieldID, expandable) {
                    expandable = expandable || false;
    
                    var inputField = inputField = document.createElement("input");
                    if (expandable) {
                        inputField = document.createElement("textarea");
                        inputField.onkeypress = keypressNewlineEnter(inputField);
                    }
                    inputField.name = fieldType + fieldID;
                    inputField.id = inputField.name;
                    return inputField;
                }
                """
        ),

        /**
         * On the [field], hitting the 'Enter' key will add a new line
         */
        KeyPressNewlineEnter_func(
            """
            function keypressNewlineEnter(field) {
                if (event.key == 'Enter') {
                    var pre = field.value.substring(0, field.selectionStart);
                    var post = field.value.substring(field.selectionStart, field.textLength);
                    field.value = pre + "\n" + post;
                    field.style.height = field.scrollHeight + 'px';
                    event.preventDefault();
                }
            }
            """.trimIndent()
        ),

        CreateCheckbox_func(
            """
                function createCheckbox(fieldType, fieldID) {
                    var inputField = document.createElement("input");
                    inputField.name = fieldType + fieldID;
                    inputField.type = "checkbox";
                    return inputField;
                }
                """
        ),

        /**
         * Created a delete button, which when clicked,
         * will call "remove()" on the passed in node
         */
        CreateDeleteBtn_func(
            """
                function createDeleteBtn(node) {
                    var deleteBtn = document.createElement("button");
                    deleteBtn.type = "button";
                    deleteBtn.innerText = "Delete";
                    deleteBtn.onclick = function() { node.remove(); };
                    return deleteBtn;
                }
                """
        ),

        CreateBtn_func(
            """
                function createBtn(name) {
                    name = name || "";
                    var newBtn = document.createElement("button");
                    newBtn.type = "button";
                    newBtn.innerText = name;
                    return newBtn;
                }
                """
        ),

        ToggleDisp_func(
            """
                function toggleView(caller, toToggle) {
                    if (!toToggle.classList.contains("hideableContent")) {
                        toToggle.classList.add("hideableContent");
                        caller.classList.remove("active");
                    } else {
                        caller.classList.toggle("active");
                        toToggle.style.overflow = "hidden";
                        if (toToggle.style.maxHeight){
                            toToggle.style.display = "none";
                            toToggle.style.maxHeight = null;
                        } else {
                            toToggle.style.display = "contents";
                            toToggle.style.width = "100%"
                            toToggle.style.height = "100%"
                            toToggle.style.maxHeight = (toToggle.scrollHeight + 100) + "px";
                            var watcher = setInterval(function() {
                                if (toToggle.clientHeight == toToggle.scrollHeight) {
                                    toToggle.style.overflow = "visible";
                                    clearInterval(watcher);
                                }
                             }, 100);
                        }
                    }
                }
                """
        ),

        SetupToggButton_func(
            """
                function setupTogggButtonTarget(target) {
                    setTimeout(function waitWrapper() {
                        var elem = document.getElementById(target);
                        if (elem == null) setTimeout(waitWrapper, 10);
                        else if (!elem.classList.contains("hideableContent"))
                            elem.classList.add("hideableContent");
                    }, 10);
                }
                """
        ),

        SubmitNameCheck(
            """
                function submitCheck(checkName) {
                    checkName.value = checkName.value.trim();
                    if (checkName.value == "")
                        checkName.value = checkName.placeholder;
                }
                """
        );

        companion object {
            /**
             * Returns a string containing all the values in [JS]
             */
            val all: String
                get() {
                    val result = StringBuilder()
                    values().asList().eachHasNext(
                        { result.append(it.value) },
                        { result.append('\n') }
                    )
                    return result.toString()
                }
        }

        val value: String
            get() = init.trimIndent()
    }

    fun FlowOrMetaDataContent.setupStyle() {
        style {
            unsafe {
                raw(
                    """
                    table {
                        font: 1em Arial;
                        border: 1px solid black;
                        width: 100%;
                    }
                    
                    button {
                        cursor: pointer;
                    }
                    
                    .inputButton {
                        border: 1px solid #ccc;
                        border-radius: 4px;
                    }

                    th {
                        background-color: #ccc;
                        width: auto;
                    }
                    td {
                        background-color: #eee;
                    }
                    th, td {
                        text-align: left;
                        padding: 0.4em 0.4em;
                    }

                    .btn_50wide {
                        width: 50%
                    }
                    .tb_25wide {
                        width: 25%
                    }
                    .center{ text-align: center; }
                    .infoText {
                        font-size: 14px;
                        color: #555
                    }
                    """.trimIndent()
                        .appendLines(
                            breadcrumbStyle,
                            collapsibleStyle,
                            tooltipStyle
                        )
                )
            }
        }
    }

    private val breadcrumbStyle: String
        get() = """
            .breadcrumb {
                padding: 10px;
                position: sticky;
                top: 10px;
                width: calc(100% - 22px);
                background-color: #eee;
                overflow: hidden;
                border: 1px solid black;
                border-radius: 5px;
                z-index: 1;
            }
            
            .breadcrumb div {
                font-size: 18px;
            }
            
            .breadcrumb .subnav+.subnav:before {
                content: "/";
            }
            
            .subnav {
                float: left;
                overflow: hidden;
            }
            
            .navHeader {
                color: #0275d8;
                text-decoration: none;
            }
            
            .navHeader:hover {
                color: white;
                cursor: pointer;
                text-decoration: underline;
            }
            
            .caret-down:after {
                content: "\25be";
                line-height: 1;
                font-style: normal;
                text-rendering: auto;
            }
            
            .subnav .navHeader {
                font-size: 16px;  
                border: none;
                outline: none;
                background-color: inherit;
                font-family: inherit;
                margin: 0;
                padding: 4px;
            }
            
            .navHeader:hover, .subnav-content *:hover {
                background-color: darkslategray;
            }
            
            .subnav-content {
                position: fixed;
                left: 5em;
                top: 42px;
                width: auto;
                background-color: slategray;
                z-index: 1;
                line-height: 1em;
                max-height: 10.5em;
                overflow-y: auto;
                background-color: transparent;
                border-top: 12px solid transparent;
                display: none;
            }
            
            .subnav-content * {
                cursor: pointer;
                float: left;
                color: white;
                padding: 8px;
                padding-right: 10em;
                text-decoration: none;
                display: inline-flex;
                background-color: slategrey;
                border-bottom: 1px solid;
            }
            
            .subnav:hover .subnav-content {
                display: grid;
            }
        """.trimIndent()

    private val collapsibleStyle: String
        get() = """
             /* Button style that is used to open and close the collapsible content */
            .collapsible {
                background-color: #999;
                color: white;
                cursor: pointer;
                padding: 8px 10px;
                margin-bottom: 4px;
                width: 100%;
                text-align: left;
                font-size: 15px;
            }
                
            .collapsible:after {
                content: '\002B';
                color: white;
                font-weight: bold;
                float: right;
                margin-left: 5px;
            }
            
            /* Background color to the button if it is clicked on (add the .active class with JS), and when you move the mouse over it (hover) */
            .active, .collapsible:hover {
                background-color: #888;
            }
            .active:after {
                content: "\2212";
            }
            
            /* Style the collapsible content. Note: "hidden" by default */
            .hideableContent {
                padding: 6px;
                max-height: 0;
                display: none;
                overflow: hidden;
                background-color: #f4f4f4;
                transition: max-height 0.4s ease-out;
            }
            """.trimIndent()

    private val tooltipStyle: String
        get() = """
            .tooltip {
                position: relative;
                display: inline-block;
                border-bottom: 1px dotted #ccc;
                color: #006080;
                cursor: default;
            }
            
            .tooltip .tooltiptext {
                visibility: hidden;
                position: absolute;
                width: 30em;
                max-width: max-content;
                background-color: #555;
                color: #fff;
                text-align: center;
                padding: 0.5em;
                border-radius: 6px;
                z-index: 1;
                opacity: 0;
                transition: opacity 0.3s;
            }
            
            .tooltip:hover .tooltiptext {
                visibility: visible;
                opacity: 1;
            }
            
            .tooltip-right {
                top: -5px;
                left: 125%;  
            }
            
            .tooltip-right::after {
                content: "";
                position: absolute;
                top: 50%;
                right: 100%;
                margin-top: -5px;
                border-width: 5px;
                border-style: solid;
                border-color: transparent #555 transparent transparent;
            }
            
            .tooltip-bottom {
                top: 135%;
                left: 50%;  
                margin-left: -60px;
            }
            
            .tooltip-bottom::after {
                content: "";
                position: absolute;
                bottom: 100%;
                left: 50%;
                margin-left: -5px;
                border-width: 5px;
                border-style: solid;
                border-color: transparent transparent #555 transparent;
            }
            
            .tooltip-top {
                bottom: 125%;
                left: 50%;  
                margin-left: -60px;
            }
            
            .tooltip-top::after {
                content: "";
                position: absolute;
                top: 100%;
                left: 50%;
                margin-left: -5px;
                border-width: 5px;
                border-style: solid;
                border-color: #555 transparent transparent transparent;
            }
            
            .tooltip-left {
                top: -5px;
                bottom:auto;
                right: 128%;  
            }
            
            .tooltip-left::after {
                content: "";
                position: absolute;
                top: 50%;
                left: 100%;
                margin-top: -5px;
                border-width: 5px;
                border-style: solid;
                border-color: transparent transparent transparent #555;
            }
            """.trimIndent()

    val CommonAttributeGroupFacade.disableEnterKey: Unit
        get() {
            onKeyDown = "return event.key != 'Enter';"
        }

    fun Map<String, String>.saveToTape(): BlankTape {
        var isNewTape = true
        val nowTape = tapeCatalog.tapes.firstOrNull { it.name == get("name_pre") }
            ?.also { isNewTape = false }

        val modTape = BlankTape.reBuild(nowTape) { tape ->
            // subDirectory = get("Directory")?.trim()
            val hostValue = get("hostValue")?.toIntOrNull() ?: RandomHost().value
            tape.tapeName = get("TapeName")?.trim() ?: hostValue.toString()
            tape.routingURL = get("RoutingUrl")?.trim()
            tape.allowLiveRecordings = get("SaveNewCalls") == "on"

            tape.attractors = RequestAttractors { attr ->
                get("filterPath")?.trim()?.also { path ->
                    if (path.isNotEmpty())
                        attr.routingPath?.value = path
                }

                if (keys.any { it.startsWith(filterKey) }) {
                    attr.queryParamMatchers = filterFindData("Parameter")
                    attr.queryHeaderMatchers = filterFindData("Header")
                    attr.queryBodyMatchers = filterFindData("Body")
                }
            }
        }

        if (isNewTape) {
            tapeCatalog.tapes.add(modTape)
            if (get("hardtape") == "on") modTape.saveFile()
        } else if (modTape.file?.exists().isTrue())
            modTape.saveFile()

        return modTape
    }

    private fun Map<String, String>.filterFindData(queryName: String): List<RequestAttractorBit>? {
        val mKey = TableQueryMatcher(queryName)

        val values = asSequence()
            .filter { it.value.isNotBlank() }
            .filter { it.key.startsWith(mKey.rowValueName) }
            .associateBy(
                {
                    val keyRange = it.key.lastIndexRange { "($loadFlag)?\\d+" }
                    it.key.substring(keyRange, "")
                },
                { it.value }
            ).filter { it.key.isNotEmpty() }

        val optionals = asSequence()
            .filter { it.key.startsWith(mKey.rowOptName) }
            .associateBy(
                { it.key.removePrefix(mKey.rowOptName) },
                { it.value }
            )

        val excepts = asSequence()
            .filter { it.key.startsWith(mKey.rowExceptName) }
            .associateBy(
                { it.key.removePrefix(mKey.rowExceptName) },
                { it.value }
            )

        val results = values.keys.map { key ->
            RequestAttractorBit {
                it.value = values.getValue(key)
                it.optional = optionals.getOrDefault(key, "") == "on"
                it.except = excepts.getOrDefault(key, "") == "on"
            }
        }
        return if (results.isEmpty()) null else results
    }

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
            get() = matcherName.take(2).toUpperCase()

        private val filterKey = "filter"

        val tableId
            get() = "$filterKey${nameShort}_Table"
        val rowID
            get() = "$filterKey${nameShort}_ID"
        val rowValueName
            get() = "$filterKey${nameShort}_Value"
        val rowOptName
            get() = "$filterKey${nameShort}_Opt"
        val rowExceptName
            get() = "$filterKey${nameShort}_Except"
    }

    /**
     * Adds a row based on the input [tableInfo].
     * Attractor info from [bit] is pre-pended if able to.
     */
    fun TABLE.addMatcherRow(
        bit: List<RequestAttractorBit>?,
        tableInfo: (TableQueryMatcher) -> Unit
    ) {
        val info = TableQueryMatcher().also(tableInfo)

        val colContent = "col_${info.tableId}"
        tr {
            th { +info.matcherName }
            td {
                makeToggleButton(colContent)
                div {
                    id = colContent
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
        table {
            id = info.tableId

            script {
                unsafe {
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
                            formatBtn.onclick = function() { beautifyField(valueInput); };
                            actionBtns.append(formatBtn);
                        }
                    }
                    """.trimIndent()
                        .appendLines(
                            JS.FormatParentFieldWidth_func.value
                        )
                }
            }

            thead {
                tr {
                    th { +"Value" }
                    th { +"Flags" }
                    th { +"Actions" }
                }
            }
            tbody {
                bit?.forEachIndexed { index, bit ->
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
            val fieldName = "${info.rowValueName}${TapeEditor.loadFlag}$count"

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

                script {
                    unsafe {
                        +"formatParentFieldWidth($fieldName);".let {
                            if (info.valueIsBody)
                                it + "beautifyField($fieldName);" else it
                        }
                    }
                }
                // todo; reset value button
            }

            td {
                checkBoxInput(name = "${info.rowOptName}${TapeEditor.loadFlag}$count") {
                    checked = bit.optional ?: false
                }
                text(" Optional")
                br()

                checkBoxInput(name = "${info.rowExceptName}${TapeEditor.loadFlag}$count") {
                    checked = bit.except ?: false
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

    fun FlowContent.addHeaderTable(headers: Headers?) {
        table {
            id = "headerTable"

            script {
                unsafe {
                    +"""
                    var headerID = 0;
                    function addNewHeaderRow() {
                        var newrow = headerTable.insertRow(headerTable.rows.length-1);

                        var keyCol = newrow.insertCell(0);
                        var keyInput = createTextInput("header_key_", headerID);
                        keyCol.append(keyInput);
                        
                        var valCol = newrow.insertCell(1);
                        var valueInput = createTextInput("header_value_", headerID);
                        valCol.append(valueInput);
                        headerID++;
                        
                        var actionBtns = newrow.insertCell(2);
                        actionBtns.append(createDeleteBtn(newrow));
                        actionBtns.append(" ");
                        var cloneBtn = createBtn("Clone");
                        cloneBtn.disabled = true;
                        actionBtns.append(cloneBtn);
                    }
                    """.trimIndent()
                }
            }

            thead {
                tr {
                    th { +"Key" }
                    th { +"Value" }
                    th { +"Actions" }
                }
            }
            tbody {
                var headInc = 0
                headers?.toMultimap()?.forEach { (t, u) ->
                    u.forEach {
                        appendheader(t to it, headInc)
                        headInc++
                    }
                }

                tr {
                    td {
                        colSpan = "3"
                        button(type = ButtonType.button) {
                            onClick = "addNewHeaderRow();"
                            +"Add new Header"
                        }
                    }
                }
            }
        }
    }

    fun TBODY.appendheader(head: Pair<String, String>, idStep: Int) {
        tr {
            td {
                textInput(name = "header_key_load_$idStep") {
                    disableEnterKey
                    placeholder = head.first
                    value = placeholder
                }
            }

            td {
                textInput(name = "header_value_load_$idStep") {
                    disableEnterKey
                    placeholder = head.second
                    value = placeholder
                }
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
            }
        }
    }

    /**
     * Displays the input [data], or displays "{ no data }" if it's null
     */
    fun FlowContent.displayInteractionData(data: Networkdata?) {
        if (data == null) {
            +"{ no data }"
            return
        }

        div {
            id = when (data) {
                is Requestdata -> "requestDiv"
                is Responsedata -> "responseDiv"
                else -> ""
            }
            table {
                thead {
                    tr {
                        th { +"Specific" }
                        th { +"Headers" }
                        th { +"Body" }
                    }
                }
                tbody {
                    tr {
                        tapeDataRow(data)
                    }
                }
            }
        }
    }

    private fun TR.tapeDataRow(data: Networkdata) {
        td {
            div {
                when (data) {
                    is Requestdata -> {
                        text("Method: \n${data.method}")
                        br()
                        +"Url: %s".format(
                            if (data.url.isNullOrBlank())
                                "{ no data }" else data.url
                        )
                    }
                    is Responsedata -> {
                        text("Code: \n${data.code}")
                        infoText(
                            "Status - '%s'",
                            HttpStatusCode.fromValue(data.code ?: 200).description
                        )
                    }
                    else -> text("{ no data }")
                }
            }
        }

        td {
            val divID = "resizingTapeData_${data.hashCode()}"
            val headers = data.headers

            if (headers == null || headers.size() == 0) {
                +"{ no data }"
            } else {
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
                                th { +"Key" }
                                th { +"Value" }
                            }
                        }
                        tbody {
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

                    script {
                        unsafe {
                            +"""
                            $divID.style.maxHeight = ($tableID.scrollHeight + 14) + 'px';
                        """.trimIndent()
                        }
                    }
                }
            }
        }

        td {
            style = "vertical-align: top;"
            val areaID = when (data) {
                is Requestdata -> "requestBody"
                is Responsedata -> "responseBody"
                else -> ""
            }

            if (data.body == null)
                +"{ no data }"

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
                hidden = data.body == null
                +data.body.orEmpty()
            }

            script { unsafe { +"beautifyField($areaID);" } }
        }
    }

    class ActiveData(private val params: Parameters) {
        private val tapeCatalog by lazy { TapeCatalog.Instance }

        var tape: BlankTape? = null
        var chapter: RecordedInteractions? = null
        var networkData: Networkdata? = null

        /**
         * tape is null
         */
        val newTape
            get() = tape == null

        /**
         * chapter is null
         */
        val newChapter
            get() = chapter == null

        val newNetworkData
            get() = networkData == null

        val networkIsRequest
            get() = networkData is Requestdata

        val networkIsResponse
            get() = networkData is Responsedata

        /**
         * Parameter data (trimmed) for 'tape', else null
         */
        val expectedTapeName
            get() = params["tape"]?.trim()
                ?.let { if (it.isBlank()) null else it }

        /**
         * Expected tape name, or a generated name (optional [default])
         */
        fun hardTapeName(default: String = RandomHost().value.toString()) =
            expectedTapeName ?: default

        /**
         * Parameter data (trimmed) for 'chapter', else null
         */
        val expectedChapName
            get() = params["chapter"]?.trim()
                ?.let { if (it.isBlank()) null else it }

        /**
         * Expected chapter name, or a generated name (optional [default])
         */
        fun hardChapName(default: String = RandomHost().valueAsUUID) =
            expectedChapName ?: default

        val expectedNetworkType
            get() = params["network"]?.trim().orEmpty()

        /**
         * Params passed in a tape name, but no tape was found by that name
         */
        val loadTape_Failed
            get() = newTape && expectedTapeName != null

        /**
         * Params passed in a tape name, but no tape was found by that name
         */
        val loadChap_Failed
            get() = newChapter && expectedChapName != null

        fun hrefMake(
            tape: String? = null,
            chapter: String? = null,
            network: String? = null
        ): String {
            val builder = StringBuilder().append("edit?")
            if (tape != null)
                builder.append("tape=%s".format(tape))
            if (chapter != null)
                builder.append("&chapter=%s".format(chapter))
            if (network != null)
                builder.append("&network=%s".format(network))
            return builder.toString()
        }

        fun hrefEdit(
            hTape: String? = null,
            hChapter: String? = null,
            hNetwork: String? = null
        ): String {
            val builder = StringBuilder().append("edit?")
            if (hTape != null || !expectedTapeName.isNullOrEmpty())
                builder.append("tape=%s".format(hTape ?: hardTapeName()))
            else return builder.toString()

            if (hChapter != null || !expectedChapName.isNullOrEmpty())
                builder.append("&chapter=%s".format(hChapter ?: hardChapName()))
            else return builder.toString()

            if (hNetwork != null || expectedNetworkType.isNotBlank())
                builder.append("&network=%s".format(hNetwork ?: expectedNetworkType))
            return builder.toString()
        }

        init {
            tape = tapeCatalog.tapes
                .firstOrNull { it.name == params["tape"] }
            chapter = tape?.chapters
                ?.firstOrNull { it.name == params["chapter"] }
            networkData = when (params["network"]) {
                "request" -> chapter?.requestData
                "response" -> chapter?.responseData
                else -> null
            }
        }
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
                        +"Tape"
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
            script {
                unsafe {
                    +"""
                        Array.from(document.getElementsByClassName('navHeader')).forEach(
                            function(item) {
                                var next = item.nextElementSibling;
                                if(next != null && next.classList.contains('subnav-content'))
                                    item.classList.add('caret-down');
                            }
                        );
                """.trimIndent()
                }
            }
        }
        br()
    }
}
