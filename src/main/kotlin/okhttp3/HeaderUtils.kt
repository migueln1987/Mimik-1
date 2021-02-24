package okhttp3

import io.ktor.http.*
import io.ktor.util.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Returns the [Headers] or default headers containing {ContentType: "* / *"}
 */
val Headers?.orDefault: Headers
    get() = this ?: Headers.headersOf(HttpHeaders.ContentType, ContentType.Any.toString())

/**
 * toMultiMap function which can preserve the case of header keys
 */
fun Headers.toMultimap(caseSensitive: Boolean): Map<String, List<String>> {
    if (!caseSensitive) return this.toMultimap()

    val result = TreeMap<String, ArrayList<String>>()
    (0 until size).forEach { i ->
        val name = name(i)
        if (!result.containsKey(name))
            result[name] = ArrayList()
        val data = result.getValue(name)
        data.add(value(i))
    }

    return result
}

val StringValues.toHeaders: Headers
    get() {
        return Headers.Builder().also { build ->
            entries().forEach { entry ->
                entry.value.forEach { value ->
                    build.add(entry.key, value)
                }
            }
        }.build()
    }

val Map<String, String>.toHeaders: Headers
    get() {
        return Headers.Builder().also { build ->
            forEach { entry ->
                build.add(entry.key, entry.value)
            }
        }.build()
    }

/**
 * Creates a [Headers] which can have multiple different values to the same key
 */
val Map<String, List<String>>.toHeaders_dupKeys: Headers
    get() {
        return Headers.Builder().also { build ->
            forEach { (key, values) ->
                values.forEach { value ->
                    build.add(key, value)
                }
            }
        }.build()
    }

val Iterable<Pair<String, String>>.toHeaders: Headers
    get() {
        return Headers.Builder()
            .also { build -> forEach { build.add(it.first, it.second) } }
            .build()
    }

/**
 * Returns the value of [Headers] if it contains any values, or [null]
 */
val Headers.valueOrNull: Headers?
    get() = if (size > 0) this else null

fun Headers.contains(key: String, value: String) = values(key).contains(value)

/** Returns an immutable (optional) case-sensitive set of header names. */
fun Headers.names(caseSensitive: Boolean): Set<String> {
    return if (caseSensitive) {
        (0 until size).map { name(it) }.toSet()
    } else names()
}

/**
 * Converts the [Headers] into a list of Key/Value pairs
 */
val Headers.toPairs: List<Pair<String, String>>
    get() = toMultimap().asSequence()
//        .filter { it.key != null }
        .flatMap { kv ->
            kv.value.asSequence().map { kv.key to it }
        }
        .toList()

val Headers.toArray: Array<String>
    get() {
        val result = mutableListOf<String>()
        toPairs.forEach {
            result.add(it.first)
            result.add(it.second)
        }
        return result.toTypedArray()
    }

/**
 * Returns this [Headers] as a list of "Key: Value", or user defined [format]
 */
inline fun Headers.toStringPairs(
    format: (Pair<String, String>) -> String = { "${it.first}: ${it.second}" }
) = toPairs.map(format)

fun Headers.asIterable() = toPairs.asIterable()
