package com.fiserv.mimik.tapeTypes.helpers

/**
 * Generates a chapter name from:
 * - the method (GET/ POST/ other)
 * - call name (opId value, query, or encoded path)
 * - filtered body
 */
val okreplay.Request.chapterName: String
    get() {
        val callName = url().query()
            ?: url().encodedPath()

        return "%s_[%s]_%s".format(
            method(),
            callName,
            filterBody().hashCode()
        )
    }

@Deprecated("Convert to using tape attractors")
val okreplay.Request.chapterNameHead: String
    get() {
        val callName = url().query()
            ?: url().encodedPath()

        return "%s_[%s]".format(
            method(),
            callName
        )
    }

/**
 * Generates a chapter name from:
 * - the method (GET/ POST/ other)
 * - call name (opId value, query, or encoded path)
 * - mock body key (provided)
 */
fun okreplay.Request.mockChapterName(method: String, bodyKey: String): String {
    val callName = url().query()
        ?: url().encodedPath()

    return "%s_[%s]_%s".format(
        method,
        callName,
        bodyKey
    )
}
