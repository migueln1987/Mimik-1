package helpers

import R
import kotlinx.html.*
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
    val btnClass = "${classes?.ensureSufix(" ").orEmpty()}inputButton"
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
fun FlowContent.makeToggleArea(
    isExpanded: Boolean = false,
    element: (DIV) -> Unit
) {
    button(
        type = ButtonType.button,
        classes = "collapsible".let {
            if (isExpanded)
                "$it active" else it
        }
    ) { +"Toggle view" }

    br()
    div {
        element.invoke(this)
    }

    script { unsafe { +"setupToggleArea();" } }
}

/**
 * Adds a br then a div containing formatted info text.
 * If [property] isn't a string property, then it's passed as-is.
 * [formatArgs] are added to the resulting string before displaying.
 */
fun FlowOrPhrasingContent.infoText(
    property: String,
    formatArgs: Any = "",
    divArgs: (DIV) -> Unit = {}
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
    divArgs: (DIV) -> Unit = {}
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
