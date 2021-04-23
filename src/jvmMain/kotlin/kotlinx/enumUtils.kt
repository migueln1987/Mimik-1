@file:Suppress("unused")

package kotlinx

inline fun <reified T : Enum<T>> enumContains(name: String?) =
    enumValues<T>().any { it.name == name }

inline fun <reified T : Enum<T>> enumSafeValue(name: String?): T? {
    return if (name == null) null else
        tryOrNull { enumValueOf<T>(name) }
}

inline fun <reified T : Enum<T>> enumValueOf(name: String?, defaultValue: T): T =
    tryOrNull { enumValues<T>().first { it.name == name } } ?: defaultValue
