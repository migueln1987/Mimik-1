package networkRouting.editorPages

import io.ktor.http.Parameters
import kotlinx.html.*
import networkRouting.editorPages.ChapterEditor.BreadcrumbNav

object TapeDataEditor : EditorModule() {
    fun HTML.dataEditor(params: Parameters) {
        val pData = params.toActiveEdit

        head {
            script { unsafe { +JS.all } }
        }

        body {
            setupStyle()
            BreadcrumbNav(pData)

        }
    }
}
