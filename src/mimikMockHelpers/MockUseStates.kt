package mimikMockHelpers

import helpers.anyTrue

class MockUseStates(val state: Int) {
    companion object {
        val ALWAYS = MockUseStates(-1)
        val DISABLE = MockUseStates(-2)
        val DISABLEDLIMITED = MockUseStates(0)
        /**
         * Single use mock
         */
        val SINGLEMOCK = MockUseStates(1)

        /**
         * Creates a State using the input [state].
         *
         * Note: [default] is used if [state] is not valid.
         */
        fun asState(state: Int, default: MockUseStates = ALWAYS): MockUseStates {
            return MockUseStates(
                if (state in (-2..Int.MAX_VALUE))
                    state else default.state
            )
        }

        /**
         * Returns true if [value] is equal to a state which is disabled
         */
        fun isDisabled(value: Int) = when (value) {
            DISABLE.state, DISABLEDLIMITED.state -> true
            else -> false
        }

        /**
         * Returns true if [value] is a type of enabled state
         */
        fun isEnabled(value: Int): Boolean = !isDisabled(value)

        /**
         * Returns true if [value] is a type of limited state
         */
        fun isLimitedMock(value: Int) = value in (0..Int.MAX_VALUE)

        /**
         * Sets the [state] as a disabled version of itself, if applicable.
         */
        fun asDisabled(state: Int): Int {
            return when (state) {
                -1 -> DISABLE.state
                in (1..Int.MAX_VALUE) -> DISABLEDLIMITED.state
                else -> state
            }
        }
    }

    /**
     * Returns if this state is a type of MOCK state
     */
    val isLimited: Boolean
        get() = state in (0..Int.MAX_VALUE)

    @Suppress("RemoveRedundantQualifierName")
    val isEnabled: Boolean
        get() = anyTrue(
            state == ALWAYS.state,
            state in (1..Int.MAX_VALUE)
        )

    /**
     * Returns this State as a type of MOCK.
     *
     * Note: invalid values set this state as DISABLEDMOCK
     */
    fun asLimited(uses: Int): MockUseStates {
        if (uses == state) return this
        return MockUseStates(
            if (uses in (0..Int.MAX_VALUE))
                uses else 0
        )
    }

    /**
     * Returns this State as type of Disabled
     */
    @Suppress("RemoveRedundantQualifierName")
    val asDisabled: MockUseStates
        get() = if (isLimited) DISABLEDLIMITED else DISABLE
}
