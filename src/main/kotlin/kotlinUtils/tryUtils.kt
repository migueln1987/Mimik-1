package kotlinUtils

/**
 * Tests if the following [action] would throw an [Exception].
 * Also safely handles throws.
 */
inline fun isThrow(action: () -> Unit = {}): Boolean {
    return try {
        action.invoke()
        false
    } catch (e: Exception) {
        true
    }
}

/*
 * Attempts to get data from [action], or returns a null.
 * Throws return null.
 */
inline fun <T> tryOrNull(
    printStackTrace: Boolean = false,
    action: () -> T?
): T? {
    return try {
        action.invoke()
    } catch (e: Exception) {
        if (printStackTrace)
            e.printStackTrace()
        null
    }
}
