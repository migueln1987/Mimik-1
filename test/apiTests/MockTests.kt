package apiTests

import com.beust.klaxon.internal.firstNotNullResult
import com.fiserv.mimik.Ports
import helpers.isValidJSON
import helpers.isTrue
import helpers.orFalse
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import mimikMockHelpers.MockUseStates
import org.junit.*

class MockTests : ApiTests {
    @Test
    fun testCreateTape_NoParams() {
        TestApp {
            val call = handleRequest(HttpMethod.Put, "/mock", Ports.config)

            call.response {
                Assert.assertEquals(HttpStatusCode.BadRequest, it.status())
                assertStartsWith("Missing mock params", it.content)
            }
        }
    }

    @Test
    fun testCreateTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
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
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "testUseTape")
                addHeader("mockTape_Only", "true")
            }

            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
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
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
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
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "FindTape")
                addHeader("mockName", "GetMail")
            }

            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
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
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "useMock")
                addHeader("mockMethod", "POST")
                addHeader("mockResponseCode", "200")
                addHeader("mockFilter_Path", "/mail")
                setBody(testBody)
            }

            handleRequest(HttpMethod.Post, "/mail", Ports.live)
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.OK, it.status())
                        Assert.assertEquals(testBody, it.content)
                    }
                }
        }
    }

    @Test // This test filters each call (queryNone, queryNew, queryMatch) into the respected tapes
    fun requiredFilterPriority() {
        val testingTapes = arrayOf("TestingTape1", "TestingTape2")
        val queryNone = "queryNone"
        val queryNew = "queryNew"
        val queryMatch = "queryMatch"

        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", testingTapes[0])
                addHeader("mockTape_Only", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockFilter_Path", "/long/path/name")
                addHeader("mockFilter_Query", "Query.+")
            }

            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", testingTapes[1])
                addHeader("mockTape_Only", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockFilter_Path", "/long/path/name")
                addHeader("mockFilter_Query", "Query=Value")
            }

            handleRequest(HttpMethod.Post, "/long/path/name", Ports.live) {
                setBody(queryNone) // should create a new tape & chapter
            }

            handleRequest(HttpMethod.Post, "/long/path/name?Query=New", Ports.live) {
                setBody(queryNew)
            }

            handleRequest(HttpMethod.Post, "/long/path/name?Query=Value", Ports.live) {
                setBody(queryMatch)
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
            Assert.assertTrue(tape1.chapters.any { it.requestData?.body == queryNew })
            Assert.assertTrue(tape2.chapters.any { it.requestData?.body == queryMatch })

            // tape 1 and 2 should not have the non-filtering call
            Assert.assertTrue(tape1.chapters.none { it.requestData?.body == queryNone })
            Assert.assertTrue(tape2.chapters.none { it.requestData?.body == queryNone })

            val hasNoQueryCall = tapes.any { tape ->
                tape.chapters.any { it.requestData?.body == queryNone }
            }
            // new call should have created it's own tape
            Assert.assertTrue(hasNoQueryCall)
        }
    }

    @Test
    fun awaitTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "awaitTape")
                addHeader("mockTape_Url", "http://valid.url")

                addHeader("mockAwait", "true")
                addHeader("mockFilter_Path", ".*")
            }

            handleRequest(HttpMethod.Post, "/awaittape_test", Ports.live) {
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
            handleRequest(HttpMethod.Post, "/$path", Ports.live) {
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
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/path")
                // avoid a body which contains "avoid" anywhere in the string
                addHeader("mockFilter_Body!", "avoid")
                setBody(avoidBody)
            }

            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/path")
                setBody(successBody)
            }

            // this call should be filtered into the mock who does not allow a body containing "avoid"
            handleRequest(HttpMethod.Post, "/path", Ports.live) {
                setBody("avoidThis")
            }.apply {
                response {
                    Assert.assertEquals(successBody, it.content)
                }
            }

            handleRequest(HttpMethod.Post, "/path", Ports.live) {
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
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/path")
                addHeader("mockHeaderOut_$headerKey", headerValue)
            }

            handleRequest(HttpMethod.Post, "/path", Ports.live) {}.apply {
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
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
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

            handleRequest(HttpMethod.Post, "/mail", Ports.live)
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
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
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

            handleRequest(HttpMethod.Post, "/mail", Ports.live)
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
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "liveTape")
                addHeader("mockLive", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockTape_Only", "true")
            }

            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "liveTape")
                addHeader("mockName", "mock_limited")
                addHeader("mockLive", "true")
                addHeader("mockFilter_Path", "/mail")
                addHeader("mockUse", MockUseStates.SINGLEMOCK.state.toString())
                addHeader("mockMethod", "POST")
                setBody("mock_limited")
            }

            val mockBody = "mock_full"
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "liveTape")
                addHeader("mockName", "mock_full")
                addHeader("mockFilter_Path", "/mail")
                addHeader("mockMethod", "POST")
                setBody(mockBody)
            }

            val mock = TapeCatalog.Instance.tapes.firstOrNull()
                ?.chapters?.firstOrNull { it.name == "mock_limited" }

            Assert.assertNotNull(mock)
            requireNotNull(mock)
            Assert.assertTrue(mock.alwaysLive.orFalse)
            Assert.assertNull(mock.responseData)

            handleRequest(HttpMethod.Post, "/mail", Ports.live)
                .response {
                    Assert.assertEquals(HttpStatusCode.OK, it.status())
                    Assert.assertTrue(MockUseStates.isDisabled(mock.mockUses))
                    Assert.assertNull(mock.responseData)
                    Assert.assertNotEquals(mockBody, it.content)
                }


            handleRequest(HttpMethod.Post, "/mail", Ports.live)
                .response {
                    Assert.assertEquals(HttpStatusCode.OK, it.status())
                    Assert.assertEquals(mockBody, it.content)
                }
        }
    }

    @Test
    fun alwaysLiveTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
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

            handleRequest(HttpMethod.Post, "/mail", Ports.live)
                .response {
                    Assert.assertEquals(HttpStatusCode.OK, it.status())
                    Assert.assertTrue(tape.chapters.isNotEmpty())
                    Assert.assertTrue(tape.chapters.first().alwaysLive.isTrue())
                }
        }
    }
}
