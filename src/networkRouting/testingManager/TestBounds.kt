package networkRouting.testingManager

import helpers.*
import java.time.Duration
import java.util.*
import kolor.red
import kolor.yellow
import kotlinx.atomicfu.atomic
import mimikMockHelpers.RecordedInteractions
import okhttp3.ResponseBody
import kotlin.concurrent.schedule
import tapeItems.BlankTape

data class TestBounds(var handle: String, val tapes: MutableList<String> = mutableListOf()) {

    companion object {
        enum class DataTypes {
            Head, Body, Var
        }
    }

    private var expireTimer: TimerTask? = null

    var boundSource: String? = null
        set(value) {
            println("Declaring bounds as: $value")
            when (value) {
                null -> {
                    isEnabled.value = false
                }
                field -> {
                    println("Re-assigning test bound -> Resetting time".yellow())

                    val (tStart, tEnd) = startTest()
                    printlnF(
                        ("Reset test bounds\n" +
                                "- Handle: %s\n" +
                                "- Target ID: (%s)\n" +
                                "- From: %s\n" +
                                "- To:   %s").yellow(),
                        handle,
                        value,
                        tStart,
                        tEnd
                    )
                }
                else -> {
                    field = value

                    val (tStart, tEnd) = startTest()

                    printlnF(
                        ("Starting test bounds\n" +
                                "- Handle: %s\n" +
                                "- Target ID: (%s)\n" +
                                "- From: %s\n" +
                                "- To:   %s").yellow(),
                        handle,
                        value,
                        tStart,
                        tEnd
                    )
                }
            }
        }

    /**
     * Limit of when the bounds's [expireTime] will expire.
     *
     * Value is in seconds
     */
    var timeLimit: Duration = Duration.ofSeconds(5)

    private fun startTest(): Pair<String, String> {
        if (tapes.contains("##All")) {
            // for unit tests only, lazy adds ALL the known tapes
            tapes.clear()
            tapes.addAll(TapeCatalog.Instance.tapes.map { it.name })
        }
        isEnabled.value = true
        expireTimer?.cancel()
        startTime = Date()
        val startStr = startTime.toString()
        expireTime = (startTime!! + timeLimit).also {
            expireTimer = Timer("Handle: $handle", false).schedule(it) {
                println("Test bounds ($handle) has expired".red())
                isEnabled.value = false
            }
        }

        return startStr to expireTime.toString()
    }

    fun stopTest() {
        expireTimer?.cancel()
        isEnabled.value = false
    }

    val createTime = Date()

    var startTime: Date? = null
        private set

    /**
     * Time of when this bounds expires
     */
    var expireTime: Date? = null
        private set

    val state: BoundStates
        get() {
            return when {
                !isEnabled.value -> BoundStates.Stopped
                expireTime == null -> BoundStates.Ready
                expireTime != null -> BoundStates.Started
                else -> BoundStates.Unknown
            }
        }

    var isEnabled = atomic(true)
        private set

    var finalized = false

    /**
     * {tape name}, <{chapter name}, uses>
     */
    val stateUses =
        mutableMapOf<String, MutableList<Pair<String, Int>>>()

    /**
     * (chapter name), (data)
     */
    val replacerData: MutableMap<String, boundChapterItems> = mutableMapOf()

    val boundVars: MutableMap<String, String> = mutableMapOf()
}

class boundChapterItems {
    val replacers_body: MutableList<Pair<String, String>> = mutableListOf()
    // todo; replacers_head

    val findVars: MutableList<Pair<String, String>> = mutableListOf()
}

enum class BoundStates {
    Ready, Started, Stopped, Unknown
}

/**
 * Observes changes to [tape] and routes the interactions to this [TestBounds].
 */
inline fun <reified T : Any?> TestBounds?.observe(tape: BlankTape, watch: () -> T?): T? {
    if (this == null) return watch.invoke()
    if (!stateUses.containsKey(tape.name))
        stateUses[tape.name] = mutableListOf()

    val watcherAct: (RecordedInteractions, Int?) -> Int = { chap, value ->
        var data = stateUses[tape.name]
            ?.firstOrNull { it.first == chap.name }

        when (value) {
            null -> { // get
                if (data == null) { // no existing data, initialize default value
                    data = (chap.name to (chap.origionalMockUses ?: chap.mockUses))
                    stateUses[tape.name]?.add(data)
                }

                data.second
            }

            else -> { // set
                if (data != null)
                    stateUses[tape.name]?.removeIf { it == data }

                stateUses[tape.name]?.add(chap.name to value)
                value
            }
        }
    }

    tape.useWatchers.push(watcherAct)

    val returns = watch.invoke()
    tape.useWatchers.remove(watcherAct)
    return returns
}

fun okhttp3.Request.collectVars(bounds: TestBounds?, chap: RecordedInteractions?) {
    if (bounds == null || chap == null) return

    val bodyContent = body()?.content()
    if (bodyContent.isNullOrEmpty()) return
    val byChap = bounds.replacerData[chap.name] ?: return

    byChap.findVars.forEach {
        val matches = it.first.matchResults(bodyContent)
        if (matches.hasMatches) {
            bounds.boundVars[it.second] = matches.first().value.orEmpty()
        }
    }
}

