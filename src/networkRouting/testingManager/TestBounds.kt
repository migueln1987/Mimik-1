package networkRouting.testingManager

import helpers.plus
import helpers.printlnF
import java.time.Duration
import java.util.*
import kolor.red
import kolor.yellow
import kotlin.concurrent.schedule
import tapeItems.BlankTape

data class TestBounds(var handle: String, val tapes: MutableList<String>) {
    var expireTimer: TimerTask? = null
        @Synchronized get
        @Synchronized private set

    var boundSource: String? = null
        @Synchronized
        set(value) {
            when (value) {
                null -> {
                    field = null
                    expireTime = null
                    isEnabled = false
                }
                field -> {
                    println("Re-assigning test bound -> Resetting time".yellow())
                    expireTimer?.cancel()

                    val startTime = Date()
                    val startStr = startTime.toString()
                    expireTime = (startTime + timeLimit).also {
                        expireTimer = Timer("Handle: $handle", false).schedule(it) {
                            println("Test bounds ($handle) has expired".red())
                        }
                    }

                    printlnF(
                        ("Reset test bounds\n" +
                                "- Handle: %s\n" +
                                "- Target ID: (%s)\n" +
                                "- From: %s\n" +
                                "- To:   %s").yellow(),
                        handle,
                        value,
                        startStr,
                        expireTime.toString()
                    )
                }
                else -> {
                    field = value
                    expireTimer?.cancel()
                    val startTime = Date()
                    val startStr = startTime.toString()
                    expireTime = (startTime + timeLimit).also {
                        expireTimer = Timer("Handle: $handle", false).schedule(it) {
                            println("Test bounds ($handle) has expired".red())
                        }
                    }

                    printlnF(
                        ("Starting test bounds\n" +
                                "- Handle: %s\n" +
                                "- Target ID: (%s)\n" +
                                "- From: %s\n" +
                                "- To:   %s").yellow(),
                        handle,
                        value,
                        startStr,
                        expireTime.toString()
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

    /**
     * Time of when this bounds expires
     */
    var expireTime: Date? = null
        private set

    val state: BoundStates
        get() {
            return when {
                !isEnabled -> BoundStates.Stopped
                boundSource.isNullOrBlank() -> BoundStates.Ready
                expireTime != null -> BoundStates.Started
                else -> BoundStates.Unknown
            }
        }

    var isEnabled = true

    /**
     * {tape name}, <{chapter name}, uses>
     */
    val stateUses =
        mutableMapOf<String, MutableList<Pair<String, Int>>>()
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

    tape.useWatcher = { chap, value ->
        var data = stateUses[tape.name]
            ?.firstOrNull { it.first == chap.name }

        when (value) {
            null -> { // set
                if (data == null) {
                    data = (chap.name to (chap.origionalMockUses ?: chap.mockUses))
                    stateUses[tape.name]?.add(data)
                }

                data.second
            }

            else -> { // get
                if (data != null)
                    stateUses[tape.name]?.removeIf { it == data }

                stateUses[tape.name]?.add(
                    (chap.name to value)
                )
                value
            }
        }
    }

    val returns = watch.invoke()
    tape.useWatcher = null
    return returns
}
