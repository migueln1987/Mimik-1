package networkRouting.editorPages

import helpers.isTrue
import helpers.toParameters
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.request.isMultipart
import io.ktor.request.receiveMultipart
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.util.filter
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
class TapeRouting(path: String) : RoutingContract(path) {

    enum class RoutePaths(val path: String) {
        ALL("all"),
        EDIT("edit"),
        DELETE("delete"),
        ACTION("action");
    }

    private val RoutePaths.asSubPath
        get() = this@TapeRouting.path + "/" + this.path

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
            if (call.request.isMultipart()) {
                val values = call.receiveMultipart()
                    .readAllParts().asSequence()
                    .filterIsInstance<PartData.FormItem>()
                    .filterNot { it.name.isNullOrBlank() }
                    .associate { it.name!! to it.value }

                call.processData(values)
            } else
                call.respondRedirect(RoutePaths.ALL.path)
        }

    private val Route.edit
        get() = route(RoutePaths.EDIT.path) {
            get {
                if (call.parameters.isEmpty()) {
                    call.respondRedirect(RoutePaths.ALL.path)
                    return@get
                }

                val limitSet: MutableSet<String> = mutableSetOf()
                val keys = listOf(
                    "tape", "chapter", "network"
                )
                val limitParams = call.parameters
                    .filter { s, _ ->
                        s.toLowerCase().let { pKey ->
                            val hasKey = keys.contains(pKey)
                            hasKey && limitSet.add(pKey)
                        }
                    }.toParameters

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

        val saveChap = foundTape.chapters
            .firstOrNull { it.name == data["name_pre"] }
            ?: let { foundTape.createNewInteraction() }

        saveChap.also {
            it.chapterName = data["nameChap"]
            // todo; save Chapter data
        }

        if (foundTape.file?.exists().isTrue())
            foundTape.saveFile()

        respondRedirect {
            path(TapeRouting.RoutePaths.EDIT.asSubPath)
            parameters.append("tape", foundTape.name)
            parameters.append("chapter", saveChap.name)
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
            "request" -> Requestdata {
                it.method = data["requestMethod"]
                it.url = data["requestUrl"]
            }

            "response" -> Responsedata {
                it.code = data["responseCode"]?.toIntOrNull()
            }

            else -> null
        }
            ?.also { nData ->
                val headerKVs = data.asSequence()
                    .filter { it.value.isNotBlank() }
                    .filter { it.key.startsWith("header_") }
                    .associateBy(
                        { it.key.removePrefix("header_") },
                        { it.value }
                    )

                val keys = headerKVs
                    .filter { it.key.startsWith("key") }
                    .mapKeys { it.key.removePrefix("key_") }

                val vals = headerKVs
                    .filter { it.key.startsWith("value") }
                    .mapKeys { it.key.removePrefix("value_") }

                val headerPairs = keys.mapNotNull {
                    val valData = vals[it.key]
                    if (valData != null)
                        it.value to valData
                    else null
                }

                nData.headers = okhttp3.Headers.Builder().also { builder ->
                    headerPairs.forEach {
                        builder.add(it.first, it.second)
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
