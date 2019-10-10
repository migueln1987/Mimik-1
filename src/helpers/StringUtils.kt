package helpers

import com.google.gson.Gson

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
 * If this string does not start with the given [prefix],
 * then the string is returned with the prefix added.
 * Else the original string is returned.
 */
fun String.ensurePrefix(prefix: String) =
    if (startsWith(prefix)) this else prefix + this

/**
 * If this string starts with the given [prefix] (in order of input), returns a copy of this string
 * with the prefix removed. Otherwise, returns this string.
 */
fun String.removePrefixs(vararg prefixs: CharSequence) =
    prefixs.fold(this) { acc, t -> acc.removePrefix(t) }

/**
 * If this string does not start with the given [prefix],
 * then the string is returned with [value] added.
 * Else the original string is returned.
 */
fun String.ensurePrefix(prefix: String, value: String) =
    if (startsWith(prefix)) this else value + this

/**
 * Returns `true` if the contents of this string is equal to the word "true", ignoring case, and `false` otherwise.
 *
 * If the value is null, [default] is returned instead, which is initially 'false'
 */
fun String?.isTrue(default: Boolean = false) = this?.toBoolean() ?: default

val String?.isJSONValid: Boolean
    get() = try {
        val adjustedString = this?.replace("\\n", "")
        Gson().fromJson(adjustedString, Any::class.java)
        true
    } catch (ex: Exception) {
        println("= isJSONValid =\n $ex")
        false
    }

val String?.isJSONValidMsg: String
    get() = try {
        val adjustedString = this?.replace("\\n", "")
        Gson().fromJson(adjustedString, Any::class.java)
        ""
    } catch (ex: Exception) {
        ex.toString()
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
