package networkRouting

import io.ktor.routing.Route
import networkRouting.editorPages.TapeRouting

class MimikHomepage : RoutingContract(TapeRouting.RoutePaths.rootPath) {

    enum class RoutePaths(val path: String) {
        Mocks("mocks"),
        Tests("tests");
    }

    override fun init(route: Route) {
        TODO("Not yet implemented")
    }
}
