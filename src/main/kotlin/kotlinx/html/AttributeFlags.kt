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

class PaddingConfigs(val attributeGroup: CommonAttributeGroupFacade) {
    /**
     * Padding which applies to all the sides
     *
     * @param value Size in px
     */
    var all: String
        get() = attributeGroup.styleProxy("padding", null)
        set(value) {
            attributeGroup.styleProxy("padding", value)
        }

    /**
     * Padding which applies to the top and bottom sides
     *
     * Note: this is a helper property, "get" value uses the "top" value
     *
     * @param value Size in px
     */
    var vertical: String
        get() = attributeGroup.styleProxy("padding-top", null)
        set(value) {
            attributeGroup.styleProxy("padding-top", value)
            attributeGroup.styleProxy("padding-bottom", value)
        }

    /**
     * Padding which applies to the left and right sides
     *
     * Note: this is a helper property, "get" value uses the "left" value
     *
     * @param value Size in px
     */
    var horizontal: String
        get() = attributeGroup.styleProxy("padding-left", null)
        set(value) {
            attributeGroup.styleProxy("padding-left", value)
            attributeGroup.styleProxy("padding-right", value)
        }

    /**
     * Padding which applies to the left sidee
     *
     * @param value Size in px
     */
    var left: String
        get() = attributeGroup.styleProxy("padding-left", null)
        set(value) {
            attributeGroup.styleProxy("padding-left", value)
        }

    /**
     * Padding which applies to the right side
     *
     * @param value Size in px
     */
    var right: String
        get() = attributeGroup.styleProxy("padding-right", null)
        set(value) {
            attributeGroup.styleProxy("padding-right", value)
        }

    /**
     * Padding which applies to the top side
     *
     * @param value Size in px
     */
    var top: String
        get() = attributeGroup.styleProxy("padding-top", null)
        set(value) {
            attributeGroup.styleProxy("padding-top", value)
        }

    /**
     * Padding which applies to the bottom side
     *
     * @param value Size in px
     */
    var bottom: String
        get() = attributeGroup.styleProxy("padding-bottom", null)
        set(value) {
            attributeGroup.styleProxy("padding-bottom", value)
        }
}

/**
 * Accessor to this element's padding attributes
 */
fun CommonAttributeGroupFacade.padding(padConfig: PaddingConfigs.() -> Unit) =
    padConfig.invoke(PaddingConfigs(this))

class BackgroundConfigs(private val attributeGroup: CommonAttributeGroupFacade) {
    var color: String
        get() = attributeGroup.styleProxy("background-color", null)
        set(value) {
            attributeGroup.styleProxy("background-color", value)
        }
}

fun CommonAttributeGroupFacade.background(backgroundConfig: BackgroundConfigs.() -> Unit) =
    backgroundConfig.invoke(BackgroundConfigs(this))

class BorderConfigs(private val attributeGroup: CommonAttributeGroupFacade) {

    /**
     * Raw 'border' attribute field
     */
    var raw: String
        get() = attributeGroup.styleProxy("border", null)
        set(value) {
            attributeGroup.styleProxy("border", value)
        }

    fun all(config: SubConfigs.() -> Unit) =
        config.invoke(SubConfigs())

    fun left(config: SubConfigs.() -> Unit) =
        config.invoke(SubConfigs("left"))

    fun right(config: SubConfigs.() -> Unit) =
        config.invoke(SubConfigs("right"))

    fun top(config: SubConfigs.() -> Unit) =
        config.invoke(SubConfigs("top"))

    fun bottom(config: SubConfigs.() -> Unit) =
        config.invoke(SubConfigs("bottom"))

    inner class SubConfigs(private val type: String? = null) {
        private val edgeType: String
            get() = type?.let { "-$it" }.orEmpty()

        var color: String
            get() = attributeGroup.styleProxy("border$edgeType-color", null)
            set(value) {
                attributeGroup.styleProxy("border$edgeType-color", value)
            }

        var style: String
            get() = attributeGroup.styleProxy("border$edgeType-style", null)
            set(value) {
                attributeGroup.styleProxy("border$edgeType-style", value)
            }

        var width: String
            get() = attributeGroup.styleProxy("border$edgeType-width", null)
            set(value) {
                attributeGroup.styleProxy("border$edgeType-width", value)
            }

        init {
            color = "inherit"
            style = "solid"
            width = "1px"
        }
    }
}

/**
 * Style of border around this element
 */
fun CommonAttributeGroupFacade.border(borderConfig: BorderConfigs.() -> Unit) =
    borderConfig.invoke(BorderConfigs(this))

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
                style = when (grab) {
                    null -> style + "$key: $value".ensureSuffix(";")
                    else -> style.replaceRange(grab.range, value)
                }
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
