package mimikMockHelpers

import helpers.attractors.AttractorMatches
import helpers.attractors.RequestAttractors
import helpers.isTrue
import tapeItems.FilteredBodyRule
import java.util.Date
import java.util.UUID

@Suppress("unused")
class RecordedInteractions {
    constructor(builder: (RecordedInteractions) -> Unit = {}) {
        builder.invoke(this)
    }

    constructor(request: okreplay.Request, response: okreplay.Response) {
        this.request = request
        this.response = response
        updateTapeData()
    }

    var recordedDate = Date()
    var chapterName = ""
    var readOnly = false
    var attractors: RequestAttractors? = null

    /**
     * Remaining uses of this mock interaction.
     *
     * -1 = Always enable.
     *
     * -2 = disable.
     *
     * 0 = memory only mock, disabled due to expired uses.
     *
     * (1..Int.Max_Value) = memory only mock
     */
    var mockUses = InteractionUseStates.ALWAYS.state

    @Transient
    lateinit var request: okreplay.Request
    @Transient
    lateinit var response: okreplay.Response
    lateinit var requestData: RequestTapedata
    lateinit var responseData: ResponseTapedata

    val hasRequestData: Boolean
        get() = ::requestData.isInitialized

    val bodyKey: String
        get() = FilteredBodyRule.filter(request).hashCode().toString()

    init {
        if (chapterName.isBlank())
            chapterName = UUID.randomUUID().toString()
    }

    /**
     * Updates Replay data using json request/ response data from the tapeData
     */
    fun updateReplayData() {
        if (::requestData.isInitialized)
            request = requestData.replayRequest
        if (::responseData.isInitialized)
            response = responseData.replayResponse
    }

    /**
     * Updates TapeData for json saving
     */
    fun updateTapeData() {
        if (::request.isInitialized)
            requestData = RequestTapedata(request)
        if (::requestData.isInitialized)
            attractors = RequestAttractors(requestData)

        if (::response.isInitialized)
            responseData = ResponseTapedata(response)
    }

    /**
     * Returns true if there is loaded replay data
     */
    private fun ensureReplayData(): Boolean {
        if (::request.isInitialized.not()) updateReplayData() // try updating the data
        if (::request.isInitialized.not()) return false // just give up
        return true
    }

    /**
     * @return
     * - (-1): there is no replay data
     * - (1): matches the path
     * - (0): does not match the path
     */
    fun matchesPath(inputRequest: okhttp3.Request): Int {
        if (!ensureReplayData()) return -1
        return if (request.url().encodedPath() == inputRequest.url().encodedPath())
            1 else 0
    }

    /**
     * Returns how many headers from [request] match this source's request
     */
    fun matchingHeaders(inputRequest: okhttp3.Request): AttractorMatches {
        if (!ensureReplayData() || request.headers().size() < 2)
            return AttractorMatches()

        val response = AttractorMatches(0, 0, 0)
        val source = request.headers().toMultimap()
        val input = inputRequest.headers().toMultimap()

        source.forEach { (t, u) ->
            response.Required += u.size
            if (input.containsKey(t)) {
                u.forEach {
                    response.Required += if (input[t]?.contains(it).isTrue()) 1 else 0
                }
            }
        }

        return response
    }
}
