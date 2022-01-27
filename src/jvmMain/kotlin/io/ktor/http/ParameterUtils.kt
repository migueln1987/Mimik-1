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
    val limitParams = mutableListOf<String>()

    return filter { s, _ ->
        s.lowercase().let { pKey ->
            when {
                !keys.contains(pKey) -> false
                limitParams.contains(pKey) && !allowDuplicates -> false
                else -> {
                    limitParams.add(pKey)
                    true
                }
            }
        }
    }.toParameters
}
