package kotlinUtils.text

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
    valueAction.invoke(this, value)
        .also { if (it is String) this.appendLine(it) }
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
    return append(preAppend)
        .also { sb ->
            valueAction.invoke(sb, value)
                .also { if (it is String) sb.append(it) }
        }.append(postAppend)
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
    return append("$useName{").also { sb ->
        valueAction.invoke(sb, name)
            .also {
                if (it is String)
                    sb.append(it.removeSurrounding("{", "}"))
            }
    }.append("}$postAppend")
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
    return append("\"$name\": ").also { sb ->
        valueAction.invoke(sb)
            .also { if (it is String) sb.append(it) }
    }.append(postAppend)
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
