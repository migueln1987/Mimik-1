package helpers.attractors

import helpers.*
import helpers.matchers.matchResults
import mimikMockHelpers.RequestData
import okhttp3.internal.http.HttpMethod

class RequestAttractors : Attractor {

    constructor(config: (RequestAttractors) -> Unit = {}) {
        config.invoke(this)
    }

    constructor(request: RequestData?) {
        request?.httpUrl?.also { url ->
            routingPath = RequestAttractorBit(url.encodedPath.removePrefix("/"))
            if (routingPath?.value?.isBlank().isTrue)
                routingPath = null

            queryMatchers = url.queryParameterNames.flatMap { key ->
                url.queryParameterValues(key).map { value ->
                    RequestAttractorBit("$key=$value")
                }
            }
            if (queryMatchers?.isEmpty().isTrue)
                queryMatchers = null
        }

        request?.headers?.toStringPairs()?.also {
            headerMatchers = it.asSequence()
                .filterNot { skipHeaders.any { s -> it.startsWith(s, true) } }
                .map { v -> RequestAttractorBit(v) }.toList()
        }

        // If the call always has a body, but a matcher wasn't set
        // then add a compliance matcher
        if (HttpMethod.requiresRequestBody(request?.method.orEmpty()))
            bodyMatchers = listOf(RequestAttractorBit { it.allowAllInputs = true })
    }

    override fun toString(): String {
        return if (isInitial)
            "{Initial}"
        else
            "{%s,%s,%s,%s}".format(
                routingPath?.let { "1" } ?: "_",
                queryMatchers?.size?.toString() ?: "_",
                headerMatchers?.size?.toString() ?: "_",
                bodyMatchers?.size?.toString() ?: "_"
            )
    }

    /**
     * Returns [true] if any of the data in this object has data
     */
    val hasData: Boolean
        get() {
            return anyTrue(
                routingPath?.hardValue?.isNotBlank().isTrue,
                queryMatchers?.isNotEmpty().isTrue,
                headerMatchers?.isNotEmpty().isTrue,
                bodyMatchers?.isNotEmpty().isTrue
            )
        }

    /**
     * Returns true if this [RequestAttractors] has it's original (or near) data
     */
    val isInitial: Boolean
        get() {
            val maybeDefaultHeader = headerMatchers
                ?.let { it.isEmpty() || (it.size == 1 && it[0].allowAllInputs.isTrue) }
                ?: false

            return allTrue(
                routingPath == null,
                queryMatchers == null,
                maybeDefaultHeader,
                bodyMatchers == null
            )
        }

    fun clone() = RequestAttractors {
        it.routingPath = routingPath?.clone()
        it.queryMatchers = queryMatchers?.map { it.clone() }
        it.headerMatchers = queryMatchers?.map { it.clone() }
        it.bodyMatchers = queryMatchers?.map { it.clone() }
    }

    /**
     * Appends [data] to this object's data. null [data] are ignored
     */
    fun append(data: RequestAttractors?) {
        if (data == null) return

        routingPath = data.routingPath?.clone() ?: routingPath

        queryMatchers = matchAppender {
            from = data.queryMatchers
            to = queryMatchers
        }

        headerMatchers = matchAppender {
            from = data.headerMatchers
            to = headerMatchers
        }

        bodyMatchers = matchAppender {
            from = data.bodyMatchers
            to = bodyMatchers
        }
    }

    private data class MatcherPair(
        var from: List<RequestAttractorBit>? = null,
        var to: List<RequestAttractorBit>? = null
    )

    /**
     * Appends data from [matchers]'s [MatcherPair.from] to the contents of [MatcherPair.to],
     * then returns [MatcherPair.to].
     */
    private fun matchAppender(matchers: MatcherPair.() -> Unit): List<RequestAttractorBit>? {
        return MatcherPair().apply {
            matchers.invoke(this) // initialize matcherPair's data

            from?.also { newData ->
                to = (to ?: listOf())
                    .toMutableList()
                    .apply {
                        addAll(newData.filterNot { contains(it) })
                    }
            }
        }.to
    }
}

