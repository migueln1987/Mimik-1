package networkRouting.testingManager

import helpers.*
import io.ktor.application.call
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import kolor.cyan
import kolor.green
import kolor.magenta
import kolor.yellow
import networkRouting.RoutingContract
import java.time.Duration
import java.util.Date

@Suppress("RemoveRedundantQualifierName")
class TestManager : RoutingContract(RoutePaths.rootPath) {
    private enum class RoutePaths(val path: String) {
        START("start"),
        APPEND("append"),
        DISABLE("disable"),
        POKE("poke"),
        STOP("stop");

        companion object {
            const val rootPath = "tests"
        }
    }

    override fun init(route: Route) {
        route.route(path) {
            start
            append
            disable
            stop
            poke
        }
    }

    companion object {
        val boundManager = mutableListOf<TestBounds>()

        /**
         * Returns if there are any enabled test bounds
         */
        val hasEnabledBounds = boundManager.any { it.isEnabled }

        /**
         * Attempts to retrieve a [TestBounds] which has the following [handle].
         * - If none is found and a bound is [BoundStates.Ready], the [handle] is applied to that bound, then returned
         * - [null] is returned if none of the above is true
         */
        @Synchronized
        fun getManagerByID(handle: String?): TestBounds? {
            if (handle == null) return null
            var bound = boundManager.asSequence()
                .filter { it.handle == handle }
                .sortedBy { it.startTime }
                .firstOrNull()
            if (bound != null)
                return bound

            bound = boundManager.asSequence()
                .filter { it.state == BoundStates.Ready }
                .sortedBy { it.createTime }
                .firstOrNull()
            if (bound != null)
                bound.boundSource = handle

            return bound
        }
    }

    private fun Headers.getValidTapes(): List<String> {
        val heads = getAll("tape")
        if (heads.isNullOrEmpty()) return listOf()
        val tapeCatNames = tapeCatalog.tapes.map { it.name }
        val containsInit = heads.any { it.equals("init", true) }

        return heads.flatMap { it.split(',') }.asSequence()
            .filter { it.isBlank() }
            .map { it.trim() }
            .filter { tapeCatNames.contains(it) }
            .toList()
            .let {
                if (containsInit)
                    it.toMutableList().apply { add("Init") }
                else it
            }
    }

    private val Route.start: Route
        get() = route(RoutePaths.START.path) {
            /**
             * Response codes:
             * - 100 (Continue) {Update existing data}
             * - 201 (Created) {Created new bounds}
             * - 303 (SeeOther) {Created, but with a non-conflicting handle}
             * - 400 (BadRequest)
             */
            post {
                val heads = call.request.headers
                var handle = heads["handle"]
                val allowedTapes = heads.getValidTapes()
                val time = heads["time"]

                val noConfigs = allTrue(
                    handle.isNullOrBlank(),
                    allowedTapes.isEmpty(),
                    time.isNullOrBlank()
                )

                var canContinue = false
                when {
                    noConfigs ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No config headers" }
                    allowedTapes.isEmpty() ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No [tape] config data" }
                    else -> canContinue = true
                }
                if (!canContinue) return@post

                var testBounds = boundManager.firstOrNull { it.handle == handle }
                var useExisting = false

                if (testBounds == null) {
                    handle = ensureUniqueName(handle)
                    printlnF(
                        "%s -> %s",
                        "No test named (%s).".magenta().format(
                            when (heads["handle"]) {
                                null -> "{null}"
                                "" -> "{empty string}"
                                else -> heads["handle"]
                            }
                        ),
                        "Creating new test named: $handle".green()
                    )

                    testBounds = TestBounds(handle, allowedTapes.toMutableList())
                    boundManager.add(testBounds)
                } else {
                    useExisting = true
                    testBounds.boundSource == null
                }

                val timeVal = time?.replace("\\D".toRegex(), "")?.toLongOrNull() ?: 5
                val timeType = time?.replace("\\d".toRegex(), "")

                testBounds.timeLimit = when (timeVal) {
                    in (Long.MIN_VALUE..0) -> Duration.ofHours(1)
                    else -> when (timeType) {
                        "m" -> Duration.ofMinutes(timeVal)
                        "h" -> Duration.ofHours(timeVal)
                        "s" -> Duration.ofSeconds(timeVal)
                        else -> Duration.ofSeconds(timeVal)
                    }
                }

                val status = when (heads["handle"]) {
                    null, "" -> HttpStatusCode.Created
                    else -> {
                        if (useExisting)
                            HttpStatusCode.Continue
                        else
                            HttpStatusCode.Created
                    }
                }

                printlnF(
                    "Test Bounds (%s) ready with %d tapes:".green() + "%s".cyan(),
                    testBounds.handle,
                    allowedTapes.size,
                    allowedTapes.joinToString(
                        prefix = "\n- ",
                        separator = "\n- "
                    ) { it }
                )

                call.response.headers.apply {
                    append("tape", allowedTapes)
                    append("handle", handle.toString())
                }

                call.respondText(status = status) { "" }
            }
        }

