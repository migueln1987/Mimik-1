package kotlinx

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.utils.io.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.*

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
fun String.removePrefixes(vararg prefixes: CharSequence) =
    prefixes.fold(this) { acc, t -> acc.removePrefix(t) }

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
 * Appends the prefix "https://" if none is found
 */
val String.ensureHttpsPrefix: String
    get() = ensurePrefix("https", "https://")

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
    val regex = predicate(this).toRegex()
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
        else lines().maxByOrNull { it.length }
    }

/**
 * Returns `true` if the contents of this string is equal to the word "true", ignoring case, and `false` otherwise.
 *
 * If the value is null, [default] is returned instead, which is initially 'false'
 */
fun String?.isStrTrue(default: Boolean = false) = this?.toBoolean() ?: default

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
 * Returns true if this [String] is a valid Url
 */
val String?.isValidURL: Boolean
    get() = this.orEmpty().toHttpUrlOrNull() != null

/**
 * Attempts to convert the [String] into a [HttpUrl]
 */
val String?.asHttpUrl: HttpUrl?
    get() = this.orEmpty().ensureHttpPrefix.toHttpUrlOrNull()

/**
 * Converts the source [String] into a indent-formatted string
 */
val String?.beautifyJson: String
    get() {
        if (this.isNullOrBlank()) return ""

        return tryOrNull {
            val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

            gson.let {
                it.fromJson(this, Any::class.java)
                    ?.let { toObj -> it.toJson(toObj) }
                    ?: this
            }
        }.orEmpty()
    }

/**
 * Returns the source, or "{empty}" if empty
 */
val String.valueOrIsEmpty: String
    get() = if (this.isEmpty()) "{empty}" else this

/**
 * Returns [this] string, unless [this] is null/ blank/ empty
 */
val String?.valueOrNull: String?
    get() = if (this.isNullOrBlank()) null else this

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

/** Converts a string to base64 */
val String?.toBase64: String
    get() = Base64.getEncoder().encodeToString(this.orEmpty().toByteArray())

/** Converts a base64 string back to the original string */
val String?.fromBase64: String
    get() = String(Base64.getDecoder().decode(this.orEmpty()))

/**
 * Returns a string capped at the requested line count [limit].
 *
 * Note: If the input is being capped, the last line (at [limit] index)
 * will say "...[###] lines" with "###" being the remaining lines
 */
fun String.limitLines(limit: Int): String {
    val lines = lines()
    return if (lines.size > limit)
        lines.take(limit).joinToString(
            separator = "",
            transform = { "$it\n" }) + "...[${lines.size - limit} lines]"
    else this
}

/** Prints the given [message] and the line separator to the standard output stream. */
fun String.println() = println(this)

fun String.toByteReadChannel() = ByteReadChannel(toByteArray())
