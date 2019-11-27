package networkRouting.testingManager

import helpers.*
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import kolor.green
import kolor.magenta
import networkRouting.RoutingContract
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Date

@Suppress("RemoveRedundantQualifierName")
class TestManager : RoutingContract(RoutePaths.rootPath) {
    private enum class RoutePaths(val path: String) {
        START("start"),
        APPEND("append"),
        DISABLE("disable"),
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
            var bound = boundManager.firstOrNull { it.handle == handle }
            if (bound != null)
                return bound

            bound = boundManager.firstOrNull { it.state == BoundStates.Ready }
            if (bound != null)
                bound.boundSource = handle

            return bound
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
                var allowedTapes = heads.getAll("tape")?.flatMap {
                    it.split(',')
                }
                val time = heads["time"]

                val noConfigs = allTrue(
                    handle.isNullOrBlank(),
                    allowedTapes?.isEmpty().isTrue(),
                    time.isNullOrBlank()
                )

                var canContinue = false
                when {
                    noConfigs ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No config headers" }
                    allowedTapes.isNullOrEmpty() ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No [tape] config data" }
                    else -> canContinue = true
                }
                if (!canContinue) return@post


                val tapeCatNames = tapeCatalog.tapes.map { it.name }
                allowedTapes = (allowedTapes ?: listOf()).filter { tapeCatNames.contains(it) } as MutableList

                var testBounds = boundManager.firstOrNull { it.handle == handle }
                var useExisting = false

                if (testBounds == null) {
                    handle = ensureUniqueName(handle)
                    printF(
                        "No test named (%s).".magenta(),
                        when (heads["handle"]) {
                            null -> "{null}"
                            "" -> "{empty string}"
                            else -> heads["handle"]
                        }
                    )
                    println(" -> " + "Creating new test named: $handle".green())

                    testBounds = TestBounds(handle, allowedTapes)
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
                var appendTapes = heads.getAll("tape")?.flatMap {
                    it.split(',')
                }

                val noConfigs = allTrue(
                    handle.isNullOrBlank(),
                    appendTapes?.isEmpty().isTrue()
                )
                val boundHandle = boundManager.firstOrNull { it.handle == handle }

                var canContinue = false
                when {
                    noConfigs ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No config headers" }
                    appendTapes.isNullOrEmpty() ->
                        call.respondText(status = HttpStatusCode.NotModified) { "No [tape] data to append" }
                    boundHandle == null ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No test bounds with the name '$handle'" }
                    else -> canContinue = true
                }
                if (!canContinue) return@post

                val tapeCatNames = tapeCatalog.tapes.map { it.name }
                appendTapes = appendTapes?.filter { tapeCatNames.contains(it) } ?: listOf()


                if (appendTapes.isEmpty()) {
                    call.respondText(status = HttpStatusCode.NotModified) { "No [tape] data to append" }
                } else {
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
                var disableTapes = heads.getAll("tape")?.flatMap {
                    it.split(',')
                }

                val noConfigs = allTrue(
                    handle.isNullOrBlank(),
                    disableTapes?.isEmpty().isTrue()
                )
                val boundHandle = boundManager.firstOrNull { it.handle == handle }

                var canContinue = false
                when {
                    noConfigs ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No config headers" }
                    disableTapes.isNullOrEmpty() ->
                        call.respondText(status = HttpStatusCode.NotModified) { "No requested [tape] to disable" }
                    boundHandle == null ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No test bounds with the name '$handle'" }
                    else -> canContinue = true
                }
                if (!canContinue) return@post

                val tapeCatNames = tapeCatalog.tapes.map { it.name }
                disableTapes = disableTapes?.filter { tapeCatNames.contains(it) } ?: listOf()

                if (disableTapes.isEmpty()) {
                    call.respondText(status = HttpStatusCode.NotModified) { "No requested [tape] to disable" }
                } else {
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
                val handle = heads["handle"]

                if (handle.isNullOrBlank()) {
                    call.respondText(status = HttpStatusCode.BadRequest) { "No [handle] parameter" }
                    return@post
                }

                var stoppedTest = false
                boundManager.asSequence()
                    .firstOrNull { it.handle == handle }
                    ?.also {
                        call.response.headers.append("state", it.state.ordinal.toString())
                        it.isEnabled = false
                        it.expireTimer?.cancel()

                        printlnF(
                            "Test bounds (%s) ran for %s",
                            it.handle,
                            (Date() - it.expireTime).toString().removePrefix("PT")
                        )
                        stoppedTest = true
                    }

                val status = when (stoppedTest) {
                    true -> {
                        println("Test Bounds ($handle) was stopped successfully".green())
                        HttpStatusCode.OK
                    }
                    false -> {
                        println("Test Bounds ($handle) was not found/ no change to state".magenta())
                        HttpStatusCode.NotModified
                    }
                }

                call.respondText(status = status) { "" }
            }
        }

    fun ensureUniqueName(handle: String?): String {
        val randHost = RandomHost()

        var result = handle
        if (result.isNullOrBlank()) {
            result = randHost.valueAsChars()
            println("Creating a new handle name: $result".green())
        }

        while (boundManager.any { it.handle == result }) {
            println("New Handle - Conflicting name: $result".magenta())
            randHost.nextRandom()
            result = randHost.valueAsChars()
        }
        return result!!
    }
}
