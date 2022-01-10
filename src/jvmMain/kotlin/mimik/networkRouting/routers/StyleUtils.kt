package mimik.networkRouting.routers

import io.ktor.*
import io.ktor.routing.*
import kotlinx.css.*
import kotlinx.css.Float
import kotlinx.html.*
import com.inet.lib.less.less.toCSS
import kotlinx.css.properties.*
import java.util.*
import kotlin.system.measureTimeMillis

// http://w3.org/TR/CSS21/selector.html#pattern-matching
enum class ExportStyles {
    Common, Breadcrumb, Collapsible,
    Tooltip, Callout, Sortable;

    companion object {
        val isExported = mutableSetOf<ExportStyles>()
    }

    override fun toString(): String = "${name.lowercase()}.css"

    val asset: String get() = "../assets/css/$this"
}

abstract class ExportStyle(private val exportName: ExportStyles) {
    abstract val data: CssBuilder
    val content by lazy {
        var result: CssContent
        measureTimeMillis {
            result = CssContent(data.toCSS())
        }.also { println("CSS $exportName: $it ms") }
        result
    }

    /**
     * Exposes the [data] to [filename] as a `Text/CSS`
     */
    fun expose(route: Route) {
        ExportStyles.isExported.add(exportName)
        route.respondCss(exportName.toString(), content)
    }
//    fun expose(route: Route) = route.respondCss_b(exportName.toString()) { content }
}

fun Route.exposeDeclaredStyles() {
    CommonStyles.expose(this)
    BreadcrumbStyle.expose(this)
    CollapsibleDiv.expose(this)
    TooltipStyle.expose(this)
    CalloutStyle.expose(this)
    Sortable.expose(this)
}

object CommonStyles : ExportStyle(ExportStyles.Common) {
    override val data
        get() = CssBuilder {
            rule("table") {
                fontSize = 1.em
                fontFamily = "Arial"
                border(1.px, BorderStyle.solid, Color.black)
                width = 100.pct
            }

            rule("button") {
                cursor = Cursor.pointer
                disabled { cursor = Cursor.default }
            }

            ruleClass("inputButton") {
                border(1.px, BorderStyle.solid, Color("#ccc"), 4.px)
            }

            rule("th") {
                backgroundColor = Color("#ccc")
                width = LinearDimension.auto
            }

            rule("td") {
                backgroundColor = Color("#eee")
            }

            rule("th", "td") {
                textAlign = TextAlign.left
                padding(0.4.em)
            }

            ruleClass("btn_50wide") { width = 50.pct }

            ruleClass("tb_25wide") { width = 25.pct }

            ruleClass("center") { textAlign = TextAlign.center }

            ruleClass("infoText") {
                fontSize = 14.px
                color = Color("#555")
            }

            ruleClass("opacity50") { opacity = 0.5 }

            ruleClass("radioDiv") {
                width = 40.pct
                padding(6.px)
            }

            ruleClass("hoverExpand") {
                hover {
//                    width = LinearDimension.fillAvailable
                    width = LinearDimension("-webkit-fill-available")
                }
            }
        }
}

