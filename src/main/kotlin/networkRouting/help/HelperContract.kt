package networkRouting.help

import io.ktor.routing.*

interface HelperContract {
    enum class HelperPaths(private val init: String) {
        DEFAULT(""),
        FORMAT("format"),
        EXAMPLE("example");

        val path: String get() = "/$init"
    }

    val subPath: String

    val Route.default: Route
    val Route.format: Route
    val Route.example: Route

    fun load(route: Route) {
        route.route(subPath) {
            default
            format
            example
        }
    }
}
