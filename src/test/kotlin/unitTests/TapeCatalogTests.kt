package unitTests

import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import mimik.tapeItems.TapeCatalog
import mimik.helpers.attractors.RequestAttractorBit
import mimik.helpers.attractors.RequestAttractors
import mimik.mockHelpers.MockUseStates
import mimik.mockHelpers.RecordedInteractions
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import mimik.tapeItems.BaseTape
import okhttp3.createResponse

class TapeCatalogTests {

    lateinit var testObject: TapeCatalog

    @Before
    fun setup() {
        testObject = TapeCatalog()
    }

    @Test
    fun findResponseByQuery_Empty() {
        val request = mockk<okhttp3.Request>()
        val response = testObject.findResponseByQuery(request).first

        Assert.assertTrue(testObject.tapes.isEmpty())
        Assert.assertEquals(response.status, HttpStatusCode.NoContent)
    }

    @Test
    fun findResponseByQuery_Found() {
        val mockChapter = mockk<RecordedInteractions> {
            every { mockUses } returns MockUseStates.ALWAYS.state
            every { attractors } returns RequestAttractors {
                it.routingPath = RequestAttractorBit("/path")
            }
            every { cachedCalls } returns mutableSetOf()
        }

        val tape = BaseTape.Builder().build().also {
            it.chapters.add(mockChapter)
        }

        testObject.tapes.add(tape)

        val request = okhttp3.Request.Builder()
            .also { it.url("http://valid.url/path") }.build()
        val response = testObject.findResponseByQuery(request).first

        Assert.assertNotNull(response.item)
        Assert.assertEquals(response.status, HttpStatusCode.Found)
    }

    @Test
    fun findResponseByQuery_NotFound() {
        val mockChapter = mockk<RecordedInteractions> {
            every { mockUses } returns MockUseStates.ALWAYS.state
            every { attractors } returns RequestAttractors {
                it.routingPath = RequestAttractorBit("/other")
            }
            every { cachedCalls } returns mutableSetOf()
        }

        val tape = BaseTape.Builder().build().also {
            it.chapters.add(mockChapter)
        }

        testObject.tapes.add(tape)

        val request = okhttp3.Request.Builder()
            .also { it.url("http://valid.url/path") }.build()
        val response = testObject.findResponseByQuery(request).first

        Assert.assertNull(response.item)
        Assert.assertEquals(response.status, HttpStatusCode.NotFound)
    }

    @Test
    fun makeCatchResponseTest() {
        val request = mockk<okhttp3.Request>()
        val testCode = HttpStatusCode.OK
        val testMessage = "test"

        runBlocking {
            testObject.apply {
                val response = request.createResponse(testCode) { testMessage }

                Assert.assertEquals(response.code, testCode.value)
                Assert.assertEquals(response.message, testMessage)
            }
        }
    }
}
