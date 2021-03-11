package kotlinx.html

import kotlinx.tryOrNull

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

/**
 * 'Height' style of this html attribute
 */
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
    get() = tryOrNull { VisibilityFlags.valueOf(styleProxy("visibility", null)) }
        ?: VisibilityFlags.unset
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
