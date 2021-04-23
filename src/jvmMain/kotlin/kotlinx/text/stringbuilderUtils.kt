package kotlinx.text

/**
 * Appends multiple [lines] to this [StringBuilder]
 */
fun StringBuilder.appendLines(vararg lines: String): StringBuilder {
    lines.forEach { appendLine(it) }
    return this
}

/**
 * The action in [valueAction] is applied to [value].
 * If the result of [valueAction] is a string,
 * then it will be appended and followed by a line separator.
 */
fun StringBuilder.appendLine(value: String = "", valueAction: StringBuilder.(String) -> Any): StringBuilder {
    valueAction(this, value).also { if (it is String) appendLine(it) }
    return this
}

/**
 * The action in [valueAction] is applied to [value],
 * then the result is appended to this [StringBuilder]
 */
inline fun StringBuilder.append(
    value: String = "",
    preAppend: String = "",
    postAppend: String = "",
    valueAction: StringBuilder.(String) -> Any
): StringBuilder {
    append(preAppend)
    valueAction(this, value).also { if (it is String) append(it) }
    append(postAppend)
    return this
}

/**
 * Appends a JSON object to this builder
 *
 * ex: "[name]": { [valueAction] }
 */
fun StringBuilder.appendObject(
    name: String,
    postAppend: String = "",
    valueAction: StringBuilder.(objName: String) -> Any = {}
): StringBuilder {
    val useName = if (name.isEmpty()) "" else "\"$name\": "
    append("$useName{")
    valueAction(this, name).also {
        if (it is String)
            append(it.removeSurrounding("{", "}"))
    }
    append("}$postAppend")
    return this
}

/**
 * Appends a JSON item to this builder
 *
 * ex: "[name]": [valueAction]
 */
fun StringBuilder.appendItem(
    name: String,
    postAppend: String = "",
    valueAction: StringBuilder.() -> Any = {}
): StringBuilder {
    append("\"$name\": ")
    valueAction(this).also { if (it is String) append(it) }
    append(postAppend)
    return this
}

/**
 * Appends [message] to this [StringBuffer] with the optional formatting [args]
 */
fun StringBuilder.appendLineFmt(message: String, vararg args: Any? = arrayOf()) =
    appendLine(message.format(*args))

/**
 * Appends [message] to this [StringBuffer] with the optional formatting [args]
 */
inline fun StringBuilder.appendLineFmt(message: () -> String, vararg args: Any? = arrayOf()) =
    appendLine(message.invoke().format(*args))
