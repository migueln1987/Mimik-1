package io.ktor.http

import io.ktor.util.*
import java.util.*

/**
 * Returns the first values for each key
 */
val Parameters.toSingleMap: Map<String, String>
    get() = toMap().mapValues { it.value.firstOrNull().orEmpty() }

/**
 * Ignores items starting with "$";
 *
 * example: $RequestHost and $RequestPort
 */
fun Parameters.ignoreHostItems(): Parameters =
    filter { s, _ -> !s.startsWith("$") }.toParameters

val StringValues.toParameters: Parameters
    get() = Parameters.build { appendAll(this@toParameters) }

/**
 * Limits the input [StringValues] to only those with the following [keys]
 */
fun StringValues.limitKeys(vararg keys: String, allowDuplicates: Boolean = false): Parameters {
    val limitParams: MutableList<String> = mutableListOf()

    return filter { s, _ ->
        s.lowercase().let { pKey ->
            if (keys.contains(pKey)) {
                if (limitParams.contains(pKey) && !allowDuplicates)
                    return@filter false
                limitParams.add(pKey)
                return@filter true
            } else false
        }
    }.toParameters
}
