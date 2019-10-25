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

    if (this is FlowContent)
        div(classes = "infoText") {
            divArgs.invoke(this)
            displayLines.eachHasNext({ +it }, { br() })
        }
    else
        br {
            div(classes = "infoText") {
                divArgs.invoke(this)
                displayLines.eachHasNext({ +it }, { br() })
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
    val splitLines = (R.getProperty(textProperty) ?: textProperty)
        .split('\n')

    div(classes = "tooltip") {
        splitLines.eachHasNext({ +it }, { br() })
        toolTip(infoProperty, position)
    }
}

fun FlowContent.toolTip(
    property: String,
    position: TooltipPositions = TooltipPositions.Top
) {
    val spanClasses = "tooltiptext ${position.value}"
    val splitLines = (R.getProperty(property) ?: property)
        .split('\n')

    div(classes = spanClasses) {
        splitLines.eachHasNext({ +it }, { br() })
    }
}
