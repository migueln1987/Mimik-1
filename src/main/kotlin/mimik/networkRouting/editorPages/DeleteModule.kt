package mimik.networkRouting.editorPages

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import kotlinUtils.isTrue

object DeleteModule : EditorModule() {
    suspend fun PipelineContext<*, ApplicationCall>.deleteActions() {
        tapeCatalog.tapes.firstOrNull { it.name == call.parameters["tape"] }
            ?.also { tape ->
                when (val chapterName = call.parameters["chapter"]) {
                    null -> {
                        if (tapeCatalog.tapes.remove(tape) && tape.file?.exists().isTrue)
                            tape.file?.delete()
                        call.redirect(TapeRouting.RoutePaths.ALL.asSubPath)
                    }
                    else -> {
                        if (tape.chapters.removeIf { it.name == chapterName })
                            tape.saveIfExists()

                        call.redirect(TapeRouting.RoutePaths.EDIT.path) {
                            val pathStart = this.encodedPath.substringBeforeLast('/')
                                .removePrefix("/")
//                            path(pathStart, TapeRouting.RoutePaths.EDIT.path)
                            parameters.clear()
                            parameters["tape"] = tape.name
                        }
                    }
                }
            }
    }
}
