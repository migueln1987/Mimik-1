@file:Suppress("KDocUnresolvedReference", "unused", "MemberVisibilityCanBePrivate")

package kotlinx.html

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
    padConfig(PaddingConfigs(this))

class MarginConfigs(val attributeGroup: CommonAttributeGroupFacade) {
    /**
     * Margin which applies to all the sides
     *
     * @param value Size in px
     */
    var all: String
        get() = attributeGroup.styleProxy("margin", null)
        set(value) {
            attributeGroup.styleProxy("margin", value)
        }

    /**
     * Margin which applies to the top and bottom sides
     *
     * Note: this is a helper property, "get" value uses the "top" value
     *
     * @param value Size in px
     */
    var vertical: String
        get() = attributeGroup.styleProxy("margin-top", null)
        set(value) {
            attributeGroup.styleProxy("margin-top", value)
            attributeGroup.styleProxy("margin-bottom", value)
        }

    /**
     * Margin which applies to the left and right sides
     *
     * Note: this is a helper property, "get" value uses the "left" value
     *
     * @param value Size in px
     */
    var horizontal: String
        get() = attributeGroup.styleProxy("margin-left", null)
        set(value) {
            attributeGroup.styleProxy("margin-left", value)
            attributeGroup.styleProxy("margin-right", value)
        }

    /**
     * Margin which applies to the left sidee
     *
     * @param value Size in px
     */
    var left: String
        get() = attributeGroup.styleProxy("margin-left", null)
        set(value) {
            attributeGroup.styleProxy("margin-left", value)
        }

    /**
     * Margin which applies to the right side
     *
     * @param value Size in px
     */
    var right: String
        get() = attributeGroup.styleProxy("margin-right", null)
        set(value) {
            attributeGroup.styleProxy("margin-right", value)
        }

    /**
     * Margin which applies to the top side
     *
     * @param value Size in px
     */
    var top: String
        get() = attributeGroup.styleProxy("margin-top", null)
        set(value) {
            attributeGroup.styleProxy("margin-top", value)
        }

    /**
     * Margin which applies to the bottom side
     *
     * @param value Size in px
     */
    var bottom: String
        get() = attributeGroup.styleProxy("margin-bottom", null)
        set(value) {
            attributeGroup.styleProxy("margin-bottom", value)
        }
}

/**
 * Accessor to this element's margin attributes
 */
fun CommonAttributeGroupFacade.margin(marginConfig: MarginConfigs.() -> Unit) =
    marginConfig(MarginConfigs(this))

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
        config(SubConfigs())

    fun left(config: SubConfigs.() -> Unit) =
        config(SubConfigs("left"))

    fun right(config: SubConfigs.() -> Unit) =
        config(SubConfigs("right"))

    fun top(config: SubConfigs.() -> Unit) =
        config(SubConfigs("top"))

    fun bottom(config: SubConfigs.() -> Unit) =
        config(SubConfigs("bottom"))

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
    borderConfig(BorderConfigs(this))

fun CommonAttributeGroupFacade.border_3D() {
    border {
        all {
            color = "black"
            width = "2px"
            style = "solid"
        }
        top { width = "1px" }
        left { width = "1px" }
    }
}

fun CommonAttributeGroupFacade.border_i3D(bColor: String = "black") {
    border {
        all {
            color = bColor
            width = "2px"
            style = "solid"
        }
        bottom { width = "1px" }
        right { width = "1px" }
    }
}
