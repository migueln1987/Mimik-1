package mimik

import io.kvision.Application
import io.kvision.panel.*
import io.kvision.routing.Routing
import mimik.kvision_tabs.BasicTab

class MimikGUI : Application() {
    init {
        println("mimik gui init")
        Routing.init()
    }

    override fun start() {
        root("gui") {
//            +"Mimik GUI - placeholder"
            vPanel {
                tabPanel(
                    TabPosition.LEFT
                ) {
                    tab("Connections") { add(BasicTab()) }
                    tab("Containers") { add(BasicTab()) }
                    tab("Tape Viewer") { add(BasicTab()) }
                    tab("Logs") { add(BasicTab()) }
                    tab("Settings") { add(BasicTab()) }
                }
            }
        }
    }
}
