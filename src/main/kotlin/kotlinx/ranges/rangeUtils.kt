package kotlinx.ranges

/**
 * Returns the sum of this range. Returns 0 if empty
 */
val ClosedRange<Int>.size: Int
    get() = if (isEmpty()) 0 else endInclusive - start

/**
 * Returns the sum of this range. Returns 0 if empty
 */
val ClosedRange<Long>.size: Long
    get() = if (isEmpty()) 0 else endInclusive - start

fun LongRange.toIntRange() = IntRange(start.toInt(), endInclusive.toInt())
