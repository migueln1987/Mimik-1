package helpers

/**
 * Loops through the [Iterable] doing [action] on each item.
 *
 * If there's an item after the current action, then [hasNext] is also called.
 */
fun <T> Iterable<T>.eachHasNext(action: (T) -> Unit, hasNext: () -> Unit = {}) {
    val iterator = iterator()
    while (iterator.hasNext()) {
        action.invoke(iterator.next())
        if (iterator.hasNext())
            hasNext.invoke()
    }
}
