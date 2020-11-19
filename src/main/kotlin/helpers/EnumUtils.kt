@file:Suppress("unused")

package helpers

inline fun <reified T : Enum<T>> enumContains(name: String?) =
    enumValues<T>().any { it.name == name }

inline fun <reified T : Enum<T>> enumSafeValue(name: String?): T? {
    if (name == null) return null
    return tryOrNull { enumValueOf<T>(name) }
}

inline fun <reified T : Enum<T>> enumValueOf(name: String?, defaultValue: T): T =
    tryOrNull { enumValues<T>().first { it.name == name } } ?: defaultValue
