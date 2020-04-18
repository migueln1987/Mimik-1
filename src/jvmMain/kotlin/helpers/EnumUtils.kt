package helpers

inline fun <reified T : Enum<T>> enumContains(name: String?) =
    enumValues<T>().any { it.name == name }

inline fun <reified T : Enum<T>> enumSafeValue(name: String?): T? {
    if (name == null) return null
    return try {
        enumValueOf<T>(name)
    } catch (_: Exception) {
        null
    }
}

inline fun <reified T : Enum<T>> enumValueOf(name: String?, defaultValue: T): T {
    return try {
        enumValues<T>().first { it.name == name }
    } catch (e: NoSuchElementException) {
        defaultValue
    }
}
