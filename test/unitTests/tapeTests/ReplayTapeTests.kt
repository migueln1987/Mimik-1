package com.fiserv.mimik.tapeTests

import helpers.attractors.RequestAttractorBit
import helpers.attractors.RequestAttractors
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import mimikMockHelpers.MockUseStates
import mimikMockHelpers.RecordedInteractions
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Protocol
import okreplay.Request
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import tapeItems.BlankTape

class ReplayTapeTests {
    lateinit var testObject: BlankTape

    @Before
    fun setup() {
        testObject = BlankTape.Builder().build()
    }

    @Test
    fun seekTest_NotFound() {
        val request = mockk<Request> {
            every { url() } returns HttpUrl.get("http://valid.url/path")
            every { hasBody() } returns false
            every { headers() } returns Headers.of("Key", "Value")
            every { method() } returns "GET"
        }

        val foundData = testObject.seek(request)
        Assert.assertFalse(foundData)
    }

    @Test
    fun seekTest() {
        val chapter = RecordedInteractions {
            it.mockUses = MockUseStates.SINGLEMOCK.state
            it.attractors = RequestAttractors { attr ->
                attr.routingPath = RequestAttractorBit("/path")
            }
        }
        testObject.chapters.add(chapter)

        val request = mockk<Request> {
            every { url() } returns HttpUrl.get("http://valid.url/path")
            every { hasBody() } returns false
            every { headers() } returns Headers.of("Key", "Value")
            every { method() } returns "GET"
        }

        val foundData = testObject.seek(request)
        Assert.assertTrue(foundData)
    }

    @Test
    fun playMockChapter() {
        val mockBodyMessage = "body_Mock"
        val chapterMock = RecordedInteractions {
            it.mockUses = MockUseStates.ALWAYS.state
            it.attractors = RequestAttractors { attr ->
                attr.routingPath = RequestAttractorBit("/path")
            }
            it.response = mockk {
                every { hasBody() } returns true
                every { bodyAsText() } returns mockBodyMessage
                every { code() } returns HttpStatusCode.OK.value
                every { protocol() } returns Protocol.HTTP_1_1
                every { headers() } returns Headers.of("key", "value")
            }
        }

        testObject.chapters.add(chapterMock)

        val request = mockk<Request> {
            every { url() } returns HttpUrl.get("http://valid.url/path")
            every { hasBody() } returns false
            every { method() } returns "GET"
            every { headers() } returns Headers.of("key", "playMockChapter")
        }

        val response = testObject.play(request)

        Assert.assertEquals(mockBodyMessage, response.bodyAsText())
    }

    @Test
    fun playMockThenLive() {
        val mockBodyMessage = "body_Mock"
        val chapterMock = RecordedInteractions {
            it.mockUses = MockUseStates.SINGLEMOCK.state
            it.attractors = RequestAttractors { attr ->
                attr.routingPath = RequestAttractorBit("/path")
            }
            it.response = mockk {
                every { hasBody() } returns true
                every { bodyAsText() } returns mockBodyMessage
                every { code() } returns HttpStatusCode.OK.value
                every { protocol() } returns Protocol.HTTP_1_1
                every { headers() } returns Headers.of("key", "value")
            }
        }

        val liveBodyMessage = "body_Live"
        val chapterLive = RecordedInteractions {
            it.mockUses = MockUseStates.ALWAYS.state
            it.attractors = RequestAttractors { attr ->
                attr.routingPath = RequestAttractorBit("/path")
            }
            it.response = mockk {
                every { hasBody() } returns true
                every { bodyAsText() } returns liveBodyMessage
                every { code() } returns HttpStatusCode.OK.value
                every { protocol() } returns Protocol.HTTP_1_1
                every { headers() } returns Headers.of("key", "value")
            }
        }

        testObject.chapters.add(chapterMock)
        testObject.chapters.add(chapterLive)

        val request = mockk<Request> {
            every { url() } returns HttpUrl.get("http://valid.url/path")
            every { hasBody() } returns false
            every { headers() } returns Headers.of("key", "value")
            every { method() } returns "GET"
        }

        var response = testObject.play(request)

        // Expect to receive the chapterMock data
        Assert.assertEquals(mockBodyMessage, response.bodyAsText())
        Assert.assertTrue(MockUseStates.isDisabled(chapterMock.mockUses))

        // Expect to receive the chapterLive data
        response = testObject.play(request)
        Assert.assertEquals(liveBodyMessage, response.bodyAsText())
    }
}
