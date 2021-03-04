package apiTests

import mimik.tapeItems.MimikContainer
import mimik.Ports
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import org.junit.Assert

class HttpTapeTests {
    // todo; pending update on new delete tape api
    // @Test
    fun deleteTape() {
        val tapeName = "DeleteTapeTest"
        TestApp {
            handleRequest(HttpMethod.Put, "/mock/tape", Ports.config) {
                addHeader("mockName", tapeName)
            }.response {
                Assert.assertEquals(HttpStatusCode.Created, it.status())
            }

            handleRequest(HttpMethod.Get, "/tapes/delete?tape=$tapeName", Ports.config)
                .response {
                    val hasTape = MimikContainer.tapeCatalog.tapes
                        .any { it.name == tapeName }

                    Assert.assertFalse(hasTape)
                }
        }
    }
}
