package apiTests

import VCRConfig
import com.fiserv.mimik.module
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockkObject
import okreplay.OkReplayConfig
import org.junit.Assert
import java.io.File

fun setupVCRConfig(newDir: String = "/test/Tapes") {
    mockkObject(VCRConfig)
    every { VCRConfig.getConfig } returns OkReplayConfig.Builder()
        .also { it.tapeRoot(File(newDir)) }
        .build()
}

fun TestApp(callback: TestApplicationEngine.() -> Unit) =
    withTestApplication({ module(testing = true) }, callback)

fun TestApplicationCall.request(config: (TestApplicationRequest) -> Unit) =
    config.invoke(request)

fun TestApplicationCall.response(config: (TestApplicationResponse) -> Unit) =
    config.invoke(response)

/**
 * Asserts that [input] contains the string [search]
 */
fun assertContains(search: String?, input: String?) {
    if (search == null && input == null) return
    Assert.assertTrue(input?.contains(search.orEmpty()) == true)
}

fun assertStartsWith(expected: String, actual: String?) =
    Assert.assertTrue(actual?.startsWith(expected) == true)
