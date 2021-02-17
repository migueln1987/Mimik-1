package mimik.networkRouting.help

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.linebreak

object RootHelp : HelperContract {
    override val subPath: String get() = ""

    // custom load, so only 'default' is loaded
    override fun load(route: Route) {
        route.route(subPath) { default }
    }

    private fun Route.toStringList(): List<String> = when (val selector = selector) {
        is PathSegmentConstantRouteSelector -> children.flatMap { it.toStringList() }.distinct()
            .map { "${selector.value}/$it" }
        is HttpMethodRouteSelector -> listOf("") // ""{${selector.method.value}}")
        else -> listOf(selector.toString())
    }

    override val Route.default: Route
        get() = route(HelperContract.HelperPaths.DEFAULT.path) {
            val self = this
            suspend fun PipelineContext<Unit, ApplicationCall>.action() {
                call.respondHtml {
                    body {
                        +"This is the root help page"
                        br()
                        +"The available helper pages are:"
                        linebreak()

                        self.toStringList().onEachIndexed { index, urlPath ->
                            if (index > 0) {
                                a {
                                    href = "/$urlPath"
                                    +href
                                }
                                br()
                            }
                        }
                    }
                }
            }

            get { action() }
            put { action() }
        }

    override val Route.format: Route
        get() = TODO("Unused")

    override val Route.example: Route
        get() = TODO("Unused")
}
