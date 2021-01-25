package mimikMockHelpers

import io.ktor.http.HttpStatusCode

/**
 * Object which contains:
 * - Object which was queried for
 * - Status of what was queried for (based on [HttpStatusCode])
 * - Response Message, to help explain the status
 *
 * Note: During initialization, [status] is set to [HttpStatusCode.Found] if [item] is not null
 *
 * @param item Item which was queried
 */
class QueryResponse<T>(var item: T? = null, build: QueryResponse<T>.() -> Unit = {}) {
    /**
     * Result status of the query
     */
    var status: HttpStatusCode = HttpStatusCode.NoContent

    /**
     * (Optional) Explication of the status
     */
    var responseMsg: String? = null

    init {
        if (item != null)
            status = HttpStatusCode.Found
        build.invoke(this)
    }

    operator fun component1() = item
    operator fun component2() = status
    operator fun component3() = responseMsg
}
