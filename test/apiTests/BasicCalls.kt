package apiTests

import helpers.isJSONValid
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import org.junit.Assert
import org.junit.Test

class BasicCalls {

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
}