/**
 * Using the provided [RequestAttractorBit]s, the [input] will be scanned for required and optional matches
 *
 * - No [RequestAttractorBit]s and [input] is not null: returns failed match
 * - Any [RequestAttractorBit]'s allowAllInputs = true: ANY input (even none) passes the match
 * - all of [RequestAttractorBit]'s values are blank: skips this test
 * - [input] is null: fail all the non-optional (and non-except) tests
 */
fun List<RequestAttractorBit>?.getMatches(input: String?): AttractorMatches =
    getMatches(input?.let { listOf(it) })

/**
 * Using the provided [RequestAttractorBit]s, the [inputs] will be scanned for required and optional matches
 *
 * - No [RequestAttractorBit]s and [inputs] is not null: returns failed match
 * - Any [RequestAttractorBit]'s allowAllInputs = true: ANY input (even none) passes the match
 * - all of [RequestAttractorBit]'s values are blank: skips this test
 * - [inputs] is null: fail all the non-optional (and non-except) tests
 */
fun List<RequestAttractorBit>?.getMatches(inputs: List<String>?): AttractorMatches {
    when {
        this.isNullOrEmpty() -> AttractorMatches().also {
            if (!inputs.isNullOrEmpty()) it.Required = 1 // mark as a failed match
        }

        any { it.allowAllInputs.isTrue }.isTrue ->
            AttractorMatches(1, 1, 0)

        all { it.hardValue.isBlank() }.isTrue ->
            AttractorMatches()

        inputs.isNullOrEmpty() ->
            AttractorMatches(count { it.required }, -1, -1)

        else -> null
    }?.apply { return this }

    // null-checks from above "when"
    requireNotNull(this)
    requireNotNull(inputs)

    /* Actions
    1. Bucketing - Sort inputs into AttractorBits
    -a each input may be filtered into multiple buckets
    -b failed matches are filtered out
    2. Ensure required buckets have data
    3. Collection how many optionals matched
    - then return Required + optional matches
     */

    fun xCounts(scanReq: Boolean): Triple<Int, Int, Int> {
        return asSequence()
            .filter { it.required == scanReq }
            .map { bit ->
                // step 1
                // bucket : matches
                bit to inputs.map { bit.value.matchResults(it)[0] }.filter { it.isNotEmpty() }
            }
            .fold(Triple(0, 0, 0)) { result, (bit, bData) ->
                val pass = when {
                    !bit.required && scanReq -> 0
                    bit.required && !scanReq -> 0
                    bData.isEmpty() && bit.except.isTrue -> 1
                    bData.isNotEmpty() && !bit.except.isTrue -> 1
                    else -> 0
                }
                Triple(
                    result.first + 1, // is as requested type
                    result.second + pass, // did it pass?
                    bData.sumBy { it.sumBy { it.litMatchCnt } } // literal matches
                )
            }
    }

    val reqCounts = xCounts(true)

    // step 2, did any fail meeting the required count
    if (reqCounts.first == 0 || reqCounts.first != reqCounts.second) {
        return AttractorMatches(reqCounts.first, reqCounts.second)
    }

    // step 3
    val optCounts = xCounts(false)

    return AttractorMatches(reqCounts.first, reqCounts.second, optCounts.second).also {
        it.reqLiterals = reqCounts.third
        it.optLiterals = optCounts.third
    }
}

fun List<RequestAttractorBit>?.append(
    newData: List<RequestAttractorBit>,
    haveSameRequired: Boolean = true
): List<RequestAttractorBit> {
    val toData = (this ?: listOf()).toMutableList()

    newData.forEach { nChk ->
        val sameValue = toData.firstOrNull {
            it.hardValue.isNotBlank() && it.hardValue == nChk.hardValue
        }

        if (sameValue == null) {
            toData.add(nChk)
        } else if (haveSameRequired && (nChk.required != sameValue.required)) {
            toData.remove(sameValue)
            toData.add(nChk)
        }
    }
    return toData
}
