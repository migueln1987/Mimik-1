package mimik

import io.kvision.*

// private val client = HttpClient(Js)
// private val scope = MainScope()
// private val endpoint = window.location.origin

// @Suppress("unused")
// @JsName("helloWorld")
// fun helloWorld(salutation: String) {
//    val message = "$salutation from Kotlin.JS"
//    println("aa")
//    val element = document.getElementById("js-response") ?: return
//    println("bb")
//    element.textContent = message
//
//    scope.launch {
//        // delay(1000)
//        element.textContent = "making request"
//        repeat((0..3).count()) {
//            // delay(1000)
//            element.textContent += "."
//        }
//
//        val response = client.get<String> {
//            url.takeFrom(endpoint)
//            url.pathComponents("test")
//        }
//        element.textContent = """result is: "$response""""
//    }
// }

// class kkv : Application() {
//    override fun start() {
//        root("kvision_test") {
//            vPanel {
//                add(BasicTab())
//            }
//        }
//    }
// }

fun main() {
    startApplication(
        ::Showcase,
        module.hot,
        BootstrapModule,
        BootstrapCssModule,
        FontAwesomeModule,
        BootstrapSelectModule,
        BootstrapDatetimeModule,
        BootstrapSpinnerModule,
        BootstrapTypeaheadModule,
        BootstrapUploadModule,
        RichTextModule,
        ChartModule,
        TabulatorModule,
        CoreModule
    )

    startApplication(
        ::MimikGUI,
        module.hot
    )

//    document.addEventListener("DOMContentLoaded", {
//        helloWorld("Hi!")
//    })
}
