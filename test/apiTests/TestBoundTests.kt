package apiTests

import com.fiserv.mimik.Ports
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TestBoundTests : ApiTests {

    var currentTestID = ""

    private fun TestApplicationRequest.uniqueIDHeader() {
        addHeader("uniqueid", "123")
    }

    @Before
    fun boundStart() {
        TestApp {
            handleRequest(HttpMethod.Post, "/tests/start", Ports.config) {
                addHeader("tape", "##All")
                addHeader("time", "10m")
            }.apply {
                response {
                    currentTestID = it.headers["handle"].orEmpty()
                }
            }
        }

        Assert.assertNotEquals("", currentTestID)
    }

    @After
    fun boundEnd() {
        TestApp {
            handleRequest(HttpMethod.Post, "/tests/stop", Ports.config) {
                addHeader("handle", currentTestID)
            }.apply {
                response {
                    val boundTime = it.headers[currentTestID + "_time"].orEmpty()
                    println("Test bound run time: $boundTime")

                    Assert.assertEquals("1", it.headers["stopped"])
                }
            }

            handleRequest(HttpMethod.Post, "/tests/stop", Ports.config) {
                addHeader("handle", "$currentTestID, ##Finalize")
            }.apply {
                response {
                    Assert.assertTrue(it.headers.contains("finalized"))
                }
            }
        }
    }

    @Test
    fun emptyVarReturnsEmptyString() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "tape_use")
                addHeader("mock_Name", "chap_use")
                addHeader("mockFilter_Path", "test")
                addHeader("mockFilter_Body", ".*")
                setBody("Activation code: [test]")
            }

            handleRequest(HttpMethod.Post, "/tests/modify", Ports.config) {
                addHeader("handle", currentTestID)
                setBody(
                    """
                    {
                        "chap_use": [
                            "body{test->@{useCode}}"
                        ]
                    }
                """.trimIndent()
                )
            }

            handleRequest(HttpMethod.Post, "/test", Ports.live) {
                uniqueIDHeader()
                setBody("")
            }.apply {
                response {
                    Assert.assertEquals(
                        "Activation code: []",
                        it.content
                    )
                }
            }
        }
    }

    @Test
    fun emptyVarReturnsFirstDefault() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "tape_use")
                addHeader("mock_Name", "chap_use")
                addHeader("mockFilter_Path", "test")
                addHeader("mockFilter_Body", ".*")
                setBody("Activation code: [test]")
            }

            handleRequest(HttpMethod.Post, "/tests/modify", Ports.config) {
                addHeader("handle", currentTestID)
                setBody(
                    """
                    {
                        "chap_use": [
                            "body{test->@{SaveVar|'none'}}"
                        ]
                    }
                """.trimIndent()
                )
            }

            handleRequest(HttpMethod.Post, "/test", Ports.live) {
                uniqueIDHeader()
                setBody("")
            }.apply {
                response {
                    Assert.assertEquals(
                        "Activation code: [none]",
                        it.content
                    )
                }
            }
        }
    }

    @Test
    fun setVarAfterCall() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "tape_activate")
                addHeader("mock_Name", "chap_activate")
                addHeader("mockFilter_Path", "activate")
                addHeader("mockFilter_Body", ".*")
                setBody("code: 12345")
            }
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", "tape_use")
                addHeader("mock_Name", "chap_use")
                addHeader("mockFilter_Path", "test")
                addHeader("mockFilter_Body", ".*")
                setBody("Activation code: [test]")
            }

            handleRequest(HttpMethod.Post, "/tests/modify", Ports.config) {
                addHeader("handle", currentTestID)
                setBody(
                    """
                    {
                        "chap_activate": [
                            "var{code: (\\d+)->SaveVar}"
                        ],
                        "chap_use": [
                            "body{test->@{SaveVar|'none'}}"
                        ]
                    }
                """.trimIndent()
                )
            }

            // Do the tests!
            handleRequest(HttpMethod.Post, "/test", Ports.live) {
                uniqueIDHeader()
                setBody("")
            }.apply {
                response {
                    Assert.assertEquals(
                        "We made the 'test' call, no set variable, so the default is used",
                        "Activation code: [none]",
                        it.content
                    )
                }
            }

            handleRequest(HttpMethod.Post, "/activate", Ports.live) {
                uniqueIDHeader()
                setBody("")
            }.apply {
                response {
                    Assert.assertEquals(
                        "We expect the 'activate' call to return the data we need",
                        "code: 12345",
                        it.content
                    )
                }
            }

            handleRequest(HttpMethod.Post, "/test", Ports.live) {
                uniqueIDHeader()
                setBody("")
            }.apply {
                response {
                    Assert.assertEquals(
                        "Activate call was made, we expect the bound variable to be set",
                        "Activation code: [12345]",
                        it.content
                    )
                }
            }
        }
    }
}
