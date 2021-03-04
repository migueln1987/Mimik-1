package mimik.networkRouting.routers

import io.ktor.routing.*

/**
 * Builds a route to match specified [port]
 */
fun Route.port(port: Int, body: Route.() -> Unit): Route {
    val selector = HttpPortRouteSelector(port)
    return createChild(selector).apply(body)
}

/**
 * Builds a route to match each specified [port]
 */
fun Route.port(vararg port: Int, body: Route.() -> Unit): List<Route> {
    return port.map {
        val selector = HttpPortRouteSelector(it)
        createChild(selector).apply(body)
    }
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
