package apiTests

import mimik.Ports
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import kolor.blue
import kolor.yellow
import org.junit.After
import org.junit.Assert
import org.junit.Before

class TestBoundTests : ApiTests {

    var currentTestID = ""

    private fun TestApplicationRequest.uniqueIDHeader() {
        addHeader("uniqueid", "123")
    }

    @Before
    fun boundStart() {
        TestApp {
            handleRequest(HttpMethod.Post, "/tests/start", Ports.gui) {
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
            handleRequest(HttpMethod.Post, "/tests/stop", Ports.gui) {
                addHeader("handle", currentTestID)
            }.apply {
                response {
                    val boundTime = it.headers[currentTestID + "_time"].orEmpty()
                    println("Test bound run time: $boundTime")

                    Assert.assertEquals("1", it.headers["stopped"])
                }
            }

            handleRequest(HttpMethod.Post, "/tests/stop", Ports.gui) {
                addHeader("handle", "$currentTestID, ##Finalize")
            }.apply {
                response {
                    Assert.assertTrue(it.headers.contains("finalized"))
                }
            }
        }
    }

    // todo; pending new API changes
    // @Test
    fun emptyVarReturnsEmptyString() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockTape_Name", "tape_use")
                addHeader("mock_Name", "chap_use")
                addHeader("mockFilter_Path", "test")
                addHeader("mockFilter_Body", ".*")
                setBody("Activation code: [test]")
            }

            handleRequest(HttpMethod.Post, "/tests/modify", Ports.gui) {
                addHeader("handle", currentTestID)
                setBody(
                    """
                    {
                      "chap_use": {
                        "seqSteps": [{
                          "Commands": ["response:body:{test}->{@{useCode}}"]
                        }]
                      }
                    }
                """.trimIndent()
                )
            }

            handleRequest(HttpMethod.Post, "/test", Ports.mock) {
                uniqueIDHeader()
                setBody("")
            }.apply {
                response {
                    println("'Test' call:: Expected response = ".blue() + "Activation code: []".yellow())
                    println("'Test' call:: Received response = ".blue() + it.content.orEmpty().yellow())
                    Assert.assertEquals(
                        "Activation code: []",
                        it.content
                    )
                }
            }
        }
    }

    // todo; pending new API changes
    // @Test
    fun emptyVarReturnsFirstDefault() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockTape_Name", "tape_use")
                addHeader("mock_Name", "chap_use")
                addHeader("mockFilter_Path", "test")
                addHeader("mockFilter_Body", ".*")
                setBody("Activation code: [test]")
            }

            handleRequest(HttpMethod.Post, "/tests/modify", Ports.gui) {
                addHeader("handle", currentTestID)
                setBody(
                    """
                    {
                      "chap_use": {
                        "seqSteps": [{
                          "Commands": ["response:body:{test}->{@{useCode|'none'}}"]
                        }]
                      }
                    }
                """.trimIndent()
                )
            }

            handleRequest(HttpMethod.Post, "/test", Ports.mock) {
                uniqueIDHeader()
                setBody("")
            }.apply {
                response {
                    println("'Test' call:: Expected response = ".blue() + "Activation code: [none]".yellow())
                    println("'Test' call:: Received response = ".blue() + it.content.orEmpty().yellow())
                    Assert.assertEquals(
                        "Activation code: [none]",
                        it.content
                    )
                }
            }
        }
    }

    // todo; pending new API changes
    // @Test
    fun setVarAfterCall() {
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockTape_Name", "tape_activate")
                addHeader("mock_Name", "chap_activate")
                addHeader("mockFilter_Path", "activate")
                addHeader("mockFilter_Body", ".*")
                setBody("code: 12345")
            }
            handleRequest(HttpMethod.Put, "/mock", Ports.gui) {
                addHeader("mockTape_Name", "tape_use")
                addHeader("mock_Name", "chap_use")
                addHeader("mockFilter_Path", "test")
                addHeader("mockFilter_Body", ".*")
                setBody("Activation code: [test]")
            }

            handleRequest(HttpMethod.Post, "/tests/modify", Ports.gui) {
                addHeader("handle", currentTestID)
                setBody(
                    """
                    {
                      "chap_activate": {
                        "seqSteps": [{
                          "Commands": ["response:body:{code: (\\d+)}->%SaveVar"]
                        }]
                      },
                      "chap_use": {
                        "seqSteps": [{
                          "Commands": ["response:body:{test}->{@{%SaveVar|'none'}}"]
                        }]
                      }
                    }
                """.trimIndent()
                )
            }

            // Do the tests!
            handleRequest(HttpMethod.Post, "/test", Ports.mock) {
                uniqueIDHeader()
                setBody("")
            }.apply {
                response {
                    println("'Test' call:: Expected response = ".blue() + "Activation code: [none]".yellow())
                    println("'Test' call:: Received response = ".blue() + it.content.orEmpty().yellow())
                    Assert.assertEquals(
                        "We made the 'test' call, no set variable, so the default is used",
                        "Activation code: [none]",
                        it.content
                    )
                }
            }

            handleRequest(HttpMethod.Post, "/activate", Ports.mock) {
                uniqueIDHeader()
                setBody("")
            }.apply {
                response {
                    println("'Activate' call:: Expected response = ".blue() + "code: 12345".yellow())
                    println("'Activate' call:: Received response = ".blue() + it.content.orEmpty().yellow())
                    Assert.assertEquals(
                        "We expect the 'activate' call to return the data we need",
                        "code: 12345",
                        it.content
                    )
                }
            }

            handleRequest(HttpMethod.Post, "/test", Ports.mock) {
                uniqueIDHeader()
                setBody("")
            }.apply {
                response {
                    println("'Test' call:: Expected response = ".blue() + "Activation code: [12345]".yellow())
                    println("'Test' call:: Received response = ".blue() + it.content.orEmpty().yellow())
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
