package networkRouting

import io.ktor.routing.Routing
import networkRouting.editorPages.EditorModule

abstract class RoutingContract(val path: String) : EditorModule() {
    abstract fun init(route: Routing)
}
