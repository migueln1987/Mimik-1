package mimik.networkRouting.help

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.beautifyJson

object TapeHelp : HelperContract {
    override val subPath get() = "tape"

    override val Route.default: Route
        get() = apply {
            suspend fun PipelineContext<Unit, ApplicationCall>.action() {
                call.respondText {
                    """
                        This is the {Tape} help page
                    """.trimIndent()
                }
            }

            get { action() }
//            put { action() }
        }

    override val Route.format: Route
        get() = route(HelperContract.HelperPaths.FORMAT) {
            suspend fun PipelineContext<Unit, ApplicationCall>.action() {
                val tapeInfo = TemplateBuilder.build(CreateTypes.Tape).beautifyJson
                call.response.headers.append(
                    HttpHeaders.ContentType, ContentType.Application.Json, false
                )

                call.respondText { tapeInfo }
            }

            get { action() }
//            put { action() }
        }

    override val Route.example: Route
        get() = route(HelperContract.HelperPaths.EXAMPLE) {
            suspend fun PipelineContext<Unit, ApplicationCall>.action() {
                val example = ExampleGenerator.build(CreateTypes.Tape)
                call.respondText { example }
            }

            get { action() }
//            put { action() }
        }
}
