package networkRouting.help

import helpers.beautifyJson
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*

object ChapterHelp : HelperContract {
    override val subPath get() = "chapter"

    override val Route.default: Route
        get() = route(HelperContract.HelperPaths.DEFAULT.path) {
            suspend fun PipelineContext<Unit, ApplicationCall>.action() {
                call.respondText {
                    """
                        This is the {Chapter} help page
                    """.trimIndent()
                }
            }

            get { action() }
            put { action() }
        }

    override val Route.format: Route
        get() = route(HelperContract.HelperPaths.FORMAT.path) {
            suspend fun PipelineContext<Unit, ApplicationCall>.action() {
                val tapeInfo = TemplateBuilder.build(CreateTypes.Chapter).beautifyJson
                call.response.headers.append(
                    HttpHeaders.ContentType, ContentType.Application.Json, false
                )
                call.respondText { tapeInfo }
            }

            get { action() }
            put { action() }
        }

    override val Route.example: Route
        get() = route(HelperContract.HelperPaths.EXAMPLE.path) {
            suspend fun PipelineContext<Unit, ApplicationCall>.action() {
                val example = ExampleGenerator.build(CreateTypes.Chapter)
                call.respondText { example }
            }

            get { action() }
            put { action() }
        }
}
