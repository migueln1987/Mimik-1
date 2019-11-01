package helpers

import R
import io.ktor.http.Parameters
import io.ktor.util.toMap
import kotlinx.html.*
import okhttp3.Headers
import kotlin.math.abs
import kotlin.random.Random

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
    script { unsafe { +"setupTogggButtonTarget('$target');" } }

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
    script {
        unsafe {
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
        toMultimap().asSequence()
            .filter { it.key != null && it.value != null }
            .flatMap { kv ->
                kv.value.asSequence()
                    .filter { it != null }
                    .map { kv.key!! to it!! }
            }
    }
    textAreaBuilder(pairs, config)
}

fun FlowContent.textAreaBuilder(data: Sequence<Pair<String, String>>?, config: TEXTAREA.() -> Unit = {}) {
    textArea {
        config.invoke(this)
        onKeyPress = "keypressNewlineEnter(this);"
        val builder = StringBuilder()
        data?.forEach {
            builder.appendln("${it.first} : ${it.second}")
        }
        +builder.toString()
    }
}

/**
 * Appends the data in [values] to the current [style].
 *
 * If an item in [values] does not end with ";", one will be added
 */
fun CommonAttributeGroupFacade.appendStyles(vararg values: String) {
    val builder = StringBuilder()
    values.forEach {
        builder.append(it.ensureSuffix(";"))
    }

    if (isThrow { style })
        style = builder.toString()
    else
        style += builder.toString()
}

/**
 * Appends the data in [values] to the current object's [classes]
 */
fun CommonAttributeGroupFacade.appendClass(vararg values: String) {
    classes = classes.toMutableSet().apply { addAll(values) }
}

val CommonAttributeGroupFacade.disabledBG: Unit
    get() = appendStyles("background-color: #E0E0E0")

val CommonAttributeGroupFacade.readonlyBG: Unit
    get() = appendStyles("background-color: #F0F0F0")

val CommonAttributeGroupFacade.disabledText: Unit
    get() = appendStyles("color: darkgray")

/**
 * Sets the contents to break per word
 */
val CommonAttributeGroupFacade.wordBreak_word: Unit
    get() = appendStyles("word-break: break-word")

/**
 * Enables the column to be horizontally resizable
 */
val TH.resizableCol
    get() = appendStyles("resize: horizontal", "overflow: auto")

val DIV.inlineDiv: Unit
    get() = appendStyles("display: inline")
