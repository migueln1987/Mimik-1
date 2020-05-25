package networkRouting.testingManager

import helpers.*
import helpers.parser.P4Action
import helpers.parser.P4Command
import java.time.Duration
import java.util.*
import kolor.red
import kolor.yellow
import kotlinx.atomicfu.atomic
import mimikMockHelpers.RecordedInteractions
import okhttp3.ResponseBody
import kotlin.concurrent.schedule
import tapeItems.BaseTape

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
     * {chapter name}, <data>
     */
    val boundData = mutableMapOf<String, BoundChapterItem>()

    val scopeVars: MutableMap<String, String> = mutableMapOf()
}

class BoundChapterItem(config: (BoundChapterItem) -> Unit = {}) {
    var stateUse: Int? = null // = MockUseStates.ALWAYS.state

    val seqSteps: MutableList<List<P4Command>> = mutableListOf()

    var scopeVars: MutableMap<String, String> = mutableMapOf()

    init {
        config.invoke(this)
    }
}

enum class BoundStates {
    Ready, Started, Stopped, Unknown
}

/**
 * Observes changes to [tape] and routes the interactions to this [TestBounds].
 */
inline fun <reified T : Any?> TestBounds?.observe(tape: BaseTape, watch: () -> T?): T? {
    if (this == null) return watch.invoke()

    val watcherAct: (RecordedInteractions, Int?) -> Int = { chap, value ->
        var data = boundData[chap.name]
        if (data == null) {
            data = BoundChapterItem().also {
                it.stateUse = chap.origionalMockUses ?: chap.mockUses
            }
            boundData[chap.name] = data
        }
        if (data.stateUse == null)
            data.stateUse = chap.origionalMockUses ?: chap.mockUses

        when (value) {
            null -> { // get
                data.stateUse ?: 0
            }

            else -> { // set
                boundData[chap.name]?.stateUse = value
                value
            }
        }
    }

    tape.useWatchers.push(watcherAct)

    val returns = watch.invoke()
    tape.useWatchers.remove(watcherAct)
    return returns
}

fun okhttp3.Response.boundActions(
    request: okhttp3.Request,
    bounds: TestBounds?,
    chap: RecordedInteractions?
): okhttp3.Response {
    if (bounds == null || chap == null) return this
    val byChap = bounds.boundData[chap.name] ?: return this

    val actionClass = P4Action { setup ->
        setup.testBounds = bounds
        setup.chapBounds = byChap
        setup.in_headers = request.headers()
        setup.in_body = request.body()?.content().orEmpty()
        setup.out_headers = headers()
        setup.out_body = body()?.content().orEmpty()
    }

    byChap.seqSteps.forEach { steps ->
        actionClass.processCommands(steps)
    }

    return newBuilder().apply {
        headers(actionClass.out_headers)
        body(
            ResponseBody.create(
                body()?.contentType(),
                actionClass.out_body
            )
        )
    }.build()
}
