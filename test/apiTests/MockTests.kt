package apiTests

import com.beust.klaxon.internal.firstNotNullResult
import helpers.containsPath
import helpers.isJSONValid
import helpers.isTrue
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import org.junit.Assert
import org.junit.Test

class MockTests {
    /**
     * Cleanup tapes created during testing
     */
    private fun removeTapeByName(vararg name: String) {
        TapeCatalog.Instance.tapes.filter { it.name in name.toList() }
            .forEach {
                it.file?.delete()
                TapeCatalog.Instance.tapes.remove(it)
            }
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

        removeTapeByName("testCreateTape")
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

        removeTapeByName("testUseTape")
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

        removeTapeByName("testCreateTape_CreateMock")
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

        removeTapeByName("FindTape")
    }

    @Test
    fun useMock() {
        val testBody = "{\"test\":true }"
        Assert.assertTrue(testBody.isJSONValid)

        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "useMock")
                addHeader("mockMethod", "POST")
                addHeader("mockResponseCode", "200")
                addHeader("mockRoute_path", "/mail")
                addHeader("mockFilter_body~", ".*") // post ALWAYS has a body
                setBody(testBody)
            }

            handleRequest(HttpMethod.Post, "/mail") {}
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.OK, it.status())
                        Assert.assertEquals(testBody, it.content)
                    }
                }
        }

        removeTapeByName("useMock")
    }

    @Test // This test filters each call (noParam, newParam, matchParam) into the respected tapes
    fun requiredFilterPriority() {
        val testingTapes = arrayOf("TestingTape1", "TestingTape2")
        removeTapeByName(*testingTapes)

        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", testingTapes[0])
                addHeader("mockTape_Only", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockFilter_Path", "/long/path/name")
                addHeader("mockFilter_Param~", "Param")
                addHeader("mockFilter_Body~", ".*")
            }

            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", testingTapes[1])
                addHeader("mockTape_Only", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockFilter_Path", "/long/path/name")
                addHeader("mockFilter_Param", "Param=Value")
                addHeader("mockFilter_Body~", ".*")
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
            Assert.assertTrue(tape1.chapters.any { it.request.bodyAsText() == "noParam" })
            Assert.assertTrue(tape1.chapters.any { it.request.bodyAsText() == "newParam" })

            val tape2 = tapes.firstOrNull { it.name == testingTapes[1] }
            Assert.assertNotNull(tape2)
            requireNotNull(tape2)
            Assert.assertTrue(tape2.chapters.any { it.request.bodyAsText() == "matchParam" })
        }

        removeTapeByName(*testingTapes)
    }

    @Test
    fun awaitTape() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "awaitTape")
                addHeader("mockTape_Url", "http://valid.url")

                addHeader("mockAwait", "true")
                addHeader("mockFilter_Path", ".*")
                addHeader("mockFilter_Body~", ".*")
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
                                    if (it.hasResponseData && it.responseData.body == "awaittape")
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

        removeTapeByName("awaitTape")
    }

    @Test
    fun newCallCreatedAwaitTape() {
        var tapeName = ""

        TestApp {
            handleRequest(HttpMethod.Post, "/await_test") {
                request {
                    setBody("testBody")
                }
            }
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.NotFound, it.status())

                        val interaction = TapeCatalog.Instance.tapes
                            .firstNotNullResult { tape ->
                                tape.chapters.firstNotNullResult { chapter ->
                                    if (chapter.hasRequestData &&
                                        chapter.requestData.url?.containsPath("await_test").isTrue()
                                    ) {
                                        tapeName = tape.name
                                        chapter
                                    } else null
                                }
                            }

                        Assert.assertNotNull(interaction)
                        requireNotNull(interaction)

                        Assert.assertTrue(interaction.awaitResponse)
                        Assert.assertFalse(interaction.hasResponseData)
                    }
                }
        }

        if (tapeName.isNotEmpty())
            removeTapeByName(tapeName)
    }
}
