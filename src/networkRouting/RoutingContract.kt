package networkRouting

import io.ktor.routing.Routing

abstract class RoutingContract(val path: String) {
    abstract fun init(route: Routing)
}
