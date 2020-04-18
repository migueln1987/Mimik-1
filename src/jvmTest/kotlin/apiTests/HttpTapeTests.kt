package apiTests

import mimik.Ports
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import org.junit.Assert
import org.junit.Test

class HttpTapeTests {
    @Test
    fun deleteTape() {
        val tapeName = "DeleteTapeTest"
        TestApp {
            handleRequest(HttpMethod.Put, "/mock", Ports.config) {
                addHeader("mockTape_Name", tapeName)
                addHeader("mockTape_Only", "true")
            }.response {
                Assert.assertEquals(HttpStatusCode.Created, it.status())
            }

            handleRequest(HttpMethod.Get, "/tapes/delete?tape=$tapeName", Ports.config)
                .response {
                    val hasTape = TapeCatalog.Instance.tapes
                        .any { it.name == tapeName }

                    Assert.assertFalse(hasTape)
                }
        }
    }
}
