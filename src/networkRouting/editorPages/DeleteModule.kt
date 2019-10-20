package networkRouting.editorPages

import helpers.isTrue
import helpers.removePrefix
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.util.pipeline.PipelineContext

object DeleteModule : EditorModule() {
    suspend fun PipelineContext<*, ApplicationCall>.deleteActions() {
        tapeCatalog.tapes.firstOrNull { it.name == call.parameters["tape"] }
            ?.also { tape ->
                val chapterName = call.parameters["chapter"]
                if (chapterName == null) {
                    tapeCatalog.tapes.remove(tape)
                    if (tape.file?.exists().isTrue())
                        tape.file?.delete()
                } else {
                    tape.chapters.removeIf { it.name == chapterName }

                    call.respondRedirect {
                        val pathStart = this.encodedPath.substringBeforeLast('/')
                            .removePrefix("/")
                        path(pathStart, TapeRouting.RoutePaths.EDIT.path)
                        parameters.clear()
                        parameters.append("tape", tape.name)
                    }
                    return
                }
            }

        call.respondRedirect(TapeRouting.RoutePaths.ALL.path)
    }
}
