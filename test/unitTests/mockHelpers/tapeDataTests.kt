package unitTests.mockHelpers

import io.ktor.http.HttpHeaders
import io.mockk.every
import io.mockk.spyk
import mimikMockHelpers.Requestdata
import mimikMockHelpers.Responsedata
import okhttp3.Headers
import okhttp3.HttpUrl
import org.junit.Assert
import org.junit.Test

class tapeDataTests {

    @Test
    fun requestReplayConversion() {
        val urlData = "http://valid.url/"
        val methodData = "POST"
        val headerValue = "value"
        val headerData = Headers.of(HttpHeaders.ContentType, headerValue)
        val bodyData = "testBody"

        val host = spyk(Requestdata()) {
            every { httpUrl } returns HttpUrl.parse(urlData)
            every { method } returns methodData
            every { tapeHeaders } returns headerData
            every { body } returns bodyData
        }

        val test = host.replayRequest

        Assert.assertEquals(urlData, test.url().toString())
        Assert.assertEquals(methodData, test.method())

        Assert.assertEquals(headerData, test.headers())
        Assert.assertTrue(test.headers().size() > 0)
        Assert.assertEquals(headerValue, test.contentType)

        Assert.assertTrue(test.hasBody())
        Assert.assertEquals(bodyData, test.bodyAsText())
        Assert.assertTrue(test.body().isNotEmpty())
    }

    @Test
    fun responseReplayConversion() {
        val codeData = 200
        val headerValue = "value"
        val headerData = Headers.of(HttpHeaders.ContentType, headerValue)
        val bodyData = "testBody"

        val host = spyk(Responsedata()) {
            every { code } returns codeData
            every { tapeHeaders } returns headerData
            every { body } returns bodyData
        }

        val test = host.replayResponse

        Assert.assertEquals(codeData, test.code())

        Assert.assertEquals(headerData, test.headers())
        Assert.assertTrue(test.headers().size() > 0)
        Assert.assertEquals(headerValue, test.contentType)

        Assert.assertTrue(test.hasBody())
        Assert.assertEquals(bodyData, test.bodyAsText())
        Assert.assertTrue(test.body().isNotEmpty())
    }
}
