package com.fiserv.ktmimic.tapeTypes.helpers

import okreplay.Request
import okreplay.Response
import java.util.*

data class RecordedInteractions(
    val request: Request,
    var response: Response
) {
    val ChapterName: String
        get() {
            return "%s_%s_%s".format(
                request.method(),
                request.url().encodedPathSegments(),
                request.filterBody().hashCode()
            )
        }

    val recorded = Date()
}