    private val Route.append: Route
        get() = route(RoutePaths.APPEND.path) {
            /**
             * Response codes:
             * - 200 (OK)
             * - 304 (NotModified)
             * - 400 (BadRequest)
             */
            post {
                val heads = call.request.headers
                val handle = heads["handle"]
                val appendTapes = heads.getValidTapes()

                val noConfigs = allTrue(
                    handle.isNullOrBlank(),
                    appendTapes.isEmpty()
                )
                val boundHandle = boundManager.firstOrNull { it.handle == handle }

                var canContinue = false
                when {
                    noConfigs ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No config headers" }
                    appendTapes.isEmpty() ->
                        call.respondText(status = HttpStatusCode.NotModified) { "No [tape] data to append" }
                    boundHandle == null ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No test bounds with the name '$handle'" }
                    else -> canContinue = true
                }
                if (!canContinue) return@post

                printlnF(
                    "Appending %s tape%s to test (%s)".green(),
                    appendTapes.size,
                    if (appendTapes.size > 1) "s" else "",
                    handle
                )
                boundHandle?.tapes?.addAll(appendTapes)
                call.response.headers.append("tape", appendTapes)
                call.respondText(status = HttpStatusCode.OK) { "" }
            }
        }

    private val Route.disable: Route
        get() = route(RoutePaths.DISABLE.path) {
            /**
             * Response codes:
             * - 200 (OK)
             * - 304 (NotModified)
             * - 400 (BadRequest)
             */
            post {
                val heads = call.request.headers
                val handle = heads["handle"]
                val disableTapes = heads.getValidTapes()

                val noConfigs = allTrue(
                    handle.isNullOrBlank(),
                    disableTapes.isEmpty()
                )
                val boundHandle = boundManager.firstOrNull { it.handle == handle }

                var canContinue = false
                when {
                    noConfigs ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No config headers" }
                    disableTapes.isEmpty() ->
                        call.respondText(status = HttpStatusCode.NotModified) { "No requested [tape] to disable" }
                    boundHandle == null ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No test bounds with the name '$handle'" }
                    else -> canContinue = true
                }
                if (!canContinue) return@post

                var dCount = 0
                disableTapes.forEach { t ->
                    boundHandle?.tapes?.removeIf {
                        (it == t).also { dCount++ }
                    }
                }
                printlnF(
                    "Disabling %s tape%s in test (%s)".green(),
                    dCount,
                    if (dCount > 1) "s" else "",
                    handle
                )
                call.respondText(status = HttpStatusCode.OK) { "" }
            }
        }

    private val Route.stop: Route
        get() = route(RoutePaths.STOP.path) {
            /**
             * Response codes:
             * - 200 (OK)
             * - 304 (NotModified)
             * - 400 (BadRequest)
             */
            post {
                val heads = call.request.headers
                val handles = heads["handle"]?.split(',')

                if (handles == null || handles.isEmpty()) {
                    call.respondText(status = HttpStatusCode.BadRequest) { "No [handle] parameter" }
                    return@post
                }

                var stoppedCnt = 0
                boundManager.asSequence()
                    .filter { handles.contains(it.handle) }
                    .forEach {
                        val sb = StringBuilder()
                        sb.appendln("Stopping test bounds (${it.handle})...")
                        when (it.state) {
                            BoundStates.Ready -> sb.append("Test was idle. no change")
                            BoundStates.Stopped -> sb.append("Test was already stopped")
                            BoundStates.Started -> sb.append("Test was stopped")
                            else -> sb.append("Test was in an unknown state".magenta())
                        }
                        println(sb.toString().yellow())

                        it.stopTest()

                        val duration = (it.timeLimit - (Date() - it.expireTime)).toString().removePrefix("PT")
                        call.response.headers.append("${it.handle}_time", duration)
                        printlnF(
                            "Test bounds (%s) ran for %s".yellow(),
                            it.handle,
                            duration
                        )
                        stoppedCnt++
                    }

                val status = when (stoppedCnt) {
                    handles.size -> HttpStatusCode.OK
                    else -> {
                        call.response.headers.append("missing", (handles.size - stoppedCnt).toString())
                        printlnF(
                            "%d Test Bounds were not stopped/ unchanged".magenta(),
                            handles.size - stoppedCnt
                        )
                        HttpStatusCode.NotModified
                    }
                }

                call.respondText(status = status) { "" }
            }
        }

    private val Route.poke: Route
        get() = route(RoutePaths.POKE.path) {
            post {
                call.respondText(status = HttpStatusCode.OK) { "good" }
            }
        }

    fun ensureUniqueName(handle: String?): String {
        val randHost = RandomHost()

        var result = handle
        if (handle.isNullOrBlank()) {
            result = randHost.valueAsChars()
            printlnF(
                "Input handle is %s".magenta() + " -> " +
                        "Creating new handle: %s".green(),
                if (handle == null) "null" else "blank",
                result
            )
        }

        while (boundManager.any { it.handle == result }) {
            println("New Handle - Conflicting name: $result".magenta())
            randHost.nextRandom()
            result = randHost.valueAsChars()
        }
        return result!!
    }
}
