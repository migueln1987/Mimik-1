package mimik

import io.kvision.Application
import io.kvision.core.*
import io.kvision.form.select.select
import io.kvision.i18n.DefaultI18nManager
import io.kvision.i18n.I18n
import io.kvision.pace.Pace
import io.kvision.panel.root
import io.kvision.panel.tab
import io.kvision.panel.tabPanel
import io.kvision.panel.vPanel
import io.kvision.routing.Routing
import io.kvision.utils.auto
import io.kvision.utils.perc
import io.kvision.utils.px
import mimik.tabs.*

class Showcase : Application() {
    init {
        Routing.init()
        Pace.init()
        io.kvision.require("css/showcase.css")
        io.kvision.require("react-awesome-button/dist/themes/theme-blue.css")
        io.kvision.require("moment/locale/pl")
//        if (!(I18n.language in listOf("en", "pl"))) {
            I18n.language = "en"
//        }
    }

    override fun start() {

//        I18n.manager =
//            DefaultI18nManager(
//                mapOf(
//                    "en" to io.kvision.require("i18n/messages-en.json")
//                )
//            )
        root("kvision_test") {
            vPanel {
                width = 100.perc
                tabPanel(scrollableTabs = true) {
                    width = 80.perc
                    margin = 20.px
                    marginLeft = auto
                    marginRight = auto
                    padding = 20.px
                    overflow = Overflow.HIDDEN
                    border = Border(2.px, BorderStyle.SOLID, Color.name(Col.SILVER))
                    tab(I18n.tr("HTML"), "fas fa-bars", route = "/basic") {
                        add(BasicTab())
                    }
                    tab(I18n.tr("Forms"), "fas fa-edit", route = "/forms") {
                        add(FormTab())
                    }
                    tab(I18n.tr("Buttons"), "far fa-check-square", route = "/buttons") {
                        add(ButtonsTab())
                    }
                    tab(I18n.tr("Dropdowns"), "fas fa-arrow-down", route = "/dropdowns") {
                        add(DropDownTab())
                    }
                    tab(I18n.tr("Containers"), "fas fa-database", route = "/containers") {
                        add(ContainersTab())
                    }
                    tab(I18n.tr("Layouts"), "fas fa-th-list", route = "/layouts") {
                        add(LayoutsTab())
                    }
                    tab(I18n.tr("Windows"), "fas fa-window-maximize", route = "/windows") {
                        add(ModalsTab())
                    }
                    tab(I18n.tr("Data binding"), "fas fa-retweet", route = "/data") {
                        add(DataTab())
                    }
                    tab(I18n.tr("Drag & Drop"), "fas fa-arrows-alt", route = "/dragdrop") {
                        add(DragDropTab())
                    }
                    tab(I18n.tr("Charts"), "far fa-chart-bar", route = "/charts") {
                        add(ChartTab())
                    }
                    tab(I18n.tr("Tables"), "fas fa-table", route = "/tabulator") {
                        add(TabulatorTab())
                    }
                    tab(I18n.tr("RESTful"), "fas fa-plug", route = "/restful") {
                        add(RestTab())
                    }
                }
                select(listOf("en" to I18n.tr("English")), I18n.language) {
                    width = 300.px
                    marginLeft = auto
                    marginRight = auto
                    onEvent {
                        change = {
                            I18n.language = self.value ?: "en"
                        }
                    }
                }
            }
        }
    }
}