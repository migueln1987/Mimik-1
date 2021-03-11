package mimik.networkRouting.HelpPages

import io.ktor.routing.*

interface HelperContract {
    fun Route.route(path: HelperPaths, build: Route.() -> Unit): Route = route(path.path, build)

    enum class HelperPaths(private val init: String) {
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
