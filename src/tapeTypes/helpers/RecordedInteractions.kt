package com.fiserv.ktmimic.tapeTypes.helpers

import okreplay.Request
import okreplay.Response
import java.util.*

class RecordedInteractions {
    @Transient
    lateinit var request: Request
    lateinit var requestData: RequestTapedata

    @Transient
    lateinit var response: Response
    lateinit var responseData: ResponseTapedata

    constructor(request: Request, response: Response) {
        this.request = request
        this.response = response
        updateTapeData()
    }

    constructor(requestData: RequestTapedata, responseData: ResponseTapedata) {
        this.requestData = requestData
        this.responseData = responseData
    }

    /**
     * Loads json data back into request/ response data for Replay
     */
    fun loadReplayData() {
        request = requestData.replayRequest
        response = responseData.replayResponse
    }

    /**
     * Updates TapeData for json saving
     */
    fun updateTapeData() {
        requestData = RequestTapedata(request)
        responseData = ResponseTapedata(response)
    }

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
