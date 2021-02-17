package kotlinx.html

import kotlinUtils.ensureSuffix
import kotlinUtils.isThrow

/**
 * Appends the data in [values] to the current [style].
 *
 * - Items are added in "key: value" format
 * - If an item in [values] does not end with ";", one will be added
 */
fun CommonAttributeGroupFacade.appendStyles(vararg values: String) {
    val builder = StringBuilder()
    values.forEach {
        builder.append(it.trim().ensureSuffix(";"))
    }

    if (isThrow { style })
        style = builder.toString()
    else
        style += builder.toString()
}

/**
 * Appends the data in [values] to the current object's [classes]
 */
fun CommonAttributeGroupFacade.appendClass(vararg values: String) {
    classes = classes.toMutableSet().apply { addAll(values) }
}

/**
 * Get/Set a property in the styles.
 *
 * - Set; non-null second value
 * - Get; second param as `null`
 */
fun CommonAttributeGroupFacade.styleProxy(key: String, value: String?): String {
    return when (value) {
        null -> when {
            isThrow { style } -> ""
            else -> styleRegex(key).find(style)?.value ?: ""
        }
        else -> when {
            isThrow { style } -> "$key: $value".ensureSuffix(";").also { style = it }
            else -> {
                val grab = styleRegex(key).find(style)?.groups?.get(1)
                style = when (grab) {
                    null -> style + "$key: $value".ensureSuffix(";")
                    else -> style.replaceRange(grab.range, value)
                }
                style
            }
        }
    }
}

/**
 * Creates a regex to find the requested key:value
 */
val styleRegex: (String) -> Regex = { "(?:^|[ ;])$it: *(.+?);".toRegex() }
