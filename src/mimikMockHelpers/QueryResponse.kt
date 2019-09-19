package mimikMockHelpers

import io.ktor.http.HttpStatusCode

class QueryResponse<T>(build: QueryResponse<T>.() -> Unit = {}) {
    var item: T? = null
    var status: HttpStatusCode = HttpStatusCode.OK
    var responseMsg: String? = null

    init {
        build.invoke(this)
    }
}
