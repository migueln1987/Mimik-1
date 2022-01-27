package mimik

import io.kvision.Application
import io.kvision.core.*
import io.kvision.html.*
import io.kvision.panel.*
import io.kvision.routing.Routing
import io.kvision.utils.px
import mimik.dataObjects.Size
import mimik.kvision_tabs.BasicTab


class MimikGUI : Application() {
    init {
        println("mimik gui init")
        Routing.init()
    }

    private fun Container.customDiv(value: String, size: Size): Tag {
        return div(value).apply {
//            paddingTop = ((size.height / 2) - 10).px
            align = Align.CENTER
            background = Background(Color.name(Col.GREEN))
            width = size.width.px
            height = size.height.px
        }
    }

    override fun start() {
        root("gui") {
//            +"Mimik GUI - placeholder"
            vPanel {
                hPanel {
                    div {
                        addCssClass("col-sm-2")
                        align = Align.CENTER
                        background = Background(Color.name(Col.GREEN))
                        div { tr("Mimik") }
                        div { tr("v2.x_2201.06") }
                    }
                }

                tabPanel(
                    TabPosition.LEFT, SideTabSize.SIZE_2
                ) {
                    val tabPanel = this

                    val hideText = Style {
                        println("Adding style: $cssClassName")
                        textIndent = (-9999).px
                        style("i.fas") {
                            display = Display.INLINEBLOCK
                            position = Position.ABSOLUTE
                            textIndent = 9999.px
                        }
                        style("i") {
                            background = Background(Color.name(Col.GREEN))
                        }
                    }

                    tab("Connections", "fas fa-plug", route = "/pipes") {
                        link.labelFirst = true
                        add(BasicTab())
                    }
                    tab("Containers", "fas fa-building", route = "/boxes") {
                        link.labelFirst = true
                        add(BasicTab())
                    }
                    tab("Tape Viewer", "far fa-folder", route = "/tapes") {
                        link.labelFirst = true
                        add(BasicTab())
                    }
                    tab("Logs", "fas fa-book", route = "/logs") {
                        link.labelFirst = true
                        add(BasicTab())
                    }
                    tab("Help", "fas fa-info", route = "/help") {
                        link.labelFirst = true
                        add(BasicTab())
                    }
                    tab("Settings", "fas fa-cog", route = "/config") {
                        link.labelFirst = true
                        add(BasicTab())
                    }

                    tab("Collapse", "fas fa-chevron-left") {
                        removeEventListeners()
//                        link.labelFirst = true
                        setEventListener<Tab> {
                            click = {
                                println("tabs (changing): ${getTabs().size}")
                                println("self.label: ${self.label}")

                                if (self.label == "Collapse") {
                                    tabPanel.getTabs().forEach { tab ->
                                        println("Updating tab: ${tab.label}")
                                        tab.link.addCssStyle(hideText)
                                        tab.link.refresh()
                                    }

                                    self.label = "Expand"
                                    self.icon = "fas fa-chevron-right"
                                } else {
                                    tabPanel.getTabs().forEach { tab ->
                                        tab.link.removeCssStyle(hideText)
                                    }
                                    self.label = "Collapse"
                                    self.icon = "fas fa-chevron-left"
                                }
                                tabPanel.refresh()
                            }
                        }
                    }
                }
            }
        }
    }
}
