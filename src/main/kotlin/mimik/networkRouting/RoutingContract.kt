package mimik.networkRouting

import io.ktor.routing.Route
import mimik.networkRouting.GUIPages.EditorModule

abstract class RoutingContract(val path: String) : EditorModule() {
    abstract fun init(route: Route)
}
