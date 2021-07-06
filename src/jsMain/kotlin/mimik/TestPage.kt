package mimik

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.kvision.Application
import io.kvision.core.*
import io.kvision.html.ListType
import io.kvision.html.listTag
import io.kvision.html.span
import io.kvision.i18n.tr
import io.kvision.module
import io.kvision.panel.SimplePanel
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import io.kvision.panel.root
import io.kvision.panel.vPanel
import io.kvision.startApplication
import io.kvision.utils.px

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
                add(BaseTab())
            }
        }
    }
}

class BaseTab : SimplePanel() {
    init {
        this.marginTop = 10.px
        this.minHeight = 400.px
        vPanel(spacing = 3) {
            span {
                +tr("A simple label")
            }
            span {
                fontFamily = "Times New Roman"
                fontSize = 32.px
                fontStyle = FontStyle.OBLIQUE
                fontWeight = FontWeight.BOLDER
                fontVariant = FontVariant.SMALLCAPS
                textDecoration =
                    TextDecoration(TextDecorationLine.UNDERLINE, TextDecorationStyle.DOTTED, Color.name(Col.RED))
                +tr("A label with custom CSS styling")
            }
            span {
                +tr("A list:")
            }
            listTag(ListType.UL, listOf(tr("First list element"), tr("Second list element"), tr("Third list element")))
        }
    }
}

fun main() {
    startApplication(::kkv, module.hot)
//    println("qq")
//    document.addEventListener("DOMContentLoaded", {
//        helloWorld("Hi!")
//    })
}
