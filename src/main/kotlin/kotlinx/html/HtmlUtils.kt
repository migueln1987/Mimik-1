package kotlinx.html

import R
import helpers.*
import io.ktor.http.Parameters
import io.ktor.util.toMap
import okhttp3.Headers
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

fun FlowOrMetaDataContent.unsafeStyle(type: String? = null, block: Unsafe.() -> Unit = {}) =
    style(type) { unsafe(block) }

fun FlowOrPhrasingOrMetaDataContent.unsafeScript(
    type: String? = null,
    src: String? = null,
    block: Unsafe.() -> Unit = {}
) = script(type, src) { unsafe { block.invoke(this) } }

/**
 * Creates a line (2 "<[br]>") for each [lines] count
 */
fun FlowOrPhrasingContent.linebreak(lines: Int = 1, classes: String? = null, block: BR.() -> Unit = {}) {
    repeat(lines) {
        br(classes, block)
        br(classes, block)
    }
}

fun FlowOrInteractiveOrPhrasingContent.inputButton(
    formEncType: ButtonFormEncType? = null,
    formMethod: ButtonFormMethod? = null,
    name: String? = null,
    type: ButtonType? = null,
    classes: String? = null,
    block: BUTTON.() -> Unit = {}
) {
    val btnClass = "${classes?.ensureSuffix(" ").orEmpty()}inputButton"
    button(formEncType, formMethod, name, type, btnClass, block)
}

/**
 * Creates a ToggleButton which will toggle the element of [target]
 */
fun FlowOrPhrasingContent.makeToggleButton(
    target: String,
    isExpanded: Boolean = false
) {
    script { unsafe { +"setupToggleButtonTarget('$target');" } }

    button(
        type = ButtonType.button,
        classes = "collapsible".let {
            if (isExpanded)
                "$it active" else it
        }
    ) {
        onClick = "toggleView(this, $target);"
        +"Toggle view"
    }
    br()
}

/**
 * Creates a ToggleButton with a [DIV] below it (as the toggle area).
 *
 * The div will contain all the contents of [element]
 */
fun FlowContent.toggleArea(
    isExpanded: Boolean = false,
    id: String? = null,
    classes: String? = null,
    element: DIV.() -> Unit
) {
    button(type = ButtonType.button, classes = "collapsible") {
        +"Toggle view"
    }

    br()
    div(classes = classes) {
        if (id != null)
            this.id = id
        element.invoke(this)
    }

    val rngName = "toggles_${RandomHost().value}"
    unsafeScript {
        +"var $rngName = setupToggleArea();%s".format(
            if (isExpanded) {
                """
                    waitForElem($rngName[1], function(elem) {
                        toggleView($rngName[0], $rngName[1]);
                        });
                """.trimIndent()
            } else ""
        )
    }
}

/**
 * Adds a br then a div containing formatted info text.
 * If [property] isn't a string property, then it's passed as-is.
 * [formatArgs] are added to the resulting string before displaying.
 */
fun FlowOrPhrasingContent.infoText(
    property: String,
    formatArgs: Any = "",
    divArgs: DIV.() -> Unit = {}
) {
    val displayLines = (R.getProperty(property) ?: property)
        .run {
            @Suppress("UnnecessaryVariable")
            when (val args = formatArgs) {
                is Array<*> -> format(*args)
                is Collection<*> -> format(*args.toTypedArray())
                else -> format(args)
            }
        }
        .split('\n')

    val divConfig: DIV.() -> Unit = {
        divArgs.invoke(this)
        displayLines.eachHasNext({ +it }, { br() })
    }

    if (this is FlowContent)
        div(classes = "infoText", block = divConfig)
    else
        span { div(classes = "infoText", block = divConfig) }
}

@Suppress("unused")
enum class TooltipPositions(val value: String) {
    Top("tooltip-top"),
    Bottom("tooltip-bottom"),
    Left("tooltip-left"),
    Right("tooltip-right")
}

/**
 * Displays [textProperty], with a tooltip hover property that displays the content of [infoProperty]
 */
fun FlowOrPhrasingContent.tooltipText(
    textProperty: String,
    infoProperty: String,
    position: TooltipPositions = TooltipPositions.Top,
    divArgs: DIV.() -> Unit = {}
) {
    val splitLines = (R.getProperty(textProperty) ?: textProperty)
        .split('\n')

    val divConfig: DIV.() -> Unit = {
        divArgs.invoke(this)
        splitLines.eachHasNext({ +it }, { br() })
        toolTip(infoProperty, position)
    }

    if (this is FlowContent)
        div(classes = "tooltip", block = divConfig)
    else
        span { div(classes = "tooltip", block = divConfig) }
}

fun FlowContent.toolTip(
    property: String,
    position: TooltipPositions = TooltipPositions.Top
) {
    val spanClasses = "tooltiptext ${position.value}"
    val splitLines = (R.getProperty(property) ?: property)
        .split('\n')

    div(classes = spanClasses) {
        val thisId = "tooltip_${abs(property.hashCode() + Random.nextInt())}"
        id = thisId
        splitLines.eachHasNext({ +it }, { br() })
        script {
            unsafe {
                +"""
                    $thisId.style.marginLeft = -($thisId.clientWidth / 2) + 'px';
                    if ($thisId.getBoundingClientRect().x < 0)
	                    $thisId.style.marginLeft = -(($thisId.clientWidth / 2) + $thisId.getBoundingClientRect().x) + 'px';
                """.trimIndent()
            }
        }
    }
}

/**
 * [textArea] which is populated by an input of [Parameters]
 */
