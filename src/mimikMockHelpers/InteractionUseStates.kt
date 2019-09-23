package mimikMockHelpers

import helpers.anyTrue

class InteractionUseStates(val state: Int) {
    companion object {
        val ALWAYS = InteractionUseStates(-1)
        val DISABLE = InteractionUseStates(-2)
        val DISABLEDMOCK = InteractionUseStates(0)
        /**
         * Single use mock
         */
        val SINGLEMOCK = InteractionUseStates(1)

        /**
         * Creates a State using the input [state].
         *
         * Note: [default] is used if [state] is not valid.
         */
        fun asState(state: Int, default: InteractionUseStates = ALWAYS): InteractionUseStates {
            return InteractionUseStates(
                if (state in (-2..Int.MAX_VALUE))
                    state else default.state
            )
        }
    }

    /**
     * Returns if this state is a type of MOCK state
     */
    val isMock: Boolean
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
    fun asMock(uses: Int): InteractionUseStates {
        if (uses == state) return this
        return InteractionUseStates(
            if (uses in (0..Int.MAX_VALUE))
                uses else 0
        )
    }

    /**
     * Returns this State as type of Disabled
     */
    @Suppress("RemoveRedundantQualifierName")
    val asDisabled: InteractionUseStates
        get() = if (isMock) DISABLEDMOCK else DISABLE
}
