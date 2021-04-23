@file:Suppress("KDocUnresolvedReference")

package mimik.mockHelpers

import kotlinx.isTrue
import mimik.helpers.*
import mimik.helpers.attractors.AttractorMatches
import mimik.helpers.attractors.RequestAttractors
import okhttp3.RequestData
import okhttp3.ResponseData
import okhttp3.orDefault
import okreplay.toOkRequest
import okreplay.toTapeData
import java.util.*

/**
 * Single recorded mock interaction which contains the following
 */
@Suppress("unused")
class RecordedInteractions {
    constructor(builder: (RecordedInteractions) -> Unit = {}) {
        builder(this)
        originalMockUses = mockUses
    }

    constructor(request: okreplay.Request, response: okreplay.Response) {
        requestData = request.toTapeData
        responseData = response.toTapeData
        attractors = RequestAttractors(requestData)
        originalMockUses = mockUses
    }

    /**
     * Cached request calls, used to quickly determine if this chapter should should be used.
     *
     * Key: (method + url + filtered headers + body) as hashcode
     */
    @Transient
    var cachedCalls: MutableSet<Int> = mutableSetOf()
        get() {
            @Suppress("SENSELESS_COMPARISON")
            if (field == null) field = mutableSetOf()
            return field
        }

    /**
     * When this chapter was created.
     *
     * Default: today's date
     */
    var recordedDate: Date? = Date()
        get() = field ?: Date()

    /**
     * When this chapter was last modified.
     *
     * Default: [recordedDate]
     *
     * Note: field is reset to use [recordedDate] if [recordedDate] is passed as the new value
     */
    var modifiedDate: Date? = null
        get() = field ?: recordedDate ?: Date()
        set(value) {
            field = if (value == recordedDate)
                null else value
        }

    /**
     * UUID assigned to this chapter
     */
    var UID: String? = UUID.randomUUID().toString()
        get() {
            return field
                ?: UUID.randomUUID().toString().also { field = it }
        }

    /**
     * User defined chapter name
     */
    var chapterName: String? = null

    /**
     * This chapter's name from either
     * - chosen chapter name
     * - a random UUID name (based on this chapter's hash code
     */
    val name: String
        get() = chapterName ?: RandomHost(hashCode()).valueAsUUID

    var alwaysLive: Boolean? = false

    /**
     * Attractors which determine if this chapter should be used
     */
    var attractors: RequestAttractors? = null

    /**
     * Sequence actions for this object, called each time this chapter is used
     */
    var seqActions: ArrayList<SeqActionObject>? = null

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
            if (originalMockUses == null)
                originalMockUses = field
            return field
        }
        set(value) {
            if (originalMockUses == null)
                originalMockUses = field
            field = value
        }

    // Todo; Sandbox only
    @Transient
    var originalMockUses: Int? = null

    /**
     * When [True], this chapter is waiting for recording or initializing response data
     */
    val awaitResponse: Boolean
        get() = !hasResponseData

    /** Request data, in okreplay format */
    var request: okreplay.Request?
        get() = requestData?.replayRequest
        set(value) {
            requestData = value?.toTapeData
        }

    /** Response, in okreplay format */
    var response: okreplay.Response?
        get() = responseData?.replayResponse
        set(value) {
            responseData = value?.toTapeData
        }

    /**
     * Raw request data
     */
    var requestData: RequestData? = null

    /**
     * Raw response data
     */
    var responseData: ResponseData? = null

    @Transient
    var recentRequest: RequestData? = null

    @Transient
    var genResponses: MutableList<ResponseData>? = mutableListOf()

    val hasRequestData: Boolean
        get() = requestData != null

    val hasResponseData: Boolean
        get() = responseData != null

    /**
     * Returns the hashCode of this [okhttp3.Request].
     *
     * If another [okhttp3.Request] has the same method + url + headers + body, then the contentHash will be the same
     *
     * Note: If there is no request
     */
    val contentHash: Int
        get() = requestData?.replayRequest?.toOkRequest?.contentHash ?: 0

    override fun toString(): String {
        val stateStr = when (mockUses) {
            MockUseStates.ALWAYS.state -> "Always"
            MockUseStates.DISABLE.state -> "Disabled"
            MockUseStates.DISABLEDLIMITED.state -> "Disabled - limited)"
            else -> "Limited"
        }

        return "%s; Uses: %d (%s)".format(
            name, mockUses, stateStr
        )
    }

    fun resetUses() {
        originalMockUses?.also { mockUses = it }
    }

    fun clone(postClone: (RecordedInteractions) -> Unit = {}) = RecordedInteractions {
        it.recordedDate = Date()
        it.chapterName = "${name}_clone"
        it.alwaysLive = alwaysLive
        it.attractors = attractors?.clone()
        it.mockUses = mockUses
        it.requestData = requestData?.clone()
        it.responseData = responseData?.clone()
    }.also { postClone(it) }

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
        return if (requestData?.httpUrl?.encodedPath == inputRequest.url.encodedPath)
            1 else 0
    }

    /**
     * Returns how many headers from [requestData] match this source's request
     */
    // todo; update to be included in matches
    @Deprecated(message = "update to be included in matches", level = DeprecationLevel.ERROR)
    private fun matchingHeaders(inputRequest: okhttp3.Request): AttractorMatches {
        if (!hasRequestData || (requestData?.headers?.size ?: 0) < 2)
            return AttractorMatches()

        val response = AttractorMatches()
        val source = requestData!!.headers.orDefault.toMultimap()
        val input = inputRequest.headers.toMultimap()

        source.forEach { (t, u) ->
            response.Required += u.size
            if (input.containsKey(t)) {
                u.forEach {
                    response.Required += if (input[t]?.contains(it).isTrue) 1 else 0
                }
            }
        }

        return response
    }
}
