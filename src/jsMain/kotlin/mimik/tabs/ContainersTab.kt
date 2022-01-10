package mimik.tabs

import io.kvision.core.Background
import io.kvision.core.Col
import io.kvision.core.Color
import io.kvision.core.Container
import io.kvision.dropdown.dropDown
import io.kvision.html.div
import io.kvision.html.h4
import io.kvision.i18n.I18n
import io.kvision.panel.*
import io.kvision.utils.px

class ContainersTab : SimplePanel() {
    init {
        this.marginTop = 10.px
        vPanel(spacing = 5) {
            addStackPanel()
            addTabPanel()
            addTabPanelLeft()
            addTabPanelRight()
            addVerticalSplitPanel()
            addHorizontalSplitPanel()
        }
    }

    private fun Container.addStackPanel() {
        h4(I18n.tr("Stack panel"))
        stackPanel {
            route("/containers/blue") {
                div("&nbsp;", rich = true) {
                    background = Background(Color.name(Col.BLUE))
                    height = 40.px
                }
            }
            route("/containers/green") {
                div("&nbsp;", rich = true) {
                    background = Background(Color.name(Col.GREEN))
                    height = 40.px
                }
            }
        }
        dropDown(
            I18n.tr("Activate panel from the stack"), listOf(
                I18n.tr("Blue panel") to "#!/containers/blue",
                I18n.tr("Green panel") to "#!/containers/green"
            )
        )
    }

    private fun Container.addTabPanel() {
        h4(I18n.tr("Tab panel with draggable tabs"))
        tabPanel(draggableTabs = true) {
            tab(I18n.tr("Blue panel")) {
                div("&nbsp;", rich = true) {
                    background = Background(Color.name(Col.BLUE))
                    height = 40.px
                }
            }
            tab(I18n.tr("Green panel")) {
                div("&nbsp;", rich = true) {
                    background = Background(Color.name(Col.GREEN))
                    height = 40.px
                }
            }
            tab(I18n.tr("Red panel (closable)"), closable = true) {
                div("&nbsp;", rich = true) {
                    background = Background(Color.name(Col.RED))
                    height = 40.px
                }
            }
        }
    }

    private fun Container.addTabPanelLeft() {
        h4(I18n.tr("Tab panel with tabs on the left"))
        tabPanel(TabPosition.LEFT, SideTabSize.SIZE_1) {
            tab(I18n.tr("Blue panel")) {
                div("&nbsp;", rich = true) {
                    background = Background(Color.name(Col.BLUE))
                    height = 140.px
                }
            }
            tab(I18n.tr("Green panel")) {
                div("&nbsp;", rich = true) {
                    background = Background(Color.name(Col.GREEN))
                    height = 140.px
                }
            }
        }
    }

    private fun Container.addTabPanelRight() {
        h4(I18n.tr("Tab panel with tabs on the right"))
        tabPanel(TabPosition.RIGHT, SideTabSize.SIZE_1) {
            tab(I18n.tr("Blue panel")) {
                div("&nbsp;", rich = true) {
                    background = Background(Color.name(Col.BLUE))
                    height = 140.px
                }
            }
            tab(I18n.tr("Green panel")) {
                div("&nbsp;", rich = true) {
                    background = Background(Color.name(Col.GREEN))
                    height = 140.px
                }
            }
        }
    }

    private fun Container.addVerticalSplitPanel() {
        h4(I18n.tr("Vertical split panel"))
        splitPanel {
            div("&nbsp;", rich = true) {
                background = Background(Color.name(Col.BLUE))
                height = 200.px
            }
            div("&nbsp;", rich = true) {
                background = Background(Color.name(Col.GREEN))
                height = 200.px
            }
        }
    }

    private fun Container.addHorizontalSplitPanel() {
        h4(I18n.tr("Horizontal split panel"))
        splitPanel(direction = Direction.HORIZONTAL) {
            height = 220.px
            div("&nbsp;", rich = true) {
                background = Background(Color.name(Col.BLUE))
                height = 100.px
            }
            div("&nbsp;", rich = true) {
                background = Background(Color.name(Col.GREEN))
                height = 100.px
            }
        }
    }
}
