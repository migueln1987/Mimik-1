package apiTests

import com.fiserv.mimik.module
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.withTestApplication
import org.junit.Assert

fun TestApp(callback: TestApplicationEngine.() -> Unit) =
    withTestApplication({ module(testing = true) }, callback)

fun TestApplicationCall.request(config: (TestApplicationRequest) -> Unit) =
    config.invoke(request)

fun TestApplicationCall.response(config: (TestApplicationResponse) -> Unit) =
    config.invoke(response)

fun assertContains(expected: String, source: String?) =
    Assert.assertTrue(source?.contains(expected) == true)

fun assertStartsWith(expected: String, source: String?) =
    Assert.assertTrue(source?.startsWith(expected) == true)
