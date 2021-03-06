package apiTests

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.collections.firstNotNullResult
import kotlinx.isTrue
import kotlinx.isValidJSON
import kotlinx.orFalse
import mimik.Ports
import mimik.mockHelpers.MockUseStates
import mimik.tapeItems.MimikContainer
import org.junit.Assert

class MockTests : ApiTests {
    val tapeCatalog get() = MimikContainer.tapeCatalog

    // todo; pending new API changes
    // @Test
    fun test_MockCall() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui).response {
                Assert.assertEquals(HttpStatusCode.ExpectationFailed, it.status())
            }
        }
    }

    // todo; pending new API changes
    // @Test
    fun testCreate_NoParams() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock/tape", Ports.gui).response {
                Assert.assertEquals(HttpStatusCode.BadRequest, it.status())
                assertStartsWith("Missing mock params", it.content)
            }

            handleRequest(HttpMethod.Put, "/mock/chapter", Ports.gui).response {
                Assert.assertEquals(HttpStatusCode.BadRequest, it.status())
                assertStartsWith("Missing mock params", it.content)
            }
        }
    }

    // todo; pending new API changes
    // @Test
    fun test_CreateFind() {
        TestApp {
            val tapeName = "tapeName"
            handleRequest(HttpMethod.Put, "/mock/tape", Ports.gui) {
                addHeader("mockName", tapeName)
            }.response {
                Assert.assertEquals(HttpStatusCode.Created, it.status())
            }

            handleRequest(HttpMethod.Put, "/mock/tape", Ports.gui) {
                addHeader("mockName", tapeName)
            }.response {
                Assert.assertEquals(HttpStatusCode.Found, it.status())
            }

            handleRequest(HttpMethod.Put, "/mock/chapter", Ports.gui) {
                addHeader("mockTapeName", tapeName)
                addHeader("mockName", "GetMail")
            }.response {
                Assert.assertEquals(HttpStatusCode.Created, it.status())
            }

            handleRequest(HttpMethod.Put, "/mock/chapter", Ports.gui) {
                addHeader("mockTapeName", tapeName)
                addHeader("mockName", "GetMail")
            }.response {
                Assert.assertEquals(HttpStatusCode.Found, it.status())
            }
        }
    }

    // todo; pending new API changes
    // @Test
    fun useMock() {
        val testBody = "{\"test\":true }"
        Assert.assertTrue(testBody.isValidJSON)

        TestApp {
            handleRequest(HttpMethod.Put, "/mock/tape", Ports.gui) {
                addHeader("mockName", "useMock")
            }

            handleRequest(HttpMethod.Put, "/mock/chapter", Ports.gui) {
                addHeader("mockTapeName", "useMock")
                addHeader("mockMethod", "POST")
                addHeader("mockResponseCode", "200")
                addHeader("mockFilter_Path", "/mail")
                setBody(testBody)
            }

            handleRequest(HttpMethod.Post, "/mail", Ports.mock)
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.OK, it.status())
                        Assert.assertEquals(testBody, it.content)
                    }
                }
        }
    }

    // todo; pending new API changes
    // @Test // This test filters each call (queryNone, queryNew, queryMatch) into the respected tapes
    fun requiredFilterPriority() {
        val testingTapes = arrayOf("TestingTape1", "TestingTape2")
        val queryNone = "queryNone"
        val queryNew = "queryNew"
        val queryMatch = "queryMatch"

        TestApp {
            handleRequest(HttpMethod.Put, "/mock/tape", Ports.gui) {
                addHeader("mockName", testingTapes[0])
                addHeader("mockUrl", "http://valid.url")
                addHeader("mockFilter_Path", "/long/path/name")
                addHeader("mockFilter_Query", "Query.+")
            }

            handleRequest(HttpMethod.Put, "/mock/tape", Ports.gui) {
                addHeader("mockName", testingTapes[1])
                addHeader("mockUrl", "http://valid.url")
                addHeader("mockFilter_Path", "/long/path/name")
                addHeader("mockFilter_Query", "Query=Value")
            }

            handleRequest(HttpMethod.Post, "/long/path/name", Ports.mock) {
                setBody(queryNone) // should create a new tape & chapter
            }

            handleRequest(HttpMethod.Post, "/long/path/name?Query=New", Ports.mock) {
                setBody(queryNew)
            }

            handleRequest(HttpMethod.Post, "/long/path/name?Query=Value", Ports.mock) {
                setBody(queryMatch)
            }

            val tapes = tapeCatalog.tapes
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

    // todo; pending new API changes
    // @Test
    fun awaitTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockTape_Name", "awaitTape")
                addHeader("mockTape_Url", "http://valid.url")

                addHeader("mockAwait", "true")
                addHeader("mockFilter_Path", ".*")
            }

            handleRequest(HttpMethod.Post, "/awaittape_test", Ports.mock) {
                request {
                    setBody("awaittape")
                }
            }
                .apply {
                    response {
                        val interaction = tapeCatalog.tapes
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

    // todo; pending new API changes
    // @Test
    fun newCallCreatedAwaitTape() {
        val path = "await_test"
        TestApp {
            handleRequest(HttpMethod.Post, "/$path", Ports.mock) {
                request {
                    setBody("testBody")
                }
            }
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.NotFound, it.status())

                        val interaction = tapeCatalog.tapes
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

    // todo; pending new API changes
    // @Test
    fun exceptTest() {
        val avoidBody = "avoidBody"
        val successBody = "successBody"

        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/path")
                // avoid a body which contains "avoid" anywhere in the string
                addHeader("mockFilter_Body!", "avoid")
                setBody(avoidBody)
            }

            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/path")
                setBody(successBody)
            }

            // this call should be filtered into the mock who does not allow a body containing "avoid"
            handleRequest(HttpMethod.Post, "/path", Ports.mock) {
                setBody("avoidThis")
            }.apply {
                response {
                    Assert.assertEquals(successBody, it.content)
                }
            }

            handleRequest(HttpMethod.Post, "/path", Ports.mock) {
                setBody("anyBody")
            }.apply {
                response {
                    // matches by path and "any body", but also doesn't contain "avoid"
                    Assert.assertEquals(avoidBody, it.content)
                }
            }
        }
    }

    // todo; pending new API changes
    // @Test
    fun headersOutTest() {
        val headerKey = "headerKey"
        val headerValue = "headerValue"

        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/path")
                addHeader("mockHeaderOut_$headerKey", headerValue)
            }

            handleRequest(HttpMethod.Post, "/path", Ports.mock) {}.apply {
                response {
                    Assert.assertTrue(it.headers.contains(headerKey))
                    Assert.assertEquals(headerValue, it.headers[headerKey])
                }
            }
        }
    }

    // todo; pending new API changes
    // @Test
    fun alwaysLiveMock_NoUrl() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockMethod", "POST")
                addHeader("mockResponseCode", "200")
                addHeader("mockFilter_Path", "/mail")
                addHeader("mockLive", "true")
            }

            val mock = tapeCatalog.tapes.firstOrNull()
                ?.chapters?.firstOrNull()

            Assert.assertNotNull(mock)
            Assert.assertTrue(mock?.alwaysLive.orFalse)
            Assert.assertNull(mock?.responseData)

            handleRequest(HttpMethod.Post, "/mail", Ports.mock)
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.BadGateway, it.status())
                    }
                }
        }
    }

    // todo; pending new API changes
    // @Test
    fun alwaysLiveMock() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockLive", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockMethod", "POST")
                addHeader("mockFilter_Path", "/mail")
            }

            val mock = tapeCatalog.tapes.firstOrNull()
                ?.chapters?.firstOrNull()

            Assert.assertNotNull(mock)
            requireNotNull(mock)
            Assert.assertTrue(mock.alwaysLive.orFalse)
            Assert.assertNull(mock.responseData)

            handleRequest(HttpMethod.Post, "/mail", Ports.mock)
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.OK, it.status())
                        Assert.assertNull(mock.responseData)
                    }
                }
        }
    }

    // todo; pending new API changes
    // @Test // Runs the live mock a limited time, then mocking response
    fun alwaysLiveLimitedMock() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockTape_Name", "liveTape")
                addHeader("mockLive", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockTape_Only", "true")
            }

            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockTape_Name", "liveTape")
                addHeader("mockName", "mock_limited")
                addHeader("mockLive", "true")
                addHeader("mockFilter_Path", "/mail")
                addHeader("mockUse", MockUseStates.SINGLEMOCK.state.toString())
                addHeader("mockMethod", "POST")
                setBody("mock_limited")
            }

            val mockBody = "mock_full"
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockTape_Name", "liveTape")
                addHeader("mockName", "mock_full")
                addHeader("mockFilter_Path", "/mail")
                addHeader("mockMethod", "POST")
                setBody(mockBody)
            }

            val mock = tapeCatalog.tapes.firstOrNull()
                ?.chapters?.firstOrNull { it.name == "mock_limited" }

            Assert.assertNotNull(mock)
            requireNotNull(mock)
            Assert.assertTrue(mock.alwaysLive.orFalse)
            Assert.assertNull(mock.responseData)

            handleRequest(HttpMethod.Post, "/mail", Ports.mock)
                .response {
                    Assert.assertEquals(HttpStatusCode.OK, it.status())
                    Assert.assertTrue(MockUseStates.isDisabled(mock.mockUses))
                    Assert.assertNull(mock.responseData)
                    Assert.assertNotEquals(mockBody, it.content)
                }

            handleRequest(HttpMethod.Post, "/mail", Ports.mock)
                .response {
                    Assert.assertEquals(HttpStatusCode.OK, it.status())
                    Assert.assertEquals(mockBody, it.content)
                }
        }
    }

    // todo; pending new API changes
    // @Test
    fun alwaysLiveTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockFilter_Path", "/mail")
                addHeader("mockLive", "true")
                addHeader("mockTape_Only", "true")
            }

            val tape = tapeCatalog.tapes.firstOrNull()
                ?.also {
                    Assert.assertTrue(it.chapters.isEmpty())
                }

            Assert.assertNotNull(tape)
            requireNotNull(tape)
            Assert.assertTrue(tape.alwaysLive.orFalse)

            handleRequest(HttpMethod.Post, "/mail", Ports.mock)
                .response {
                    Assert.assertEquals(HttpStatusCode.OK, it.status())
                    Assert.assertTrue(tape.chapters.isNotEmpty())
                    Assert.assertTrue(tape.chapters.first().alwaysLive.isTrue)
                }
        }
    }
}
