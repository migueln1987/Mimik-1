package networkRouting.editorPages

import helpers.isTrue
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.util.pipeline.PipelineContext

object DeleteModule : EditorModule() {
    suspend fun PipelineContext<*, ApplicationCall>.deleteActions() {
        tapeCatalog.tapes.firstOrNull { it.name == call.parameters["tape"] }
            ?.also { tape ->
                when (val chapterName = call.parameters["chapter"]) {
                    null -> {
                        if (tapeCatalog.tapes.remove(tape) && tape.file?.exists().isTrue())
                            tape.file?.delete()
                        call.respondRedirect(TapeRouting.RoutePaths.ALL.path)
                    }
                    else -> {
                        if (tape.chapters.removeIf { it.name == chapterName } && tape.file?.exists().isTrue())
                            tape.saveFile()

                        call.respondRedirect {
                            val pathStart = this.encodedPath.substringBeforeLast('/')
                                .removePrefix("/")
                            path(pathStart, TapeRouting.RoutePaths.EDIT.path)
                            parameters.clear()
                            parameters.append("tape", tape.name)
                        }
                    }
                }
            }
    }
}