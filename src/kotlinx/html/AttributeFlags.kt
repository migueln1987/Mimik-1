package kotlinx.html

import helpers.ensureSuffix
import helpers.isThrow
import helpers.tryOrNull

/**
 * Appends the data in [values] to the current [style].
 *
 * - Items are added in "key: value" format
 * - If an item in [values] does not end with ";", one will be added
 */
fun CommonAttributeGroupFacade.appendStyles(vararg values: String) {
    val builder = StringBuilder()
    values.forEach {
        builder.append(it.trim().ensureSuffix(";"))
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

/**
 * 'Width' style of this html attribute
 */
var CommonAttributeGroupFacade.width: String
    get() = styleProxy("width", null)
    set(value) {
        styleProxy("width", value)
    }

var CommonAttributeGroupFacade.height: String
    get() = styleProxy("height", null)
    set(value) {
        styleProxy("height", value)
    }

enum class VisibilityFlags {
    collapse,
    hidden,
    inherit,
    initial,
    unset,
    visible
}

var CommonAttributeGroupFacade.visibility: VisibilityFlags
    get() {
        return tryOrNull {
            VisibilityFlags.valueOf(styleProxy("visibility", null))
        } ?: VisibilityFlags.unset
    }
    set(value) {
        styleProxy("visibility", value.name)
    }

// https://www.w3schools.com/cssref/pr_class_display.asp
enum class DisplayFlags {
    inline, block, contents, flex, grid,
    inline_block, inline_flex, inline_grid, inline_table,
    list_item, run_in,
    table, table_caption, table_column_group,
    table_header_group, table_footer_group,
    table_row_group,
    table_cell, table_column, table_row,
    none, inital, inherit;

    val value: String
        get() = name.replace("_", "-")

    companion object {
        fun toValue(item: String): DisplayFlags {
            return valueOf(item.replace("-", "_"))
        }
    }
}

var CommonAttributeGroupFacade.display: DisplayFlags
    get() = tryOrNull { DisplayFlags.toValue(styleProxy("display", null)) }
        ?: DisplayFlags.inline
    set(value) {
        styleProxy("display", value.value)
    }

/**
 * Lambda to Get/Set a property in the styles.
 *
 * - Get; second param as `null`
 * - Set; non-null second value
 */
val styleProxy: CommonAttributeGroupFacade.(String, String?) -> String = { key, value ->
    when (value) {
        null -> when {
            isThrow { style } -> ""
            else -> styleRegex(key).find(style)?.value ?: ""
        }
        else -> when {
            isThrow { style } -> "$key: $value".ensureSuffix(";").also { style = it }
            else -> {
                val grab = styleRegex(key).find(style)?.groups?.get(1)
                style = if (grab == null)
                    style + "$key: $value".ensureSuffix(";")
                else
                    style.replaceRange(grab.range, value)
                style
            }
        }
    }
}

/**
 * Creates a regex to find the requested key:value
 */
val styleRegex: (String) -> Regex = { "(?:^|[ ;])$it: *(.+?);".toRegex() }

fun CommonAttributeGroupFacade.setMinMaxSizes(
    widthMin: String,
    widthMax: String,
    heightMin: String,
    heightMax: String
) {
    appendStyles(
        "min-width: $widthMin", "max-width: $widthMax",
        "min-height: $heightMin", "max-height: $heightMax"
    )
}
