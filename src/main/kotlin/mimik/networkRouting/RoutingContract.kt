package mimik.networkRouting

import io.ktor.routing.*
import mimik.networkRouting.editorPages.EditorModule

abstract class RoutingContract(val path: String) : EditorModule() {
    abstract fun init(route: Route)
}
