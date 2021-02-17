@file:Suppress("unused")

package mimik.helpers

import kotlinUtils.collections.firstNotNullResult

/**
 * Returns the elements yielding the largest value of the given function.
 */
inline fun <T, R : Comparable<R>> Sequence<T>.filterByMax(crossinline selector: (T) -> R): Sequence<T> {
    var maxValue: R? = null

    return map {
        val v = selector(it)
        if (maxValue == null || v >= maxValue!!)
            maxValue = v
        it to v
    }.toList()
        .asSequence()
        .filter { maxValue != null && it.second >= maxValue!! }
        .map { it.first }
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 *
 * The operation is _terminal_.
 */
inline fun <T, R : Any> Sequence<T>.firstNotNullResult(predicate: (T) -> R?): R? =
    this.iterator().firstNotNullResult(predicate)