object BreadcrumbStyle : ExportStyle(ExportStyles.Breadcrumb) {
    override val data
        get() = CssBuilder {
            ruleClass("breadcrumb") {
                padding = 10.px.toString()
                position = Position.sticky
                top = 10.px
                width = 100.pct - 22.px
                backgroundColor = Color("#eee")
                overflow = Overflow.hidden
                border(1.px, BorderStyle.solid, Color.black, 5.px)
                zIndex = 1

                div {
                    fontSize = 14.px
                }

                classDescendant("subnav") {
//                    float = Float.left
//                    overflow = Overflow.hidden

                    // subnav:before
                    classAdjacentSibling("subnav") {
                        before { content = "/".quoted }
                    }
                }
            }

            ruleClass("subnav") {
                float = Float.left
                overflow = Overflow.hidden

                hover {
                    classDescendant("subnav-content") {
                        display = Display.grid
                    }
                }

                classDescendant("navHeader") {
                    fontSize = 16.px
                    borderStyle = BorderStyle.none
                    outline = Outline.none
                    backgroundColor = Color.inherit
                    fontFamily = FontStyle.inherit.value
                    margin = 0.px.toString()
                    padding = 4.px.toString()
                }
            }

            ruleClass("navHeader") {
                color = Color("#0275d8")
                textDecoration = TextDecoration.none

                hover {
                    backgroundColor = Color.darkSlateGray
                    color = Color.white
                    cursor = Cursor.pointer
                    textDecoration(TextDecorationLine.underline)
                }
            }

            ruleClass("subnav-content") {
                position = Position.fixed
                left = 5.em
//                top = 42.px
                width = LinearDimension.auto
//                backgroundColor = Color.slateGray
                backgroundColor = Color.transparent
                zIndex = 1
                lineHeight = 1.em.lh
                maxHeight = 10.5.em
                overflowY = Overflow.auto
                borderTop(12.px, BorderStyle.solid, Color.transparent)
                display = Display.none

                universal {
                    cursor = Cursor.pointer
                    float = Float.left
                    color = Color.white
                    padding = 8.px.toString()
                    paddingRight = 10.em
                    textDecoration = TextDecoration.none
                    display = Display.inlineFlex
                    backgroundColor = Color.slateGray
                    borderBottom(1.px, BorderStyle.solid, Color.initial)

                    hover {
                        backgroundColor = Color.darkSlateGray
                    }
                }
            }

            ruleClass("caret-down") {
                after {
                    content = "\\25be".quoted
                    lineHeight = 1.px.lh
                    fontStyle = FontStyle.normal
                }
            }
        }
}

object CollapsibleDiv : ExportStyle(ExportStyles.Collapsible) {
    override val data
        get() = CssBuilder {
            // Button style that is used to open and close the collapsible content
            ruleClass("collapsible") {
                backgroundColor = Color("#999")
                color = Color.white
                cursor = Cursor.pointer
                padding(8.px, 10.px)
                marginBottom = 4.px
                width = 100.pct
                textAlign = TextAlign.left
                fontSize = 15.px

                after {
                    content = "\\002B".quoted
                    color = Color.white
                    fontWeight = FontWeight.bold
                    float = Float.right
                    marginLeft = 5.px
                }

                hover {
                    backgroundColor = Color("#888")
                }
            }

            ruleClass("active") {
                // background color to the button if it is clicked on (add the .active class with JS), and when you move the mouse over it (hover)
                backgroundColor = Color("#888")

                after {
                    content = "\\2212".quoted
                }
            }

            // Style the collapsible content. Note: "hidden" by default
            ruleClass("hideableContent") {
                padding(6.px)
                maxHeight = 0.px
                display = Display.none
                overflow = Overflow.hidden
                width = LinearDimension("-webkit-fill-available")
                backgroundColor = Color("#f4f4f4")
                transition("max-height", 0.4.s, Timing.easeOut)
            }
        }
}

object TooltipStyle : ExportStyle(ExportStyles.Tooltip) {
    override val data
        get() = CssBuilder {
            ruleClass("tooltip") {
                position = Position.relative
                display = Display.inlineBlock
                borderBottom(1.px, BorderStyle.dotted, Color("#ccc"))
                color = Color("#006080")
                cursor = Cursor.default

                ruleClass("tooltiptext") {
                    visibility = Visibility.hidden
                    position = Position.absolute
                    width = 30.em
                    maxWidth = LinearDimension.maxContent
                    backgroundColor = Color("#555")
                    color = Color("#fff")
                    textAlign = TextAlign.center
                    padding(0.5.em)
                    borderRadius = 6.px
                    zIndex = 1
                    opacity = 0
                    transition("opacity", 0.3.s)
                }

                hover {
                    ruleClass("tooltiptext") {
                        visibility = Visibility.visible
                        opacity = 1
                    }
                }
            }

            ruleClass("tooltip-right") {
                top = (-5).px
                left = 125.pct

                after {
                    content = "".quoted
                    position = Position.absolute
                    top = 50.pct
                    right = 100.pct
                    marginTop = (-5).px
                    border(5.px, BorderStyle.solid, Color.transparent)
                    borderRightColor = Color("#555")
                }
            }

            ruleClass("tooltip-bottom") {
                top = 135.pct
                left = 50.pct
                marginLeft = (-60).px

                after {
                    content = "".quoted
                    position = Position.absolute
                    bottom = 100.pct
                    left = 50.pct
                    marginLeft = (-5).pct
                    border(5.px, BorderStyle.solid, Color.transparent)
                    borderBottomColor = Color("#555")
                }
            }

            ruleClass("tooltip-top") {
                bottom = 125.pct
                left = 50.pct
                marginLeft = (-60).px

                after {
                    content = "".quoted
                    position = Position.absolute
                    top = 100.pct
                    left = 50.pct
                    marginLeft = (-5).pct
                    border(5.px, BorderStyle.solid, Color.transparent)
                    borderTopColor = Color("#555")
                }
            }

            ruleClass("tooltip-left") {
                top = (-5).pct
                bottom = LinearDimension.auto
                right = 128.pct

                after {
                    content = "".quoted
                    position = Position.absolute
                    top = 50.pct
                    left = 100.pct
                    marginTop = (-5).pct
                    border(5.px, BorderStyle.solid, Color.transparent)
                    borderLeftColor = Color("#555")
                }
            }
        }
}

