package unitTests.tapeTests

import apiTests.assertContains
import helpers.asHttpUrl
import helpers.attractors.RequestAttractorBit
import helpers.attractors.RequestAttractors
import helpers.attractors.UniqueBit
import helpers.attractors.UniqueTypes
import io.mockk.every
import io.mockk.mockk
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.RecordedInteractions
import mimikMockHelpers.Requestdata
import okreplay.TapeMode
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import tapeItems.BlankTape

class BlankTapeTest {

    lateinit var testObject: BlankTape

    @Before
    fun setup() {
        testObject = BlankTape.Builder().build()
    }

    @Test
    fun testBuilder() {
        Assert.assertTrue(testObject.name.isNotEmpty())
        Assert.assertFalse(testObject.hasNameSet)

        Assert.assertNull(testObject.attractors)
        Assert.assertFalse(testObject.isValidURL)

        Assert.assertEquals(testObject.mode, TapeMode.READ_WRITE)
        Assert.assertTrue(testObject.isReadable)
        Assert.assertTrue(testObject.isWritable)

        Assert.assertEquals(testObject.file?.nameWithoutExtension, testObject.name)
        Assert.assertEquals(testObject.file?.extension, "json")
    }

    @Test
    fun testBuilderValues() {
        val name = "name123"
        val url = "http://none.com"
        val path = "path123"
        val attractorData = RequestAttractors {
            it.routingPath = RequestAttractorBit(path)
        }

        testObject = BlankTape.Builder {
            it.tapeName = name
            it.routingURL = url
            it.attractors = attractorData
            it.allowLiveRecordings = false
        }.build()

        Assert.assertEquals(name, testObject.name)
        Assert.assertTrue(testObject.hasNameSet)

        Assert.assertNotNull(testObject.attractors)
        assertContains(path, testObject.attractors?.routingPath?.value)

        Assert.assertEquals(url, testObject.routingUrl)
        Assert.assertTrue(testObject.isValidURL)

        Assert.assertEquals(testObject.mode, TapeMode.READ_ONLY)
        Assert.assertTrue(testObject.isReadable)
        Assert.assertFalse(testObject.isWritable)

        Assert.assertEquals(name, testObject.file?.nameWithoutExtension)
        Assert.assertEquals("json", testObject.file?.extension)
    }

    @Test
    fun uppercaseJsonName() {
        val name = "/name"
        testObject = BlankTape.Builder {
            it.tapeName = name
        }.build()

        Assert.assertEquals("Name", testObject.file?.nameWithoutExtension)
    }

    @Test
    fun updateNameByURL() {
        val url = "/testing"
        testObject.updateNameByURL(url)

        Assert.assertTrue(testObject.name.contains(url.removePrefix("/")))
        Assert.assertTrue(testObject.hasNameSet)
    }

    @Test
    fun appendChapters() {
        val chapter = RecordedInteractions()

        Assert.assertTrue(testObject.chapters.isEmpty())
        testObject.chapters.add(chapter)
        Assert.assertTrue(testObject.chapters.isNotEmpty())
        Assert.assertTrue(testObject.size() > 0)
    }

    @Test
    fun rehostRequestToChain() {
        val oldUrl = "http://replace.me"
        val validUrl = "http://Host.url"
        val request = okhttp3.Request.Builder()
            .also {
                it.url(oldUrl)
                it.method("GET", null)
                it.header("key", "value")
            }.build()

        testObject.routingUrl = validUrl
        Assert.assertTrue(testObject.isValidURL)

        val testChain = testObject.requestToChain(request)

        Assert.assertNotNull(testChain)
        requireNotNull(testChain)

        val testRequest = testChain.request()

        val routeUrl = testObject.httpRoutingUrl
        Assert.assertNotNull(routeUrl)
        requireNotNull(routeUrl)

        Assert.assertEquals(routeUrl.host(), "host.url")

        val testHost = testRequest.header("HOST")
        Assert.assertEquals(testHost, routeUrl.host())

        Assert.assertEquals(testRequest.url().host(), routeUrl.host())

    }

    @Test
    fun createInteractionTest() {
        Assert.assertEquals(testObject.size(), 0)
        val data = testObject.createNewInteraction {
            it.mockUses = MockUseStates.ALWAYS.state
        }
        Assert.assertEquals(1, testObject.size())

        Assert.assertEquals(data.mockUses, testObject.chapters.first().mockUses)
    }

    @Test
    fun appendUniqueAttractors() {
        val reqBody = """
            {
                "batchStartPoint": 0,
                "batchEndPoint": 4,
                "transactionStartDate": 1572791496721,
                "transactionEndDate": 1575383496721,
                "transactionFor": "CARD",
                "transactionForId": 123456,
                "countryCode": "US",
                "deviceUniqueId": "123",
            }
            """.replace(" +|\n|\r".toRegex(), "")
        val urlStr = "http://url.ext/action?opId=GET_LIST&Version=v2.0"
        val requestData = mockk<Requestdata> {
            every { httpUrl } returns urlStr.asHttpUrl
            every { body } returns reqBody
        }

        testObject.also {
            val uList = listOf(
                UniqueBit("opId=GET_LIST", UniqueTypes.Query),
                UniqueBit("EndPoint[^\\d]+\\d+", UniqueTypes.Body),
                UniqueBit("ForId[^\\d]+\\d+", UniqueTypes.Body)
            )
            it.byUnique = listOf(uList)
        }

        val result = testObject.createNewInteraction {
            it.requestData = requestData
        }

        Assert.assertNotNull(result.attractors)
        Assert.assertEquals(1, result.attractors?.queryMatchers?.size)
        Assert.assertEquals(2, result.attractors?.bodyMatchers?.size)
    }
}
