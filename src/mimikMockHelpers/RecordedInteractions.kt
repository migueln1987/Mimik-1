package com.fiserv.mimik.mimikMockHelpers

import com.fiserv.mimik.tapeItems.RequestAttractors
import com.fiserv.mimik.tapeTypes.helpers.chapterName
import com.fiserv.mimik.tapeTypes.helpers.filterBody
import okreplay.Request
import okreplay.Response
import java.util.Date

@Suppress("unused")
class RecordedInteractions {

    constructor()
    constructor(request: Request, response: Response) {
        this.request = request
        this.response = response
        updateTapeData()
    }

    var recordedDate = Date()
    var chapterName = ""
    var exportData = true
    var attractors: RequestAttractors? = null

    /**
     * Remaining uses of this mock interaction
     */
    var mockUses = 0

    @Transient
    lateinit var request: Request
    @Transient
    lateinit var response: Response
    lateinit var requestData: RequestTapedata
    lateinit var responseData: ResponseTapedata

    val bodyKey: String
        get() = request.filterBody().hashCode().toString()

    init {
        if (chapterName.isBlank())
            chapterName = request.chapterName
        updateTapeData()
    }

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
