package helpers

import com.google.gson.Gson
import com.google.gson.GsonBuilder

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
 * If this string does not end with the given [suffix],
 * then the string is returned with [value] added.
 * Else the original string is returned.
 */
fun String.ensureSufix(suffix: String, value: String? = null) =
    if (endsWith(suffix)) this else this + (value ?: suffix)

/**
 * Returns the last instance range of the matching [predicate].
 *
 * If no match is found, [(0..0)] is returned.
 */
fun String.lastIndexRange(predicate: (String) -> String): IntRange {
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
 * Returns `true` if the contents of this string is equal to the word "true", ignoring case, and `false` otherwise.
 *
 * If the value is null, [default] is returned instead, which is initially 'false'
 */
fun String?.isTrue(default: Boolean = false) = this?.toBoolean() ?: default

/**
 * Returns [true] if the input is a valid json
 */
val String?.isJSONValid: Boolean
    get() = try {
        val adjustedString = this?.replace("\\n", "")
        Gson().fromJson(adjustedString, Any::class.java)
        true
    } catch (ex: Exception) {
//        println("= isJSONValid =\n $ex")
        false
    }

/**
 * Returns an empty string if the json is valid, or the error message
 */
val String?.isJSONValidMsg: String
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
 * Converts the source [String] into a indent-formatted string
 */
val String?.beautifyJson: String
    get() {
        val gson = GsonBuilder().setPrettyPrinting().create()
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
 * and the line separator to the standard output stream. */
@Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
fun println(message: String, vararg args: Any? = arrayOf()) =
    System.out.println(message.format(*args))
