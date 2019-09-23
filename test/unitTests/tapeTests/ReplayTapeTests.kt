package com.fiserv.mimik.tapeTests

import helpers.attractors.RequestAttractors
import io.mockk.every
import io.mockk.mockk
import mimikMockHelpers.InteractionUseStates
import mimikMockHelpers.RecordedInteractions
import okhttp3.Headers
import okhttp3.HttpUrl
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
        val request = mockk<Request>()
        every { request.url() } returns HttpUrl.get("http://valid.url/path")
        every { request.hasBody() } returns false

        val foundData = testObject.seek(request)
        Assert.assertFalse(foundData)
    }

    @Test
    fun seekTest() {
        val chapter = RecordedInteractions {
            it.mockUses = 1
            it.attractors = RequestAttractors { attr ->
                attr.routingPath = "/path"
            }
        }
        testObject.chapters.add(chapter)

        val request = mockk<Request>()
        every { request.url() } returns HttpUrl.get("http://valid.url/path")
        every { request.hasBody() } returns false

        val foundData = testObject.seek(request)
        Assert.assertTrue(foundData)
    }

    @Test
    fun playMockChapter() {
        val mockBodyMessage = "body_Mock"
        val mockResponseForMock = mockk<okreplay.Response>() {
            every { bodyAsText() } returns mockBodyMessage
        }
        val mockUseCount = 1
        val chapterMock = RecordedInteractions {
            it.mockUses = mockUseCount
            it.attractors = RequestAttractors { attr ->
                attr.routingPath = "/path"
            }
            it.response = mockResponseForMock
        }

        testObject.chapters.add(chapterMock)

        val request = mockk<Request> {
            every { url() } returns HttpUrl.get("http://valid.url/path")
            every { hasBody() } returns false
            every { headers() } returns Headers.of("key", "playMockChapter")
        }

        val response = testObject.play(request)

        Assert.assertEquals(response.bodyAsText(), mockBodyMessage)
        Assert.assertTrue(chapterMock.mockUses < mockUseCount)
    }

    @Test
    fun playMockThenLive() {
        val mockBodyMessage = "body_Mock"
        val mockResponseForMock = mockk<okreplay.Response>() {
            every { bodyAsText() } returns mockBodyMessage
        }
        val mockUseCount = InteractionUseStates.SINGLEMOCK
        val chapterMock = RecordedInteractions {
            it.mockUses = mockUseCount.state
            it.attractors = RequestAttractors { attr ->
                attr.routingPath = "/path"
            }
            it.response = mockResponseForMock
        }

        val liveBodyMessage = "body_Live"
        val mockResponseForLive = mockk<okreplay.Response>() {
            every { bodyAsText() } returns liveBodyMessage
        }
        val chapterLive = RecordedInteractions {
            it.mockUses = InteractionUseStates.ALWAYS.state
            it.attractors = RequestAttractors { attr ->
                attr.routingPath = "/path"
            }
            it.response = mockResponseForLive
        }

        testObject.chapters.add(chapterMock)
        testObject.chapters.add(chapterLive)

        val request = mockk<Request>()
        every { request.url() } returns HttpUrl.get("http://valid.url/path")
        every { request.hasBody() } returns false

        var response = testObject.play(request)

        // Expect to receive the chapterMock data
        Assert.assertEquals(response.bodyAsText(), mockBodyMessage)
        Assert.assertEquals(chapterMock.mockUses, mockUseCount.asDisabled.state)

        // Expect to receive the chapterLive data
        response = testObject.play(request)
        Assert.assertEquals(response.bodyAsText(), liveBodyMessage)
    }

}