object CalloutStyle : ExportStyle(ExportStyles.Callout) {
    override val data
        get() = CssBuilder {
            ruleClass("callout") {
                position = Position.fixed
                maxWidth = 300.px
                margin(horizontal = 40.pct)
                width = 30.pct
                top = 0.px
                zIndex = 20
                transition("top", 0.2.s)
            }

            ruleClass("callout-header") {
                padding(vertical = 2.px) // horizontal = 20.px
                paddingRight = 30.px
                paddingLeft = 10.px
                backgroundColor = Color("#555")
                fontSize = 30.px
                color = Color.white
            }

            ruleClass("callout-container") {
                padding(6.px)
                backgroundColor = Color("#ccc")
                color = Color.black
            }

            ruleClass("closebtn") {
                position = Position.absolute
                top = 0.px
                right = 6.px
                color = Color.white
                fontSize = 30.px
                cursor = Cursor.pointer

                hover {
                    color = Color.lightGray
                }
            }
        }
}

object Sortable : ExportStyle(ExportStyles.Sortable) {
    override val data
        get() = CssBuilder {
            ruleClass("sjs_ghost") {
                opacity = 0.5
                backgroundColor = Color("#C8EBFB")
            }

            ruleClass("sjs_group") {
                paddingLeft = 0.px
                marginBottom = 0.px
            }

            ruleClass("sjs_group-item") {
                position = Position.relative
                display = Display.block
                padding(0.75.rem, 0.5.rem)
                marginBottom = (-1).px
                backgroundColor = Color("#fff")
                border(1.px, BorderStyle.solid, rgba(0, 0, 0, 0.125))
            }

            ruleClass("nested-sortable", "nested-1", "nested-2", "nested-3") {
                margin(vertical = 4.px)
            }

            ruleClass("nested-1") { backgroundColor = Color("#e6e6e6") }
            ruleClass("nested-2") { backgroundColor = Color("#cccccc") }
            ruleClass("nested-3") { backgroundColor = Color("#b3b3b3") }

            ruleClass("sjs_col") {
                flexBasis = FlexBasis.zero
                flexGrow = 1.0
                maxWidth = 100.pct
            }

            ruleClass("sjs_row") {
                display = Display.flex
                flexWrap = FlexWrap.wrap
                margin(horizontal = (-15).px)
            }

            ruleClass("sjs_handle") {
                display = Display.inline
                cursor = Cursor.rowResize
            }

            ruleClass("sjs_noDrag")

            ruleClass("inline") {
                display = Display.inline
            }
        }
}

object StyleUtils {
    @Deprecated("Migrate to css files", level = DeprecationLevel.ERROR)
    fun FlowOrMetaDataContent.setupStyle() {
        unsafeStyle {
            +arrayOf(
                CommonStyles.data,
                BreadcrumbStyle.data,
                CollapsibleDiv.data,
                TooltipStyle.data,
                CalloutStyle.data
            ).joinToString(separator = "\n")
        }
    }
}
