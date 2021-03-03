package unitTests.mockHelpers

import io.ktor.http.*
import io.mockk.every
import io.mockk.spyk
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestData
import okhttp3.ResponseData
import org.junit.Assert
import org.junit.Test

class tapeDataTests {

    @Test
    fun requestReplayConversion() {
        val urlData = "http://valid.url/"
        val methodData = "POST"
        val headerValue = ContentType.Any.toString()
        val headerData = headersOf(HttpHeaders.ContentType, headerValue)
        val bodyData = "testBody"

        val host = spyk(RequestData()) {
            every { httpUrl } returns urlData.toHttpUrlOrNull()
            every { method } returns methodData
            every { headers } returns headerData
            every { body } returns bodyData
        }

        val test = host.replayRequest

        Assert.assertEquals(urlData, test.url().toString())
        Assert.assertEquals(methodData, test.method())

        Assert.assertEquals(headerData, test.headers())
        Assert.assertTrue(test.headers().size > 0)
        Assert.assertEquals(headerValue, test.contentType)

        Assert.assertTrue(test.hasBody())
        Assert.assertEquals(bodyData, test.bodyAsText())
        Assert.assertTrue(test.body().isNotEmpty())
    }

    @Test
    fun responseReplayConversion() {
        val codeData = 200
        val headerValue = ContentType.Any.toString()
        val headerData = headersOf(HttpHeaders.ContentType, headerValue)
        val bodyData = "testBody"

        val host = spyk(ResponseData()) {
            every { code } returns codeData
            every { headers } returns headerData
            every { body } returns bodyData
        }

        val test = host.replayResponse

        Assert.assertEquals(codeData, test.code())

        Assert.assertEquals(headerData, test.headers())
        Assert.assertTrue(test.headers().size > 0)
        Assert.assertEquals(headerValue, test.contentType)

        Assert.assertTrue(test.hasBody())
        Assert.assertEquals(bodyData, test.bodyAsText())
        Assert.assertTrue(test.body().isNotEmpty())
    }
}
