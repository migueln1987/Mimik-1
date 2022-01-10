package mimik

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.kvision.Application
import io.kvision.module
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import io.kvision.panel.root
import io.kvision.panel.vPanel
import io.kvision.startApplication
import mimik.tabs.BasicTab

private val client = HttpClient(Js)
private val scope = MainScope()
private val endpoint = window.location.origin

@Suppress("unused")
@JsName("helloWorld")
fun helloWorld(salutation: String) {
    val message = "$salutation from Kotlin.JS"
    println("aa")
    val element = document.getElementById("js-response") ?: return
    println("bb")
    element.textContent = message

    scope.launch {
//        delay(1000)
        element.textContent = "making request"
        repeat((0..3).count()) {
//            delay(1000)
            element.textContent += "."
        }

        val response = client.get<String> {
            url.takeFrom(endpoint)
            url.pathComponents("test")
        }
        element.textContent = """result is: "$response""""
    }
}

class kkv : Application() {
    override fun start() {
        root("kvision_test") {
            vPanel {
                add(BasicTab())
            }
        }
    }
}

fun main() {
    startApplication(::Showcase, module.hot)
//    println("qq")
//    document.addEventListener("DOMContentLoaded", {
//        helloWorld("Hi!")
//    })
}
