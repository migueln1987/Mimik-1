package mimik.networkRouting

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import mimik.helpers.toJson
import java.io.File
import kotlin.math.max

/**
 * Server handle which responds with the age of a file.
 *
 * This is used in the file watcher, to determine if a file was modified
 */
class FetchResponder : RoutingContract(RoutePaths.rootPath) {

    private enum class RoutePaths(val path: String) {
        AgeCheck("ageCheck");

        companion object {
            const val rootPath = "fetch"
        }
    }

    override fun init(route: Route) {
        route.route(path) {
            ageCheck
        }
    }

    private val Route.ageCheck: Route
        get() = route(RoutePaths.AgeCheck.path) {
            post { ageCheckAction() }
        }

    data class ageCheckObj(
        var action: String,
        @Transient
        val build: ageCheckObj.() -> Unit = {}
    ) {
        var data: String = ""

        init {
            build.invoke(this)
        }
    }

    suspend fun PipelineContext<*, ApplicationCall>.ageCheckAction() {
        val params = call.anyParameters()
//        if (params == null) {
//            call.respondText { ageCheckObj("Invalid") { data = "Unknown input" }.toJson }
//            return
//        }

        val fileName = params["file"].orEmpty()
        val tape = tapeCatalog.tapes
            .firstOrNull { it.file.toString() == fileName }
        if (tape == null) {
            call.respondText {
                ageCheckObj("Invalid") { data = "Data not found" }.toJson
            }
            return
        }

        val lastAge = params["age"]?.toLongOrNull() ?: Long.MAX_VALUE
        var newAge = 0L
        val file = File(fileName)

        when (params["type"]) {
            "file", "tape" -> {
                newAge = max(
                    file.lastModified(),
                    tape.modifiedDate?.time ?: 0
                )
            }

            "chapter" -> {
                val chap = tape.chapters.firstOrNull { it.name == params["name"] }
                    ?.apply {
                        newAge = max(
                            file.lastModified(),
                            modifiedDate?.time ?: 0
                        )
                    }

                if (chap == null) {
                    call.respondText {
                        ageCheckObj("Invalid") { data = "Chapter not found" }.toJson
                    }
                    return
                }
            }
        }

        if (newAge > lastAge)
            call.respondText { ageCheckObj("Refresh") { data = "$newAge" }.toJson }
        else
            call.respondText { ageCheckObj("").toJson }
    }
}
