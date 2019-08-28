package com.fiserv.ktmimic.tapeTypes.helpers

import okreplay.Request
import okreplay.Response
import java.util.Date

class RecordedInteractions {

    constructor(request: Request, response: Response) {
        this.request = request
        this.response = response
        chapterName = request.chapterName
        updateTapeData()
    }

    constructor(requestData: RequestTapedata, responseData: ResponseTapedata) {
        this.requestData = requestData
        this.responseData = responseData
    }

    @Suppress("unused")
    val recordedDate = Date()
    var chapterName = ""

    /**
     * Remaining uses of this mock interaction
     */
    var mockUses = 0

    @Transient
    lateinit var request: Request
    lateinit var requestData: RequestTapedata

    @Transient
    lateinit var response: Response
    lateinit var responseData: ResponseTapedata

    val bodyKey: String
        get() = request.filterBody().hashCode().toString()

    /**
     * Updates Replay data using json request/ response data from the tapeData
     */
    fun updateReplayData() {
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
