package helpers

import R
import kotlinx.html.*

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
 * Adds a br then a div containing formatted info text.
 * If [property] isn't a string property, then it's passed as-is.
 * [formatArgs] are added to the resulting string before displaying.
 */
fun FlowOrPhrasingContent.infoText(
    property: String,
    formatArgs: Array<Any> = arrayOf(),
    divArgs: (DIV) -> Unit = {}
) {
    val display = (R.getProperty(property) ?: property)
        .format(*formatArgs)

    if (this is FlowContent)
        div(classes = "infoText") {
            divArgs.invoke(this)
            +display
        }
    else
        br {
            div(classes = "infoText") {
                divArgs.invoke(this)
                +display
            }
        }
}

@Suppress("unused")
enum class TooltipPositions(val value: String) {
    Top("tooltip-top"),
    Bottom("tooltip-bottom"),
    Left("tooltip-left"),
    Right("tooltip-right")
}

fun FlowContent.tooltipText(
    textProperty: String,
    infoProperty: String,
    position: TooltipPositions = TooltipPositions.Top
) {
    val tipVal = (R.getProperty(textProperty) ?: textProperty).trim()
    val splitLines = tipVal.split('\n')

    div(classes = "tooltip") {
        splitLines.eachHasNext({ +it.trim() }, { br() })
        toolTip(infoProperty, position)
    }
}

fun FlowContent.toolTip(
    property: String,
    position: TooltipPositions = TooltipPositions.Top
) {
    val display = R.getProperty(property) ?: property
    val spanClasses = "tooltiptext ${position.value}"

    val splitLines = display.split('\n')
    div(classes = spanClasses) {
        splitLines.eachHasNext({ +it.trim() }, { br() })
    }
}
