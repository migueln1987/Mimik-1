package networkRouting

import io.ktor.routing.Route

abstract class RoutingContract(val path: String) : EditorModule_b() {
    abstract fun init(route: Route)
}
