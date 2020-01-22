package networkRouting.testingManager

import helpers.*
import java.time.Duration
import java.util.*
import kolor.red
import kolor.yellow
import kotlinx.atomicfu.atomic
import mimikMockHelpers.RecordedInteractions
import networkRouting.testingManager.TestBounds.Companion.DataTypes
import okhttp3.ResponseBody
import kotlin.concurrent.schedule
import tapeItems.BlankTape

data class TestBounds(var handle: String, val tapes: MutableList<String> = mutableListOf()) {

    companion object {
        enum class DataTypes {
            Head, Body
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
     * (chapter name), Map<DataTypes, List<data>>
     */
    val replacerData: MutableMap<String, MutableMap<DataTypes, MutableList<Pair<String, String>>>> = mutableMapOf()
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

fun okhttp3.Response.replaceByTest(bounds: TestBounds?, chap: RecordedInteractions?): okhttp3.Response {
    if (bounds == null || chap == null) return this
    val byChap = bounds.replacerData[chap.name] ?: return this

    var bodyContent = body()?.content().orEmpty()
    var newBody = false
    byChap[DataTypes.Body].orEmpty()
        .forEach {
            val matchStr = it.first.match(bodyContent).first
            if (matchStr != null) {
                printlnF(
                    "Replaced:\n== From\n %s\n== To\n %s",
                    matchStr,
                    it.second
                )
                newBody = true
                bodyContent = when {
                    matchStr.isBlank() -> it.second
                    else -> bodyContent.replace(matchStr, it.second)
                }
            }
        }

    // todo; replace response headers

    return if (newBody) {
        newBuilder().apply {
            body(
                ResponseBody.create(
                    body()?.contentType(),
                    bodyContent
                )
            )
        }.build()
    } else this
}
