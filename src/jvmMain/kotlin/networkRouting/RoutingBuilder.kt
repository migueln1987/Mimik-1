package networkRouting

import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext

/**
 * Builds a route to match specified [port]
 */
fun Route.port(port: Int, body: Route.() -> Unit): Route {
    val selector = HttpPortRouteSelector(port)
    return createChild(selector).apply(body)
}

/**
 * Evaluates if a route port equals [port]
 * @param port equals [port]
 */
data class HttpPortRouteSelector(val port: Int) : RouteSelector(RouteSelectorEvaluation.qualityConstant) {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
        if (context.call.request.local.port == port)
            RouteSelectorEvaluation.Constant
        else RouteSelectorEvaluation.Failed

    override fun toString() = "(port:$port)"
}
