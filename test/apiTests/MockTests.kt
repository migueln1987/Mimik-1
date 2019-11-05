package apiTests

import VCRConfig
import com.beust.klaxon.internal.firstNotNullResult
import helpers.isValidJSON
import helpers.isTrue
import helpers.orFalse
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.mockk.unmockkObject
import mimikMockHelpers.MockUseStates
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class MockTests {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            setupVCRConfig("test/apiTests")
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            unmockkObject(VCRConfig)
        }
    }

    @Before
    fun clearTapes() {
        TapeCatalog.Instance.tapes.forEach { it.file?.delete() }
        TapeCatalog.Instance.tapes.clear()
    }

    @Test
    fun testCreateTape_NoParams() {
        TestApp {
            val call = handleRequest(HttpMethod.Put, "/mock")

            call.response {
                Assert.assertEquals(HttpStatusCode.BadRequest, it.status())
                assertStartsWith("Missing mock params", it.content)
            }
        }
    }

    @Test
    fun testCreateTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "testCreateTape")
                addHeader("mockTape_Only", "true")
            }.apply {
                response {
                    Assert.assertEquals(HttpStatusCode.Created, it.status())
                }
            }
        }
    }

    @Test
    fun testUseTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "testUseTape")
                addHeader("mockTape_Only", "true")
            }

            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "testUseTape")
                addHeader("mockTape_Only", "true")
            }.apply {
                response {
                    Assert.assertEquals(HttpStatusCode.Found, it.status())
                }
            }
        }
    }

    @Test
    fun testCreateTape_CreateMock() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "testCreateTape_CreateMock")
                addHeader("mockName", "GetMail")
            }.apply {
                response {
                    Assert.assertEquals(HttpStatusCode.Created, it.status())
                }
            }
        }
    }

    @Test
    fun testCreateMock_FindTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "FindTape")
                addHeader("mockName", "GetMail")
            }

            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "FindTape")
                addHeader("mockName", "GetMail")
            }.apply {
                response {
                    Assert.assertEquals(HttpStatusCode.Found, it.status())
                }
            }
        }
    }

    @Test
    fun useMock() {
        val testBody = "{\"test\":true }"
        Assert.assertTrue(testBody.isValidJSON)

        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "useMock")
                addHeader("mockMethod", "POST")
                addHeader("mockResponseCode", "200")
                addHeader("mockFilter_Path", "/mail")
                setBody(testBody)
            }

            handleRequest(HttpMethod.Post, "/mail")
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.OK, it.status())
                        Assert.assertEquals(testBody, it.content)
                    }
                }
        }
    }

    @Test // This test filters each call (noParam, newParam, matchParam) into the respected tapes
    fun requiredFilterPriority() {
        val testingTapes = arrayOf("TestingTape1", "TestingTape2")

        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", testingTapes[0])
                addHeader("mockTape_Only", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockFilter_Path", "/long/path/name")
                addHeader("mockFilter_Param", "Param")
            }

            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", testingTapes[1])
                addHeader("mockTape_Only", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockFilter_Path", "/long/path/name")
                addHeader("mockFilter_Param", "Param=Value")
            }

            handleRequest(HttpMethod.Post, "/long/path/name") {
                setBody("noParam")
            }

            handleRequest(HttpMethod.Post, "/long/path/name?Param=New") {
                setBody("newParam")
            }

            handleRequest(HttpMethod.Post, "/long/path/name?Param=Value") {
                setBody("matchParam")
            }

            val tapes = TapeCatalog.Instance.tapes
            Assert.assertTrue(tapes.isNotEmpty())

            val tape1 = tapes.firstOrNull { it.name == testingTapes[0] }
            Assert.assertNotNull(tape1)
            requireNotNull(tape1)
            val tape2 = tapes.firstOrNull { it.name == testingTapes[1] }
            Assert.assertNotNull(tape2)
            requireNotNull(tape2)

            // check that tape 1 and tape 2 have the expected calls by filter
            Assert.assertTrue(tape1.chapters.any { it.requestData?.body == "newParam" })
            Assert.assertTrue(tape2.chapters.any { it.requestData?.body == "matchParam" })

            // tape 1 and 2 should not have the non-filtering call
            Assert.assertTrue(tape1.chapters.none { it.requestData?.body == "noParam" })
            Assert.assertTrue(tape2.chapters.none { it.requestData?.body == "noParam" })

            val hasNoParamCall = tapes.any { tape ->
                tape.chapters.any { it.requestData?.body == "noParam" }
            }
            // new call should have created it's own tape
            Assert.assertTrue(hasNoParamCall)
        }
    }

    @Test
    fun awaitTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "awaitTape")
                addHeader("mockTape_Url", "http://valid.url")

                addHeader("mockAwait", "true")
                addHeader("mockFilter_Path", ".*")
            }

            handleRequest(HttpMethod.Post, "/awaittape_test") {
                request {
                    setBody("awaittape")
                }
            }
                .apply {
                    response {
                        val interaction = TapeCatalog.Instance.tapes
                            .firstNotNullResult { tape ->
                                tape.chapters.firstNotNullResult {
                                    if (it.hasResponseData && it.responseData?.body == "awaittape")
                                        it else null
                                }
                            }

                        Assert.assertNotNull(interaction)
                        requireNotNull(interaction)

                        Assert.assertFalse(interaction.awaitResponse)
                        Assert.assertTrue(interaction.hasResponseData)
                    }
                }
        }
    }

    @Test
    fun newCallCreatedAwaitTape() {
        val path = "await_test"
        TestApp {
            handleRequest(HttpMethod.Post, "/$path") {
                request {
                    setBody("testBody")
                }
            }
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.NotFound, it.status())

                        val interaction = TapeCatalog.Instance.tapes
                            .firstNotNullResult { tape ->
                                tape.chapters.firstOrNull()
                            }

                        Assert.assertNotNull(interaction)
                        requireNotNull(interaction)

                        Assert.assertTrue(interaction.awaitResponse)
                        Assert.assertFalse(interaction.hasResponseData)

                        Assert.assertEquals(path, interaction.attractors?.routingPath?.value)
                    }
                }
        }
    }

    @Test
    fun exceptTest() {
        val avoidBody = "avoidBody"
        val successBody = "successBody"

        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/path")
                addHeader("mockFilter_Body!", "avoid")
                // wild-card "all", to allow this mock to accept any body (addition to above filter)
                addHeader("mockFilter_Body", ".*")
                setBody(avoidBody)
            }

            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/path")
                setBody(successBody)
            }

            // this call should be filtered into the mock who does not allow a body containing "avoid"
            handleRequest(HttpMethod.Post, "/path") {
                setBody("avoidThis")
            }.apply {
                response {
                    Assert.assertEquals(successBody, it.content)
                }
            }

            handleRequest(HttpMethod.Post, "/path") {
                setBody("anyBody")
            }.apply {
                response {
                    // matches by path and "any body", but also doesn't contain "avoid"
                    Assert.assertEquals(avoidBody, it.content)
                }
            }
        }
    }

    @Test
    fun headersOutTest() {
        val headerKey = "headerKey"
        val headerValue = "headerValue"

        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/path")
                addHeader("mockHeaderOut_$headerKey", headerValue)
            }

            handleRequest(HttpMethod.Post, "/path", {}).apply {
                response {
                    Assert.assertTrue(it.headers.contains(headerKey))
                    Assert.assertEquals(headerValue, it.headers[headerKey])
                }
            }
        }
    }

    @Test
    fun alwaysLiveMock_NoUrl() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockMethod", "POST")
                addHeader("mockResponseCode", "200")
                addHeader("mockFilter_Path", "/mail")
                addHeader("mockLive", "true")
            }

            val mock = TapeCatalog.Instance.tapes.firstOrNull()
                ?.chapters?.firstOrNull()

            Assert.assertNotNull(mock)
            Assert.assertTrue(mock?.alwaysLive.orFalse)
            Assert.assertNull(mock?.responseData)

            handleRequest(HttpMethod.Post, "/mail")
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.BadGateway, it.status())
                    }
                }
        }
    }

    @Test
    fun alwaysLiveMock() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockLive", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/mail")
            }

            val mock = TapeCatalog.Instance.tapes.firstOrNull()
                ?.chapters?.firstOrNull()

            Assert.assertNotNull(mock)
            requireNotNull(mock)
            Assert.assertTrue(mock.alwaysLive.orFalse)
            Assert.assertNull(mock.responseData)

            handleRequest(HttpMethod.Post, "/mail")
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.OK, it.status())
                        Assert.assertNull(mock.responseData)
                    }
                }
        }
    }

    @Test // Runs the live mock a limited time, then mocking response
    fun alwaysLiveLimitedMock() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "liveTape")
                addHeader("mockLive", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockTape_Only", "true")
            }

            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "liveTape")
                addHeader("mockName", "mock_limited")
                addHeader("mockLive", "true")
                addHeader("mockFilter_Path", "/mail")
                addHeader("mockUse", MockUseStates.SINGLEMOCK.state.toString())
            }

            val mockBody = "TestBody"
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "liveTape")
                addHeader("mockName", "mock_full")
                addHeader("mockFilter_Path", "/mail")
                addHeader("mockMethod", "POST")
                setBody(mockBody)
            }

            val mock = TapeCatalog.Instance.tapes.firstOrNull()
                ?.chapters?.firstOrNull()

            Assert.assertNotNull(mock)
            requireNotNull(mock)
            Assert.assertTrue(mock.alwaysLive.orFalse)
            Assert.assertNull(mock.responseData)

            handleRequest(HttpMethod.Post, "/mail")
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.OK, it.status())
                        Assert.assertTrue(MockUseStates.isDisabled(mock.mockUses))
                        Assert.assertNull(mock.responseData)
                        Assert.assertNotEquals(mockBody, it.content)
                    }
                }

            handleRequest(HttpMethod.Post, "/mail")
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.OK, it.status())
                        Assert.assertEquals(mockBody, it.content)
                    }
                }
        }
    }

    @Test
    fun alwaysLiveTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockFilter_Path", "/mail")
                addHeader("mockLive", "true")
                addHeader("mockTape_Only", "true")
            }

            val tape = TapeCatalog.Instance.tapes.firstOrNull()
                ?.also {
                    Assert.assertTrue(it.chapters.isEmpty())
                }

            Assert.assertNotNull(tape)
            requireNotNull(tape)
            Assert.assertTrue(tape.alwaysLive.orFalse)

            handleRequest(HttpMethod.Post, "/mail")
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.OK, it.status())
                        Assert.assertTrue(tape.chapters.isNotEmpty())
                        Assert.assertTrue(tape.chapters.first().alwaysLive.isTrue())
                    }
                }
        }
    }
}
