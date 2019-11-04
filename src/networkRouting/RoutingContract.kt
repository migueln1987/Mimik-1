package networkRouting

import io.ktor.routing.Route
import networkRouting.editorPages.EditorModule

abstract class RoutingContract(val path: String) : EditorModule() {
    abstract fun init(route: Route)
}
