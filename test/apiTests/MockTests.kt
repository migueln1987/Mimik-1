package apiTests

import helpers.isJSONValid
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.parseQueryString
import io.ktor.http.plus
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import org.junit.Assert
import org.junit.Test
import java.io.File

class MockTests {

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
                addHeader("mockTape_Name", "Google")
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
                addHeader("mockTape_Name", "Google")
                addHeader("mockTape_Only", "true")
            }

            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "Google")
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
                addHeader("mockTape_Name", "Google")
                addHeader("mockName", "GetMail")
            }.apply {
                response {
                    Assert.assertEquals(HttpStatusCode.Created, it.status())
                }
            }
        }
    }

    @Test
    fun testCreateMock_FindMock() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "Google")
                addHeader("mockName", "GetMail")
            }

            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", "Google")
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
        Assert.assertTrue(testBody.isJSONValid)

        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockMethod", "POST")
                addHeader("mockResponseCode", "200")
                addHeader("mockRoute_path", "/mail")
                setBody(testBody)
            }

            handleRequest(HttpMethod.Post, "/mail", {})
                .apply {
                    response {
                        Assert.assertEquals(HttpStatusCode.OK, it.status())
                        Assert.assertEquals(testBody, it.content)
                    }
                }
        }
    }

    @Test
    fun requiredFilterPriority() {
        val testingTapes = arrayOf("TestingTape1", "TestingTape2")
        TapeCatalog.Instance.tapes.filter { it.tapeName in testingTapes }
            .forEach {
                it.file?.delete()
                TapeCatalog.Instance.tapes.remove(it)
            }

        TestApp {
            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", testingTapes[0])
                addHeader("mockTape_Only", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockFilter_Path", "/long/path/name")
            }

            handleRequest(HttpMethod.Put, "/mock") {
                addHeader("mockTape_Name", testingTapes[1])
                addHeader("mockTape_Only", "true")
                addHeader("mockTape_Url", "http://valid.url")
                addHeader("mockFilter_Path", "/long/path/name")
                addHeader("mockFilter_Param", "Param=Value")
            }

            var recievedResponse = false
            handleRequest(HttpMethod.Post, "/long/path/name") {
                setBody("Test1")
            }.also {
                it.response {
                    handleRequest(HttpMethod.Post, "/long/path/name?Param=Value") {
                        setBody("Test2")
                    }.also { it.response { recievedResponse = true } }
                }
            }

            val tapes = TapeCatalog.Instance.tapes
            Assert.assertEquals(2, tapes.size)

            val tape1 = tapes.firstOrNull { it.tapeName == testingTapes[0] }
            Assert.assertNotNull(tape1)
            requireNotNull(tape1)
            Assert.assertTrue(tape1.chapters.any { it.request.bodyAsText() == "Test1" })

            val tape2 = tapes.firstOrNull { it.tapeName == testingTapes[1] }
            Assert.assertNotNull(tape2)
            requireNotNull(tape2)
            Assert.assertTrue(tape2.chapters.any { it.request.bodyAsText() == "Test2" })
        }

        TapeCatalog.Instance.tapes.filter { it.tapeName in testingTapes }
            .forEach {
                it.file?.delete()
                TapeCatalog.Instance.tapes.remove(it)
            }
    }
}
