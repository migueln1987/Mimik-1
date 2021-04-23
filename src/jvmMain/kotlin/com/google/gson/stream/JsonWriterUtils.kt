package com.google.gson.stream

/**
 * Appends [actions] with begin/end Object
 */
fun JsonWriter.writeObject(actions: () -> Unit) {
    beginObject()
    actions.invoke()
    endObject()
}

/**
 * Appends [actions] with begin/end Array
 */
fun JsonWriter.writeArray(actions: () -> Unit) {
    beginArray()
    actions.invoke()
    endArray()
}

/**
 * Writes a new variable with a name of [varName] and [data].
 * Unknown data types will export [null].
 */
fun JsonWriter.writeData(varName: String, data: Any) {
    name(varName)
    when (data) {
        is String -> value(data)
        is Boolean -> value(data)
        is Double -> value(data)
        is Long -> value(data)
        is Number -> value(data)
        else -> nullValue()
    }
}

/**
 * Writes a new variable with a name of [varName].
 *
 * [dataActions] is expected to write the data part
 */
fun JsonWriter.writeData(varName: String, dataActions: () -> Unit) {
    name(varName)
    dataActions.invoke()
}