fun okhttp3.Response.collectVars(bounds: TestBounds?, chap: RecordedInteractions?): okhttp3.Response {
    if (bounds == null || chap == null) return this
    val byChap = bounds.replacerData[chap.name] ?: return this

    val bodyContent = body()?.content().orEmpty()

    byChap.findVars.forEach {
        val matches = it.first.matchResults(bodyContent)
        if (matches.hasMatches) {
            bounds.boundVars[it.second] = matches.last().value.orEmpty()
        }
    }

    return newBuilder().apply {
        body(
            ResponseBody.create(
                body()?.contentType(),
                bodyContent
            )
        )
    }.build()
}

private const val templateReg = """@\{(.+?)\}"""
private const val variableMatch = """((?<content>\w+)|(?<final>['"].+?['"]))"""
fun okhttp3.Response.replaceByTest(bounds: TestBounds?, chap: RecordedInteractions?): okhttp3.Response {
    if (bounds == null || chap == null) return this
    val byChap = bounds.replacerData[chap.name] ?: return this

    /* Actions:
    1. use (replacerFrom) to find matching content in (body)
    -> [0] = (repFromRoot)
    --> range which must be replaced
    -> [+] (replacer[+])
    --> content which was matched

    2.a Template checking
    2.a.1 (templateReg) determined that (replacerTo) is a template; "@{...}"
    -> result becomes (templateMatches)
    ~> (replacerTo) -> (repToOut) as writable copy

    2.a.2 (variableMatch) collects the results of (templateMatches)[1].sortedByDescending
    -> each result becomes (templateMatch)
    -> (temRootRange) = indexOf(item) or (templateMatch)[0]

    2.a.3 loop through the items of (templateMatch) by group name
    - process/ use only the items which are valid
    2.a.3.a empty; skip ths item
    2.a.3.b "content"; use as is
    2.a.3.c "final"; remove padding quotes and use data
    2.a.3.d other; TBD, use as is for now

    2.a.4 Process the valid group items
    2.a.4.a group name is "final"
    ->> (value to range to replace)

    2.a.4.b item is a template to be processed
    ~> below processing (1-3) becomes (result)
    2.a.4.b.1 collect vars and process the item
    --> (temValue) = index/ group name/ bounds variable

    2.a.4.b.1.a (temValue) is an index
    2.a.4.b.1.a.1 (replacer) must contain a index of (content index)
    -> (temRepValue) = (replacer)[temValue as index]

    2.a.4.b.1.b check if (replacer) contains [temValue] - as it's closer relative
    -> (temRepValue) = (replacer)[temValue]

    2.a.4.b.1.c check if (boundVars) contains (content key)
    -> (temRepValue) = boundVars[temValue]

    2.a.4.b.2 (temRepValue) is not null
    ->> (temRepValue to range to replace)

    2.a.4.b.3 above result is null, no matches or "finals" were found
    ->> (empty string to range to replace)

    2.a.5 Use result and replace range; (result to temRootRange)
    => replace (repToOut) at (temRootRange) with value from (result)

    2.b Not a template
    => replace (body) at (replacer[0] range) with (replacerTo)
   */

    var bodyContent = body()?.content().orEmpty()

    // todo; move content into it's own function, so replacers_head can use it too
    byChap.replacers_body.forEach { (replacerFrom, replacerTo) ->
        val replacer = replacerFrom.matchResults(bodyContent)
        if (replacer.hasMatches) {
            val repRootFromRange = replacer[0].first().range

            val templateMatches = templateReg.matchResults(replacerTo)
            var repToOut: String? = null
            if (templateMatches.hasMatches) {
                repToOut = replacerTo
                templateMatches[1].sortedByDescending { it.range.first }.forEach { temMatch ->
                    val template = variableMatch.matchResults(temMatch.value)
                    val temRootRange = templateMatches.rootIndexOf(temMatch)!!.range

                    val temReplace = template.mapNotNull {
                        when (it.groupName) {
                            "" -> null
                            "content" -> it
                            "final" -> { // remove bounding quotes (single or double)
                                it.clone { b ->
                                    b.value = b.value!!.drop(1).dropLast(1)
                                }
                            }
                            else -> it
                        }
                    }
                        .firstNotNullResult {
                            when (it.groupName) {
                                "final" -> it.value.orEmpty()
                                else -> {
                                    // if this path has a non-null result, then use it
                                    val temValue = it.value.orEmpty()
                                    val temIndex = temValue.toIntOrNull()

                                    when {
                                        temIndex != null -> { // is it a index?
                                            if (replacer.contains(temIndex))
                                                replacer[temIndex].first().value
                                            else null
                                        }

                                        replacer.contains(temValue) -> // a named group?
                                            replacer[temValue].first().value.orEmpty()

                                        bounds.boundVars.containsKey(temValue) -> // a variable?
                                            bounds.boundVars.getValue(temValue)

                                        else -> null // nothing found
                                    }
                                }
                            }
                        }.orEmpty()

                    repToOut = repToOut!!.replaceRange(
                        temRootRange.first,
                        temRootRange.last,
                        temReplace
                    )
                }
            }
            // todo; "Replaced:\n== From\n %s\n== To\n %s"
            bodyContent = bodyContent
                .replaceRange(repRootFromRange.first, repRootFromRange.last, repToOut ?: replacerTo)
        }
    }

    return newBuilder().apply {
        body(
            ResponseBody.create(
                body()?.contentType(),
                bodyContent
            )
        )
    }.build()
}
