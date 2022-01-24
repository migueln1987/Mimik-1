package mimik.networkRouting.testingManager

import javax.util.plus
import kolor.red
import kolor.yellow
import kotlinx.println
import mimik.helpers.parser.P4Action
import mimik.mockHelpers.RecordedInteractions
import mimik.mockHelpers.SeqActionObject
import mimik.tapeItems.BaseTape
import mimik.tapeItems.MimikContainer
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.content
import okhttp3.contents
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.schedule

/**
 * Self-contained sandboxed testing environment
 */
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
                    isEnabled.set(false)
                }
                field -> {
                    println("Re-assigning test bound -> Resetting time".yellow())

                    val (tStart, tEnd) = startTest()
                    buildString {
                        appendLine("Reset test bounds")
                        appendLine("- Handle: $handle")
                        appendLine("- Target ID: ($value)")
                        appendLine("- From: $tStart")
                        append("- To:   $tEnd")
                    }.yellow().println()
                }
                else -> {
                    field = value

                    val (tStart, tEnd) = startTest()

                    buildString {
                        appendLine("Starting test bounds")
                        appendLine("- Handle: $handle")
                        appendLine("- Target ID: ($value)")
                        appendLine("- From: $tStart")
                        append("- To:   $tEnd")
                    }.yellow().println()
                }
            }
        }

    /**
     * Limit of when the bounds's [expireTime] will expire.
     *
     * Value is in seconds
     */
    var timeLimit: Duration = Duration.ofSeconds(5)

    /**
     * List of all the Tapes this TestBound is allowed to use
     */
    val enabledTableList by lazy {
        if (tapes.contains("##All"))
            MimikContainer.tapeCatalog.tapes.map { it.name }
        else tapes
    }

    private fun startTest(): Pair<String, String> {
        if (tapes.contains("##All")) {
            // for unit tests only, lazy adds ALL the known tapes
            tapes.clear()
            tapes.addAll(MimikContainer.tapeCatalog.tapes.map { it.name })
        }
        isEnabled.set(true)
        expireTimer?.cancel()
        startTime = Date()
        val startStr = startTime.toString()
        expireTime = (startTime!! + timeLimit).also {
            expireTimer = Timer("Handle: $handle", false).schedule(it) {
                println("Test bounds ($handle) has expired".red())
                isEnabled.set(false)
            }
        }

        return startStr to expireTime.toString()
    }

    fun stopTest() {
        expireTimer?.cancel()
        isEnabled.set(false)
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
                !isEnabled.get() -> BoundStates.Stopped
                expireTime == null -> BoundStates.Ready
                expireTime != null -> BoundStates.Started
                else -> BoundStates.Unknown
            }
        }

    var isEnabled = AtomicBoolean(true)
        private set

    var finalized = false

    /**
     * Collection of artifacts which define test bound chapters
     *
     * {chapter name}, <data>
     */
    val boundData = mutableMapOf<String, BoundChapterItem>()

    /**
     * Variables which persist across the whole state of this test
     */
    val scopeVars: MutableMap<String, String> = mutableMapOf()
}

/**
 * Configuration which is applied onto a chapter during test bound execution.
 * This class is also used as a holding area for non-test bound action variables
 *
 * - [stateUse]; The active state declared for this chapter during usage
 * - [seqSteps]; Actions, in test execution, which are applied after the base chapter's actions
 * - [scopeVars]; Variables which are scoped to this chapter
 */
class BoundChapterItem(config: (BoundChapterItem) -> Unit = {}) {
    /**
     * Active state of this chapter
     */
    var stateUse: Int? = null // = MockUseStates.ALWAYS.state

    /**
     * Actions which are run during chapter execution.
     *
     * Note; These actions are run AFTER the base chapter's actions
     */
    val seqSteps: MutableList<SeqActionObject> = mutableListOf()

    /**
     * Holding variable which are at the [chapter level].
     *
     * These variables persist across actions which are in the same chapter
     */
    var scopeVars: MutableMap<String, String> = mutableMapOf()

    init {
        config(this)
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
        val data = boundData.getOrPut(chap.name) { BoundChapterItem() }

        if (data.stateUse == null)
            data.stateUse = chap.originalMockUses ?: chap.mockUses

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
    if (chap == null) return this
    val activeBounds = bounds ?: TestBounds("")
    val boundChapData = activeBounds.boundData[chap.name] ?: BoundChapterItem()

    val actionClass = P4Action { setup ->
        setup.testBounds = activeBounds
        setup.chapBounds = boundChapData
        setup.in_headers = request.headers
        setup.in_body = request.body?.content().orEmpty()
        setup.out_headers = headers
        setup.out_body = body?.contents().orEmpty()
    }

    /* Process actions
    - Tape (pending)
    - Chapter "mock"
    - Test bound
    */

//   val chapTape =  TapeCatalog.Instance.tapes.first { it.chapters.any { it == chap } }

    chap.seqActions?.forEach { seqBlocks ->
        actionClass.processCommands(seqBlocks.Commands)
    }

    boundChapData.seqSteps.forEach { seqBlocks ->
        actionClass.processCommands(seqBlocks.Commands)
    }

    return newBuilder().apply {
        headers(actionClass.out_headers)
        body(
            actionClass.out_body.toResponseBody(body?.contentType())
        )
    }.build()
}
