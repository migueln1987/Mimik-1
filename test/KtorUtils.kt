@file:Suppress("PackageDirectoryMismatch")
package io.ktor.server.testing

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod

/**
 * Make a test request
 */
fun TestApplicationEngine.handleRequest(
    method: HttpMethod,
    uri: String,
    port: Int,
    setup: TestApplicationRequest.() -> Unit = {}
): TestApplicationCall = handleRequest {
    this.uri = uri
    this.method = method
    addHeader(HttpHeaders.Host, "localhost:$port")
    setup()
}
