package mimikMockHelpers

import io.ktor.http.HttpStatusCode

class QueryResponse<T> {
    var item: T? = null
    var status: HttpStatusCode = HttpStatusCode.NoContent
    var responseMsg: String? = null

    constructor(build: QueryResponse<T>.() -> Unit = {}) {
        build.invoke(this)
    }

    constructor(input: T, build: QueryResponse<T>.() -> Unit = {}) {
        item = input
        status = HttpStatusCode.Found
        build.invoke(this)
    }
}
