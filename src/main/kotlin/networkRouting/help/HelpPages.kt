package networkRouting.help

import io.ktor.routing.*
import networkRouting.RoutingContract

/**
 * API call which returns documentation on how to use the "/mock" api
 *
 * @see <a href="0.0.0.0:4321/help">help</a>
 */
class HelpPages : RoutingContract("help") {

    override fun init(route: Route) {
        route.route(path) {
            arrayOf(
                RootHelp,
                TapeHelp,
                ChapterHelp
            ).forEach { it.load(this) }
        }
    }
}
