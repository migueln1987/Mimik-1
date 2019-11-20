package networkRouting.testingManager

import helpers.RandomHost
import helpers.allTrue
import helpers.appendHeaders
import helpers.isTrue
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import networkRouting.RoutingContract
import kotlin.math.max

@Suppress("RemoveRedundantQualifierName")
class TestManager : RoutingContract(RoutePaths.rootPath) {
    private enum class RoutePaths(val path: String) {
        START("start"),
        STOP("stop");

        companion object {
            const val rootPath = "tests"
        }
    }

    override fun init(route: Route) {
        route.route(path) {
            start
            stop
        }
    }

    companion object {
        val boundManager = mutableListOf<TestBounds>()

        /**
         * Returns if there are any enabled test bounds
         */
        val hasEnabledBounds = boundManager.any {
            it.isEnabled
        }

        /**
         * Attempts to retrieve a [TestBounds] which has the following [handle].
         * - If none is found and a bound is [BoundStates.Ready], the [handle] is applied to that bound, then returned
         * - [null] is returned if none of the above is true
         */
        fun getManagerByID(handle: String?): TestBounds? {
            if (handle == null) return null
            var bound = boundManager.firstOrNull { it.handle == handle }
            if (bound != null)
                return bound

            bound = boundManager.firstOrNull { it.state == BoundStates.Ready }
            if (bound != null) {
                bound.boundSource = handle
                return bound
            }

            return null
        }
    }

    private val Route.start: Route
        get() = route(RoutePaths.START.path) {
            post {
                val heads = call.request.headers
                var handle = heads["handle"]
                var allowedTapes = heads.getAll("tape")
                val time = heads["time"]

                val noConfigs = allTrue(
                    handle == null,
                    allowedTapes?.isEmpty().isTrue(),
                    time == null
                )

                if (noConfigs) {
                    call.respondText(status = HttpStatusCode.BadRequest) { "No config headers" }
                    return@post
                }

                if (allowedTapes.isNullOrEmpty()) {
                    call.respondText(status = HttpStatusCode.BadRequest) { "No [tape] config data" }
                    return@post
                }

                val tapeCatNames = tapeCatalog.tapes.map { it.name }
                allowedTapes = allowedTapes.filter { tapeCatNames.contains(it) }

                handle = ensureUniqueName(handle)

                boundManager.add(
                    TestBounds(handle, allowedTapes).also {
                        it.timeLimit = max(
                            it.timeLimit,
                            time?.toIntOrNull() ?: 0
                        )
                    }
                )

                val status = when (handle) {
                    heads["handle"] -> HttpStatusCode.OK
                    else -> HttpStatusCode.SeeOther
                }

                val responseHeads = mapOf("tape" to allowedTapes)
                call.response.headers.appendHeaders(responseHeads)
                call.respondText(status = status) { handle }
            }
        }

    private val Route.stop: Route
        get() = route(RoutePaths.STOP.path) {
            post {
                val heads = call.request.headers
                val handle = heads["handle"]

                if (handle.isNullOrBlank()) {
                    call.respondText(status = HttpStatusCode.BadRequest) { "No [handle] parameter" }
                    return@post
                }

                var stoppedTest = false
                boundManager.asSequence()
                    .filter { it.handle == handle }
                    .forEach {
                        it.isEnabled = false
                        stoppedTest = true
                    }

                val status = when (stoppedTest) {
                    true -> HttpStatusCode.OK
                    false -> HttpStatusCode.NotModified
                }
                call.respond(status, "")
            }
        }

    fun ensureUniqueName(handle: String?): String {
        val randHost = RandomHost()

        var result = handle
        if (result.isNullOrBlank())
            result = randHost.valueAsChars()

        while (boundManager.any { it.handle == result }) {
            randHost.nextRandom()
            result = randHost.valueAsChars()
        }
        return result!!
    }
}
