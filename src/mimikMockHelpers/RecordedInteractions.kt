package mimikMockHelpers

import helpers.RandomHost
import helpers.attractors.AttractorMatches
import helpers.attractors.RequestAttractors
import helpers.isTrue
import helpers.toTapeData
import java.util.Date

@Suppress("unused")
class RecordedInteractions {
    constructor(builder: (RecordedInteractions) -> Unit = {}) {
        builder.invoke(this)
        origionalMockUses = mockUses
    }

    constructor(request: okreplay.Request, response: okreplay.Response) {
        requestData = request.toTapeData
        responseData = response.toTapeData
    }

    var recordedDate: Date? = Date()
        get() = field ?: Date()
    var modifiedDate: Date? = Date()
        get() = field ?: recordedDate ?: Date()
        set(value) {
            field = if (value == recordedDate)
                null else value
        }

    var chapterName: String? = null
        set(value) {
            if (value.isNullOrBlank().not())
                field = value
        }

    val name: String
        get() = chapterName ?: RandomHost(hashCode()).valueAsUUID

    var alwaysLive: Boolean? = false

    var attractors: RequestAttractors? = null

    /**
     * Remaining uses of this mock interaction.
     *
     * -1 = Always enable.
     *
     * -2 = disable.
     *
     * 0 = limited mock, disabled due to expired uses.
     *
     * (1..Int.Max_Value) = limited mock
     */
    var mockUses = MockUseStates.ALWAYS.state
        get() {
            if (origionalMockUses == null)
                origionalMockUses = field
            return field
        }
        set(value) {
            if (origionalMockUses == null)
                origionalMockUses = field
            field = value
        }

    @Transient
    var origionalMockUses: Int? = null

    val awaitResponse: Boolean
        get() = !hasResponseData

    var request: okreplay.Request?
        get() = requestData?.replayRequest
        set(value) {
            requestData = value?.toTapeData
        }

    var response: okreplay.Response?
        get() = responseData?.replayResponse
        set(value) {
            responseData = value?.toTapeData
        }

    var requestData: Requestdata? = null
    var responseData: Responsedata? = null

    @Transient
    var recentRequest: Requestdata? = null

    @Transient
    var genResponses: MutableList<Responsedata>? = mutableListOf()

    val hasRequestData: Boolean
        get() = requestData != null

    val hasResponseData: Boolean
        get() = responseData != null

    override fun toString(): String {
        return "%s; Uses: %d".format(
            name, mockUses
        )
    }

    fun resetUses() {
        origionalMockUses?.also { mockUses = it }
    }

    fun clone(postClone: (RecordedInteractions) -> Unit = {}) = RecordedInteractions {
        it.recordedDate = Date()
        it.chapterName = "${name}_clone"
        it.alwaysLive = alwaysLive
        it.attractors = attractors?.clone()
        it.mockUses = mockUses
        it.requestData = requestData?.clone()
        it.responseData = responseData?.clone()
    }.also { postClone.invoke(it) }

    /**
     * @return
     * - (-1): there is no replay data
     * - (1): matches the path
     * - (0): does not match the path
     */
    // todo; update to be included in matches
    @Deprecated(message = "update to be included in matches", level = DeprecationLevel.ERROR)
    private fun matchesPath(inputRequest: okhttp3.Request): Int {
        if (!hasRequestData) return -1
        return if (requestData?.httpUrl?.encodedPath() == inputRequest.url().encodedPath())
            1 else 0
    }

    /**
     * Returns how many headers from [requestData] match this source's request
     */
    // todo; update to be included in matches
    @Deprecated(message = "update to be included in matches", level = DeprecationLevel.ERROR)
    private fun matchingHeaders(inputRequest: okhttp3.Request): AttractorMatches {
        if (!hasRequestData || (requestData?.tapeHeaders?.size() ?: 0) < 2)
            return AttractorMatches()

        val response = AttractorMatches()
        val source = requestData!!.tapeHeaders.toMultimap()
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