fun FlowContent.paramTextArea(params: Parameters?, config: TEXTAREA.() -> Unit = {}) {
    val pairs = params?.run {
        toMap().asSequence()
            .flatMap { kv ->
                kv.value.asSequence().map { kv.key to it }
            }
    }

    textAreaBuilder(pairs, config)
}

/**
 * [textArea] which is populated by an input of [Headers]
 */
fun FlowContent.headerTextArea(headers: Headers?, config: TEXTAREA.() -> Unit = {}) {
    val pairs = headers?.run {
        toMultimap(true).asSequence()
            .flatMap { kv ->
                kv.value.asSequence()
                    .map { kv.key to it }
            }
    }
    textAreaBuilder(pairs, config)
}

fun FlowContent.textAreaBuilder(data: Sequence<Pair<String, String>>?, config: TEXTAREA.() -> Unit = {}) {
    textArea {
        config.invoke(this)
        onKeyPress = "keypressNewlineEnter(this);"
        val builder = StringBuilder()
        var maxWd = 0
        var lines = 0
        data?.forEach {
            lines++
            "${it.first} : ${it.second}".also { str ->
                if (str.length > maxWd)
                    maxWd = str.length
                builder.appendln(str)
            }
        }

        width = "${maxWd - 6}em"
        height = "${lines + 2}em"
        +builder.toString()
    }
}

fun FlowContent.calloutWindow(
    calloutID: String = "",
    headerDiv: DIV.() -> Unit = { +"" },
    container: DIV.() -> Unit
) {
    div(classes = "callout") {
        id = calloutID
        div(classes = "callout-header") { headerDiv.invoke(this) }
        span(classes = "closebtn") {
            onClick = "parentElement.style.top = -parentElement.clientHeight + 'px';"
            +"x"
        }
        div(classes = "callout-container") { container.invoke(this) }
    }
}

/**
 * type: (file (default), tape, chapter)
 * name: (chapter name); file/tape is from filename
 * age: Date (in Long)
 */
fun FlowContent.refreshWatchWindow(
    file: File?,
    extraAppend: () -> List<Pair<String, String>>? = { null }
) {
    if (file == null) return
    val fileID = abs(file.hashCode())
    val extras = extraAppend.invoke() ?: listOf()
    val watchType = extras.firstOrNull { it.first == "type" }?.second ?: "file"
    val watchName = extras.firstOrNull { it.first == "name" }?.second ?: file.nameWithoutExtension
    val watchAge = extras.firstOrNull { it.first == "age" && it.second.isNotBlank() }?.second
        ?: file.lastModified().toString()
    val refreshNotifID = "refreshNotif_$fileID"

    div(classes = "callout") {
        id = "refreshWatch_$fileID"
        appendStyles("z-index: 18", "opacity: 0.4")
        onMouseOver = "this.style.opacity = 1;"
        onMouseOut = "this.style.opacity = 0.4;"

        div(classes = "callout-container") {
            checkBoxInput {
                id = "refreshOn_$fileID"
                checked = false
                onChange = "if(checked) runWatcher();"
            }
            +" Observing $watchType: $watchName"
            br()
            div {
                style = "text-align: center;"
                id = "refreshBlink_$fileID"
                +"."
            }
        }
    }

    calloutWindow(refreshNotifID, { +"New Data" }) {
        a {
            href = "javascript:window.location.reload(true)"
            +"Refresh page"
        }
    }

    script {
        unsafe {
            val ageID = "lastAge_$fileID"
            val appender = StringBuilder().apply {
                extras.forEach { appendln("formData.append('%s', '%s');".format(it.first, it.second)) }

                if (!this.contains("append('age'"))
                    appendln("formData.append('age', '%s');".format(watchAge))
                if (!this.contains("append('type'"))
                    appendln("formData.append('type', '%s');".format(watchType))
            }

            +"""
                $refreshNotifID.style.top = -$refreshNotifID.clientHeight + 'px';
                
                var observeCnt_$fileID = 0;
                function runWatcher() {
                    var ${ageID}_watcher = setInterval(function() {
                        refreshBlink_$fileID.innerText += "..";
                        observeCnt_$fileID++;
                        if (observeCnt_$fileID == 4) {
                            refreshBlink_$fileID.innerText = ".";
                            observeCnt_$fileID = 0;
                        }
                        
                        if (!refreshOn_$fileID.checked)
                            clearInterval(${ageID}_watcher);
                        
                        const formData = new FormData();
                        formData.append('file', '$file');
                        $appender
                    
                        fetch('../fetch/ageCheck', {
                            method: "POST",
                            body: formData
                        })
                        .then(function(response) {
                            response.json().then((json) => {
                                switch(json['action']) {
                                    case 'Invalid':
                                        refreshOn_$fileID.checked = false;
                                        refreshOn_$fileID.disabled = true;
                                        refreshBlink_$fileID.innerText = json['data'];
                                        clearInterval(${ageID}_watcher);
                                        break;
                                    case 'Refresh':
                                        $refreshNotifID.style.top = "";
                                        refreshOn_$fileID.checked = false;
                                        clearInterval(${ageID}_watcher);
                                        break;
                                }
                             });
                        })
                        .catch(function(error) {
                            refreshOn_$fileID.checked = false;
                            clearInterval(${ageID}_watcher);
                            console.log(error);
                        });
                    }, 1000);
                }
                
                if (refreshOn_$fileID.checked)
                    runWatcher();
            """.trimIndent()
        }
    }
}

/**
 * Groups a section of code, applies nothing to the result html code.
 */
inline fun FlowContent.group(crossinline block: FlowContent.() -> Unit = {}) = block.invoke(this)
