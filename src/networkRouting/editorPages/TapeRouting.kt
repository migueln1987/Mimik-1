package networkRouting.editorPages

import helpers.*
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.*
import mimikMockHelpers.Requestdata
import mimikMockHelpers.Responsedata
import networkRouting.RoutingContract
import networkRouting.editorPages.ChapterEditor.getChapterPage
import networkRouting.editorPages.DeleteModule.deleteActions
import networkRouting.editorPages.NetworkDataEditor.dataEditor
import networkRouting.editorPages.TapeEditor.getAllTapesPage
import networkRouting.editorPages.TapeEditor.getTapePage
import tapeItems.BlankTape

@Suppress("RemoveRedundantQualifierName")
class TapeRouting(path: String = RoutePaths.rootPath) : RoutingContract(path) {

    enum class RoutePaths(val path: String) {
        ALL("all"),
        EDIT("edit"),
        DELETE("delete"),
        ACTION("action");

        companion object {
            const val rootPath = "tapes"
        }

        val asSubPath
            get() = "$rootPath/$path"
    }

    override fun init(route: Routing) {
        route.route(path) {
            all
            action
            edit
            delete
            get { call.respondRedirect(RoutePaths.ALL.asSubPath) }
        }
    }

    private val Route.all
        get() = route(RoutePaths.ALL.path) {
            get { call.respondHtml { getAllTapesPage() } }
            post { call.respondRedirect(RoutePaths.ALL.path) }
        }

    private val Route.action
        get() = post(RoutePaths.ACTION.path) {
            val params = call.anyParameters()
            if (params == null || params.isEmpty())
                call.respondRedirect(RoutePaths.ALL.path)
            else
                call.processData(params.toSingleMap)
        }

    private val Route.edit
        get() = route(RoutePaths.EDIT.path) {
            get {
                if (call.parameters.isEmpty()) {
                    call.respondRedirect(RoutePaths.ALL.path)
                    return@get
                }

                val limitParams = call.parameters
                    .limit(listOf("tape", "chapter", "network"))

                if (call.parameters != limitParams) {
                    call.respondRedirect {
                        parameters.clear()
                        parameters.appendAll(limitParams)
                    }
                    return@get
                }

                EditorModule.randomHost.nextRandom()
                call.respondHtml {
                    if (limitParams.contains("tape")) {
                        if (limitParams.contains("chapter")) {
                            if (limitParams.contains("network"))
                                dataEditor(limitParams)
                            else
                                getChapterPage(limitParams)
                        } else
                            getTapePage(limitParams)
                    }
                }
            }

            post {
                call.respondRedirect(RoutePaths.EDIT.asSubPath)
            }
        }

    private val Route.delete
        get() = route(RoutePaths.DELETE.path) {
            get { deleteActions() }

            post {
                call.respondText("delete page")
            }
        }

    /**
     * Processes the POST "/Action" call
     */
    private suspend fun ApplicationCall.processData(data: Map<String, String>) {
        when (data["Action"]) {
            "SaveTape" -> Action_SaveTape(data)
            "SaveChapter" -> Action_SaveChapter(data)
            "SaveNetworkData" -> Action_SaveNetworkData(data)

            "SaveToHardTape" -> {
                val foundTape = tapeCatalog.tapes
                    .firstOrNull { it.name == data["tape"] }
                    ?.also { it.saveFile() }

                respondRedirect {
                    if (foundTape != null && data["resumeEdit"] == "true") {
                        path(TapeRouting.RoutePaths.EDIT.asSubPath)
                        parameters.append("tape", data["tape"].orEmpty())
                    } else path(TapeRouting.RoutePaths.ALL.asSubPath)
                }
            }

            "Edit" -> {
                respondRedirect {
                    path(path, TapeRouting.RoutePaths.EDIT.path)
                    parameters.apply {
                        data.filterNot { it.key == "Action" }
                            .forEach { (t, u) -> append(t, u) }
                    }
                }
            }

            "Remove" -> {
                tapeCatalog.tapes.firstOrNull { it.name == data["tape"] }
                    ?.also { tape ->
                        data["chapter"]?.also { chap ->
                            tape.chapters.removeIf { it.name == chap }
                            respondRedirect(TapeRouting.RoutePaths.EDIT.path)
                            return
                        } ?: tapeCatalog.tapes.remove(tape)
                    }

                respondRedirect(TapeRouting.RoutePaths.ALL.path)
            }

            "Delete" -> {
                respondRedirect {
                    path(path, TapeRouting.RoutePaths.DELETE.path)
                    val filterKeys = listOf("tape", "chapter")
                    parameters.apply {
                        data.asSequence()
                            .filter { filterKeys.contains(it.key) }
                            .forEach { (t, u) -> append(t, u) }
                    }
                }
            }

            else -> respondRedirect(TapeRouting.RoutePaths.ALL.path)
        }
    }

