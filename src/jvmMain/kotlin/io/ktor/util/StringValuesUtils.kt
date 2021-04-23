package io.ktor.util

fun StringValues.flatEntries(): List<Pair<String, String>> {
    return entries().sortedBy { it.key }.flatMap { (key, values) ->
        values.map { key to it }
    }
}
