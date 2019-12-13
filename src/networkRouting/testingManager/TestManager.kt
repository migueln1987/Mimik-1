package networkRouting.testingManager

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import helpers.*
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.pipeline.PipelineContext
import kolor.cyan
import kolor.green
import kolor.magenta
import kolor.yellow
import networkRouting.RoutingContract
import networkRouting.testingManager.TestBounds.Companion.DataTypes
import networkRouting.testingManager.TestBounds.Companion.DataTypes.valueOf
import java.lang.Exception
import java.time.Duration
import java.util.Date

@Suppress("RemoveRedundantQualifierName")
class TestManager : RoutingContract(RoutePaths.rootPath) {
    private enum class RoutePaths(val path: String) {
        START("start"),
        APPEND("append"),
        Modify("modify"),
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
            modify
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
            .filterNot { it.isBlank() }
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
                    tapeCatalog.tapes.isEmpty() ->
                        call.respondText(status = HttpStatusCode.PreconditionFailed) { "No tapes to append this test to" }
                    noConfigs ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No config headers" }
                    allowedTapes.isEmpty() ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No [tape] config data" }
                    else -> canContinue = true
                }
                if (!canContinue) return@post

                var testBounds = boundManager.firstOrNull { it.handle == handle }
                var useExisting = false

                val replacers = getReplacers()
                if (replacers.isNullOrEmpty()) {
                    call.respondText(status = HttpStatusCode.NoContent) { "No modifications entered" }
                    return@post
                } else {
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

                        testBounds = TestBounds(handle, allowedTapes.toMutableList()).also {
                            if (replacers.isNotEmpty())
                                it.replacerData.putAll(replacers)
                        }
                        boundManager.add(testBounds)
                    } else {
                        useExisting = true
                        testBounds.boundSource == null
                        if (replacers.isNotEmpty())
                            testBounds.replacerData.putAll(replacers)
                    }
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
                    if (replacers.isNotEmpty()) {
                        val items = replacers.values.sumBy { it.values.sumBy { it.size } }
                        append("mappers", items.toString())
                    }
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

    private suspend fun PipelineContext<*, ApplicationCall>.getReplacers() =
        try {
            getReplacers_v1()
        } catch (_: Exception) {
            try {
                getReplacers_v2()
            } catch (_: Exception) {
                null
            }
        }

    /**
    {
    "aa": {
    "Body": [
    {
    "from": "bb",
    "to": "cc"
    },
    {
    "from": "dd",
    "to": "dd"
    }
    ]
    }
    }
     */
    private suspend fun PipelineContext<*, ApplicationCall>.getReplacers_v1() = Gson()
        .fromJson(call.tryGetBody().orEmpty(), LinkedTreeMap::class.java).orEmpty()
        .mapKeys { it.key as String }
        .mapValues { mv ->
            (mv.value as LinkedTreeMap<*, *>)
                .mapKeys { DataTypes.valueOf(it.key.toString().uppercaseFirstLetter()) }
                .mapValues { mvv ->
                    (mvv.value as List<*>)
                        .map { (it as LinkedTreeMap<*, *>).map { it.value.toString() } }
                        .mapNotNull { if (it.size >= 2) it[0] to it[1] else null }
                        .toMutableList()
                }.toMap(mutableMapOf())
        }.toMutableMap()

    /**
    {
    "aa": [
    "body{[abc]->none}",
    "body{[abc]->none}"
    ],
    "bb": [
    "body{[ab]->none}"
    ]
    }
     */
    private suspend fun PipelineContext<*, ApplicationCall>.getReplacers_v2() = Gson()
        .fromJson(call.tryGetBody().orEmpty(), LinkedTreeMap::class.java).orEmpty()
        .mapKeys { it.key.toString() }
        .mapValues { mv ->
            (mv.value as List<*>)
                .map {
                    val contents = it.toString().split("{")
                    contents.getOrNull(0) to
                            contents.getOrNull(1)?.dropLast(1)?.split("->")
                                ?.run { if (size >= 2) get(0) to get(1) else null }
                }
                .filterNot { it.first == null || it.second == null }
                .map { it.first!! to it.second!! }
                .groupBy { it.first }
                .mapKeys { DataTypes.valueOf(it.key.uppercaseFirstLetter()) }
                .mapValues { it.value.map { it.second }.toMutableList() }
                .toMutableMap()
        }
        .toMutableMap()

    private val Route.modify: Route
        get() = route(RoutePaths.Modify.path) {
            post {
                val heads = call.request.headers
                val handle = heads["handle"]
                val testBounds = boundManager.firstOrNull { it.handle == handle }

                var canContinue = false
                when {
                    handle == null ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "Invalid handle name" }
                    testBounds == null ->
                        call.respondText(status = HttpStatusCode.BadRequest) { "No test bounds with the name($handle)" }
                    else -> canContinue = true
                }
                if (!canContinue) return@post

                val replacers = getReplacers()
                if (replacers.isNullOrEmpty()) {
                    call.respondText(status = HttpStatusCode.NoContent) { "No modifications entered" }
                } else {
                    requireNotNull(testBounds)
                    testBounds.replacerData.clear()
                    testBounds.replacerData.putAll(replacers)
                    val items = replacers.values.sumBy { it.values.sumBy { it.size } }
                    call.respondText(status = HttpStatusCode.OK) { "Appended $items to test bounds ${testBounds.handle}" }
                }
            }
            // todo; patch
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

    // todo; for testing
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