    private suspend fun ApplicationCall.Action_SaveTape(data: Map<String, String>) {
        val tape = data.saveToTape()
        respondRedirect {
            path(TapeRouting.RoutePaths.EDIT.asSubPath)

            parameters.append("tape", tape.name)
            when (data["afterAction"]) {
                "newChapter" -> parameters.append("chapter", "")
                "addNew" -> parameters["tape"] = ""
                "allTapes" -> {
                    parameters.remove("tape")
                    path(TapeRouting.RoutePaths.ALL.asSubPath)
                }
            }
        }
    }

    private suspend fun ApplicationCall.Action_SaveChapter(data: Map<String, String>) {
        val tapeName = data["tape"]
        val foundTape = tapeCatalog.tapes
            .firstOrNull { it.name == tapeName }
            ?: let {
                BlankTape.reBuild { it.tapeName = tapeName }
                    .also { tapeCatalog.tapes.add(it) }
            }

        val chap = data.saveChapter(foundTape)

        respondRedirect {
            path(TapeRouting.RoutePaths.EDIT.asSubPath)
            parameters.append("tape", foundTape.name)
            parameters.append("chapter", chap.name)
            when (data["afterAction"]) {
                "newChapter" -> parameters["chapter"] = ""
                "parentTape" -> parameters.remove("chapter")
            }
        }
    }

    private suspend fun ApplicationCall.Action_SaveNetworkData(data: Map<String, String>) {
        val tapeName = data["tape"]
        val foundTape = tapeCatalog.tapes
            .firstOrNull { it.name == tapeName }
            ?: let {
                BlankTape.reBuild { it.tapeName = tapeName }
                    .also { tapeCatalog.tapes.add(it) }
            }

        val foundChap = foundTape.chapters
            .firstOrNull { it.name == data["chapter"] }
            ?: let {
                foundTape.createNewInteraction() { it.chapterName = data["chapter"] }
            }

        val network = when (data["network"]) {
            "request" -> (foundChap.requestData ?: Requestdata()).also {
                it.method = data["requestMethod"]
                it.url = (it.httpUrl ?: it.url.asHttpUrl)
                    .reHost(data["requestUrl"]).toString()
            }

            "response" -> (foundChap.responseData ?: Responsedata()).also {
                it.code = data["responseCode"]?.toIntOrNull()
            }

            else -> null
        }
            ?.also { nData ->
                val params = data["reqParams"].orEmpty()
                    .toPairs() { !it[0].startsWith("//") }

                if (nData is Requestdata) {
                    nData.url = nData.httpUrl.reParam(params).toString()

                    if (nData.url.isNullOrBlank())
                        nData.url = null
                }

                val headersData = data["netHeaders"].orEmpty()
                    .toPairs() { !it[0].startsWith("//") }
                nData.headers = okhttp3.Headers.Builder().also { builder ->
                    headersData?.filter { it.second != null }
                        ?.forEach {
                            builder.add(it.first, it.second!!)
                        }
                }.build()

                nData.body = data["networkBody"]
            }

        when (data["network"]) {
            "request" ->
                foundChap.requestData = network as? Requestdata
            "response" ->
                foundChap.responseData = network as? Responsedata
        }

        respondRedirect {
            path(TapeRouting.RoutePaths.EDIT.asSubPath)

            parameters.append("tape", foundTape.name)
            parameters.append("chapter", foundChap.name)
            parameters.append("network", data["network"].orEmpty())
            when (data["afterAction"]) {
                "viewChapter" -> parameters.remove("network")
            }
        }
    }
}
