package com.google.gson.stream

import helpers.tryOrNull
import kotlin.reflect.full.superclasses

/**
 * Reads in an Object to [actions], if the reader begins with [JsonToken.BEGIN_OBJECT]
 */
fun JsonReader.readObject(actions: () -> Unit) {
    if (peek() != JsonToken.BEGIN_OBJECT) return

    beginObject()
    while (peek() != JsonToken.END_OBJECT)
        actions.invoke()
    endObject()
}

/**
 * Reads in an Array to [actions], if the reader begins with [JsonToken.BEGIN_ARRAY]
 */
fun JsonReader.readArray(actions: (String?) -> Unit) {
    var varName: String? = null
    when (peek()) {
        JsonToken.NAME -> {
            varName = nextName()
            if (peek() != JsonToken.BEGIN_ARRAY)
                return
        }
        JsonToken.BEGIN_ARRAY -> Unit
        else -> return
    }

    beginArray()
    while (peek() != JsonToken.END_ARRAY)
        actions.invoke(varName)
    endArray()
}

/**
 * Reads a single word from [JsonReader].
 *
 * The word is then cast to typed data [T] and passed to [loadData] with optional [givenName].
 *
 * Note: [data] could be null
 */
inline fun <reified T : Any> JsonReader.readData(
    givenName: String = "",
    loadData: (varName: String?, data: T?) -> Unit
) {
    val dataName = when {
        givenName.isNotBlank() -> givenName
        peek() == JsonToken.NAME -> nextName()
        else -> null
    }

    val data = tryOrNull {
        when (peek()) {
            JsonToken.STRING -> nextString()
            JsonToken.BOOLEAN -> nextBoolean()
            JsonToken.NUMBER -> {
                when {
                    Double::class in T::class.superclasses -> nextDouble()
                    Long::class in T::class.superclasses -> nextLong()
                    Number::class in T::class.superclasses -> nextInt()
                    else -> skipValue()
                }
            }
            else -> null
        }
    } as? T

    loadData.invoke(dataName, data)
}

/**
 * Reads a single word from [JsonReader].
 *
 * The word is then cast to typed data [T];
 * on successful casting, [data] is passed to [loadData] with optional [givenName].
 */
inline fun <reified T : Any> JsonReader.readData_nonNull(
    givenName: String = "",
    loadData: (varName: String?, data: T) -> Unit
) {
    readData<T>(givenName) { varName, data ->
        if (data != null) loadData(varName, data)
    }
}
