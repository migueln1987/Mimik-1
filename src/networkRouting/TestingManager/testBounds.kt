package networkRouting.TestingManager

import helpers.add
import tapeItems.BlankTape
import java.util.Calendar
import java.util.Date

data class testBounds(var handle: String, val tapes: List<String>) {
    var boundSource: String? = null
        set(value) {
            field = value
            expireTime = Date().add(Calendar.SECOND, timeLimit)
        }

    /**
     * Limit of when the bounds's [expireTime] will expire.
     *
     * Value is in seconds
     */
    var timeLimit = 5

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

inline fun <reified T : Any?> testBounds?.observe(tape: BlankTape, watch: () -> T?): T? {
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
