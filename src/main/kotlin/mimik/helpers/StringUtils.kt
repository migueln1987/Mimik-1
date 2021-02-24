@file:Suppress("unused", "KDocUnresolvedReference")

package mimik.helpers

import com.beust.klaxon.Klaxon
import com.google.gson.*
import java.io.StringReader
import java.util.*

/**
 * Converts the input object into a json string
 */
val Any.toJson: String
    get() = Gson().toJsonTree(this).toString()

/**
 * Converts a JSON string to object
 */
inline fun <reified T> String.fromJson(): T? = Klaxon().let { kx ->
    kx.parseFromJsonObject<T>(kx.parseJsonObject(StringReader(this)))
}

/** Prints the given [message], with optional formatting [args],
 *  to the standard output stream. */
fun printF(message: String, vararg args: Any? = arrayOf()) =
    print(message.format(*args))

/** Prints the given [message], with optional formatting [args],
 * and the line separator to the standard output stream. */
fun printlnF(message: String, vararg args: Any? = arrayOf()) =
    println(message.format(*args))

inline fun printlnF(message: () -> String = { "" }, vararg args: Any? = arrayOf()) =
    println(message.invoke().format(*args))

/**
 * Filter for [toPairs] which removes lines starting with "//"
 */
val removeCommentFilter: (List<String>) -> Boolean
    get() = { !it[0].startsWith("//") }

/**
 * Parses a string, by line and ":" on each line, into String/ String pairs.
 *
 * [allowFilter]: If the value returns true, the item is allowed
 */
@Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
inline fun String?.toPairs(crossinline allowFilter: (List<String>) -> Boolean = { true }): Sequence<Pair<String, String>>? {
    if (this == null) return null

    return split('\n').asSequence()
        .mapNotNull {
            val items = it.split(delimiters = arrayOf(":"), limit = 2)
            if (!allowFilter.invoke(items)) return@mapNotNull null
            when (items.size) {
                1 -> (items[0].trim() to null)
                2 -> items[0].trim() to items[1].trim()
                else -> null
            }
        }
        .filterNot { it.first.isBlank() }
        .filterNot { it.second.isNullOrBlank() }
        .map { it.first to it.second!! }
}

/**
 * Converts a [ByteArray] to a int-aligned hex string
 */
fun ByteArray.toHexString(separator: String = ""): String {
    return this.asSequence()
        .map { it.toInt() }
        .map { if (it < 0) it + 256 else it }
        .map { Integer.toHexString(it) }
        .map { if (it.length < 2) "0$it" else it }
        .joinToString(separator = separator)
}

/**
 * Returned a hex string, 32 bytes per line with spacing between each byte
 */
fun ByteArray.toChunkedHexString(separator: String = " "): String {
    return toHexString("").chunked(32)
        .map { it.chunked(2) }
        .joinToString(separator = "") {
            it.toString()
                .replace(", ", separator)
                .removeSurrounding("[", "]") + "\n"
        }
}
