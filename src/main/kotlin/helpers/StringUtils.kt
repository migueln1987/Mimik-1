package helpers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.HttpUrl

/**
 * If this string starts with the given [prefix], returns a copy of this string
 * with the prefix removed. Otherwise, returns this string.
 *
 *  @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 */
fun String.removePrefix(prefix: String, ignoreCase: Boolean): String {
    return if (startsWith(prefix, ignoreCase))
        substring(prefix.length, length)
    else this
}

/**
 * Returns the string with only the first letter in Upper case
 */
fun String.uppercaseFirstLetter() = take(1).toUpperCase() + drop(1)

/**
 * If this string starts with the given [prefix] (in order of input), returns a copy of this string
 * with the prefix removed. Otherwise, returns this string.
 */
fun String.removePrefixs(vararg prefixs: CharSequence) =
    prefixs.fold(this) { acc, t -> acc.removePrefix(t) }

/**
 * If this string does not start with the given [prefix],
 * then the string is returned with [value] (or [prefix])  added.
 * Else the original string is returned.
 */
fun String.ensurePrefix(prefix: String, value: String? = null) =
    if (startsWith(prefix)) this else (value ?: prefix) + this

/**
 * Appends the prefix "http://" if none is found
 */
val String.ensureHttpPrefix: String
    get() = ensurePrefix("http", "http://")

/**
 * If this string does not end with the given [suffix],
 * then the string is returned with [value] added.
 * Else the original string is returned.
 */
fun String.ensureSuffix(suffix: String, value: String? = null) =
    if (endsWith(suffix)) this else this + (value ?: suffix)

/**
 * Returns the last instance range of the matching [predicate].
 *
 * If no match is found, [(0..0)] is returned.
 */
inline fun String.lastIndexRange(predicate: (String) -> String): IntRange {
    val regex = predicate.invoke(this).toRegex()
    val matches = regex.findAll(this)
    if (matches.none()) return (0..0)
    return matches.last().range
}

/**
 * Returns a substring specified by the given [range] of indices.
 *
 * If [range]'s last value is less than or equal to 0, [default] is returned.
 */
fun String.substring(range: IntRange, default: String) =
    if (range.last <= 0) default else substring(range)

/**
 * Adds each [strings] to [this], with a new line between each value and the source string.
 */
fun String.appendLines(vararg strings: String) =
    strings.fold(this) { acc, t -> "$acc\n$t" }

/**
 * Returns the longest line (separated line line breaks) in this string
 */
val String?.longestLine: String?
    get() {
        return if (this == null) this
        else lines().maxBy { it.length }
    }

/**
 * Returns `true` if the contents of this string is equal to the word "true", ignoring case, and `false` otherwise.
 *
 * If the value is null, [default] is returned instead, which is initially 'false'
 */
fun String?.isTrue(default: Boolean = false) = this?.toBoolean() ?: default

/**
 * Returns [true] if the input is a valid json
 */
val String?.isValidJSON: Boolean
    get() = !isThrow {
        val adjustedString = this?.replace("\\n", "")
        Gson().fromJson(adjustedString, Any::class.java)
    }

/**
 * Tries to [beautifyJson] the input if it's a valid json, else returns the input
 */
val String?.tryAsPrettyJson: String?
    get() = if (isValidJSON) beautifyJson else this

/**
 * Returns an empty string if the json is valid, or the error message
 */
val String?.isValidJSONMsg: String
    get() = try {
        val adjustedString = this?.replace("\\n", "")
        Gson().fromJson(adjustedString, Any::class.java)
        ""
    } catch (ex: Exception) {
        ex.toString()
    }

/**
 * Converts a [String] to a valid *.json string
 */
val String.toJsonName: String
    get() = replace(" ", "_")
        .replace("""/(\w)""".toRegex()) {
            it.groups[1]?.value?.toUpperCase() ?: it.value
        }
        .replace("/", "")
        .replace(".", "-")
        .plus(".json")

/**
 * Converts the input object into a json string
 */
val Any.toJson: String
    get() = Gson().toJsonTree(this).toString()

/**
 * Returns true if this [String] is a valid Url
 */
val String?.isValidURL: Boolean
    get() = HttpUrl.parse(this.orEmpty()) != null

/**
 * Attempts to convert the [String] into a [HttpUrl]
 */
val String?.asHttpUrl: HttpUrl?
    get() = HttpUrl.parse(this.orEmpty().ensureHttpPrefix)

private val gson by lazy { GsonBuilder().setPrettyPrinting().create() }

/**
 * Converts the source [String] into a indent-formatted string
 */
val String?.beautifyJson: String
    get() {
        if (this.isNullOrBlank()) return ""

        return try {
            gson.let {
                it.fromJson(this, Any::class.java)
                    ?.let { toObj -> it.toJson(toObj) }
                    ?: this
            }
        } catch (e: java.lang.Exception) {
            ""
        }
    }

/**
 * Returns the source, or "{empty}" if empty
 */
val String.valueOrIsEmpty: String
    get() = if (this.isEmpty()) "{empty}" else this

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
inline fun String?.toPairs(crossinline allowFilter: (List<String>) -> Boolean = { true }): Sequence<Pair<String, String>>? {
    if (this == null) return null

    return split('\n').asSequence()
        .mapNotNull {
            val items = it.split(delimiters = *arrayOf(":"), limit = 2)
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
 * Appends multiple [lines] to this [StringBuilder]
 */
fun StringBuilder.appendlns(vararg lines: String) =
    lines.forEach { appendln(it) }

/**
 * Appends [message] to this [StringBuffer] with the optional formatting [args]
 */
fun StringBuilder.appendlnFmt(message: String, vararg args: Any? = arrayOf()) =
    appendln(message.format(*args))

/**
 * Appends [message] to this [StringBuffer] with the optional formatting [args]
 */
inline fun StringBuilder.appendlnFmt(message: () -> String, vararg args: Any? = arrayOf()) =
    appendln(message.invoke().format(*args))

/**
 * Returns if this [String] is a type of Base64
 *
 * - length is a multiple of 4
 * - chars are within the valid type range
 */
val String?.isBase64: Boolean
    get() {
        return if (this == null || (length % 4 != 0)) false
        else "[a-z\\d/+]+={0,2}".toRegex(RegexOption.IGNORE_CASE)
            .matches(this)
    }

fun String.limitLines(limit: Int): String {
    val lines = lines()
    return if (lines.size > limit)
        lines.take(limit).joinToString(
            separator = "",
            transform = { "$it\n" }) + "...[${lines.size - limit} lines]"
    else this
}
