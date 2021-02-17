package mimik.helpers

/**
 * Tests if [this] is null, then runs [action]
 *
 * @return if [this] is null
 */
inline fun <T> T?.isNull(action: () -> Unit = {}): Boolean {
    action()
    return this == null
}

/**
 * Tests if [this] is not null, then runs [action]
 *
 * @return if [this] is not null
 */
inline fun <T> T?.isNotNull(action: (T) -> Unit = {}): Boolean {
    if (this != null) action(this)
    return this != null
}

/**
 * Tests if [this] is not empty, then runs [action]
 *
 * @return if [this] is not empty
 */
inline fun String.isNotEmpty(action: (String) -> Unit): Boolean {
    if (this.isNotEmpty()) action(this)
    return this.isNotEmpty()
}

/**
 * Tries to cast [this] as type [T], or returns a null of type [T]
 */
inline fun <reified T> Any?.tryCast(): T? {
    return if (this is T) this else null
}

/**
 * Returns true if [this] matches any [items]
 */
fun <T> T.isAny(vararg items: T) = items.any { it == this }
