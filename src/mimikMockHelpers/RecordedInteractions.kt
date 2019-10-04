package mimikMockHelpers

import helpers.attractors.AttractorMatches
import helpers.attractors.RequestAttractors
import helpers.isTrue
import helpers.toTapeData
import java.util.Date
import java.util.UUID

@Suppress("unused")
class RecordedInteractions {
    constructor(builder: (RecordedInteractions) -> Unit = {}) {
        builder.invoke(this)
    }

    constructor(request: okreplay.Request, response: okreplay.Response) {
        requestData = request.toTapeData
        responseData = response.toTapeData
    }

    var recordedDate = Date()

    var chapterName: String? = null
        set(value) {
            if (value.isNullOrBlank().not())
                field = value
        }
    val name: String
        get() = chapterName ?: UUID.randomUUID().toString()

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

    val awaitResponse: Boolean
        get() = !hasResponseData

    var request: okreplay.Request
        get() = requestData.replayRequest
        set(value) {
            requestData = value.toTapeData
        }

    var response: okreplay.Response
        get() = responseData.replayResponse
        set(value) {
            responseData = value.toTapeData
        }

    lateinit var requestData: RequestTapedata
    lateinit var responseData: ResponseTapedata

    val hasRequestData: Boolean
        get() = ::requestData.isInitialized

    val hasResponseData: Boolean
        get() = ::responseData.isInitialized

    override fun toString(): String {
        return "%s; Uses: %d".format(
            name, mockUses
        )
    }

    /**
     * @return
     * - (-1): there is no replay data
     * - (1): matches the path
     * - (0): does not match the path
     */
    // todo; update to be included in matches
    fun matchesPath(inputRequest: okhttp3.Request): Int {
        if (!hasRequestData) return -1
        return if (requestData.httpUrl?.encodedPath() == inputRequest.url().encodedPath())
            1 else 0
    }

    /**
     * Returns how many headers from [requestData] match this source's request
     */
    // todo; update to be included in matches
    fun matchingHeaders(inputRequest: okhttp3.Request): AttractorMatches {
        if (!hasRequestData || requestData.tapeHeaders.size() < 2)
            return AttractorMatches()

        val response = AttractorMatches()
        val source = requestData.tapeHeaders.toMultimap()
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
