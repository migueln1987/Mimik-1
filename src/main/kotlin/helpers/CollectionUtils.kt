package helpers

import kotlin.collections.ArrayList

/**
 * Loops through the [Iterable] doing [action] on each item.
 *
 * If there's an item after the current action, then [hasNext] is also called.
 */
inline fun <T> Iterable<T>.eachHasNext(action: (T) -> Unit, hasNext: () -> Unit = {}) {
    val iterator = iterator()
    while (iterator.hasNext()) {
        action.invoke(iterator.next())
        if (iterator.hasNext())
            hasNext.invoke()
    }
}

/**
 * Returns a map containing only the non-null results of applying the given [transform] function
 * to each entry in the original map.
 */
inline fun <T, V, R> Map<T, V>.mapNotNullToMap(transform: (Map.Entry<T, V>) -> R?): Map<T, R> {
    val result = mutableMapOf<T, R>()
    forEach { element ->
        transform(element)?.also { result[element.key] = it }
    }
    return result
}

/**
 * Subtracts [other] from this, then remaps the values based on [transform]
 */
inline fun <T, V, oV, R> Map<T, V>.subtractMap(
    other: Map<T, oV>,
    transform: (Map.Entry<T, V>) -> R?
) = filterNot { other.keys.contains(it.key) }.mapNotNullToMap(transform)

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
 * Returns an array of Byte containing all of the elements of this collection.
 */
fun Collection<Int>.toByteArray(): ByteArray = map { it.toByte() }.toByteArray()

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

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
inline fun <T, R : Any> Iterable<T>.firstNotNullResult(predicate: (T) -> R?): R? =
    this.iterator().firstNotNullResult(predicate)

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 *
 * The operation is _terminal_.
 */
inline fun <T, R : Any> Sequence<T>.firstNotNullResult(predicate: (T) -> R?): R? =
    this.iterator().firstNotNullResult(predicate)

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
inline fun <T, R : Any> Iterator<T>.firstNotNullResult(predicate: (T) -> R?): R? {
    for (element in this) {
        val result = predicate(element)
        if (result != null) return result
    }
    return null
}

inline fun <T, R : Any> Iterable<T>.lastNotNullResult(predicate: (T) -> R?): R? {
    var elem: R? = null
    for (element in this) {
        elem = predicate(element) ?: elem
    }
    return elem
}

/**
 * Returns the first item in [this] which matches a [predicates]
 */
fun <T> Iterable<T>.firstMatchNotNull(vararg predicates: (T) -> Boolean): T? {
    for (p in predicates) {
        val element = firstOrNull { p.invoke(it) }
        if (element != null) return element
    }
    return null
}

/**
 * Appends each list in [lists] that isn't null
 */
fun <T> Iterable<T>.appendNotNull(vararg lists: List<T>?): List<T> =
    this.toList() + lists.filterNotNull().flatten()

/**
 * Returns a new [ArrayList] filled with all elements of this collection.
 */
fun <T> List<T>.toArrayList() = ArrayList(this)

/**
 * Returns true if this collection contains an item at [index]
 */
fun <T> Collection<T>.hasIndex(index: Int) = index < size

/**
 * For each Key/Value pair in [from], if it's not already contained in this map, then it is added
 */
fun <K, V> MutableMap<K, V>.putIfAbsent(from: Map<K, V>) {
    from.forEach { (key, value) ->
        putIfAbsent(key, value)
    }
}
