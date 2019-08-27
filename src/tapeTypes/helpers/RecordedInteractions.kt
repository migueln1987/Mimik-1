package com.fiserv.ktmimic.tapeTypes.helpers

import okreplay.Request
import okreplay.Response
import java.util.Date

class RecordedInteractions {

    constructor(request: Request, response: Response) {
        this.request = request
        this.response = response

        val callName = request.url().queryParameter("opId")
            ?: request.url().query()
            ?: request.url().encodedPath()

        chapterName = "%s_[%s]_%s".format(
            request.method(),
            callName,
            request.filterBody().hashCode()
        )
        updateTapeData()
    }

    constructor(requestData: RequestTapedata, responseData: ResponseTapedata) {
        this.requestData = requestData
        this.responseData = responseData
    }

    val recorded = Date()
    var chapterName = ""

    @Transient
    lateinit var request: Request
    lateinit var requestData: RequestTapedata

    @Transient
    lateinit var response: Response
    lateinit var responseData: ResponseTapedata

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
}
