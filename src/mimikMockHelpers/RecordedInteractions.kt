package mimikMockHelpers

import helpers.attractors.RequestAttractors
import helpers.isTrue
import tapeItems.helpers.filterBody
import java.util.Date
import java.util.UUID

@Suppress("unused")
class RecordedInteractions {

    constructor()
    constructor(request: okreplay.Request, response: okreplay.Response) {
        this.request = request
        this.response = response
        updateTapeData()
    }

    enum class UseStates(val state: Int) {
        ALWAYS(-1),
        DISABLE(-2),
        DISABLEDMOCK(0)
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
    var mockUses = UseStates.ALWAYS.state

    @Transient
    lateinit var request: okreplay.Request
    @Transient
    lateinit var response: okreplay.Response
    lateinit var requestData: RequestTapedata
    lateinit var responseData: ResponseTapedata

    val bodyKey: String
        get() = request.filterBody().hashCode().toString()

    init {
        if (chapterName.isBlank())
            chapterName = UUID.randomUUID().toString()
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
        if (::request.isInitialized)
            requestData = RequestTapedata(request)
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
    fun matchingHeaders(inputRequest: okhttp3.Request): Int {
        if (!ensureReplayData()) return 0

        val source = request.headers().toMultimap()
        val input = inputRequest.headers().toMultimap()

        var matches = 0
        source.forEach { (t, u) ->
            if (input.containsKey(t)) {
                u.forEach {
                    matches += if (input[t]?.contains(it).isTrue()) 1 else 0
                }
            }
        }

        return matches
    }
}
