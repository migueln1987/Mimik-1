package mimik.networkRouting.GUIPages

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.ensureHttpPrefix
import kotlinx.uppercaseFirstLetter
import mimik.helpers.*
import mimik.mockHelpers.RecordedInteractions
import mimik.mockHelpers.toAttractors
import mimik.networkRouting.RoutingContract
import mimik.networkRouting.GUIPages.ChapterEditor.getChapterPage
import mimik.networkRouting.GUIPages.DeleteModule.deleteActions
import mimik.networkRouting.GUIPages.NetworkDataEditor.dataEditor
import mimik.networkRouting.GUIPages.TapeEditor.getAllTapesPage
import mimik.networkRouting.GUIPages.TapeEditor.getTapePage
import mimik.tapeItems.BaseTape
import okhttp3.RequestData
import okhttp3.ResponseData
import okhttp3.internal.http.HttpMethod
import okhttp3.reQuery

/**
 * GUI page for editing tapes and processor of edit commands (action and delete)
 *
 * @see <a href="0.0.0.0:4321/">all</a>
 * @see <a href="0.0.0.0:4321/edit">edit</a>
 */
class TapeRouting : RoutingContract(RoutePaths.rootPath) {

    enum class RoutePaths(val path: String) {
        ALL("all"),
        EDIT("edit"),
        DELETE("delete"),
        ACTION("action");

        companion object {
            const val rootPath = "tapes"
        }

        /**
         * [rootPath]/[path]
         */
        val asSubPath
            get() = "$rootPath/$path"
    }

    override fun init(route: Route) {
        route.route(path) {
            all
            action
            edit
            delete
            get { call.redirect(RoutePaths.ALL.asSubPath) }
        }
    }

    private val Route.all
        get() = route(RoutePaths.ALL.path) {
            suspend fun ApplicationCall.action() = respondHtml { getAllTapesPage() }
            get { call.action() }
            post { call.action() }
        }

    private val Route.action
        get() = post(RoutePaths.ACTION.path) {
            val params = call.anyParameters()
            if (params.isEmpty())
                call.redirect(RoutePaths.ALL.path)
            else
                call.processData(params.toSingleMap)
        }

    private val Route.edit
        get() = route(RoutePaths.EDIT.path) {
            get {
                if (call.parameters.ignoreHostItems().isEmpty()) {
                    call.redirect(RoutePaths.ALL.path)
                    return@get
                }
                val limitParams = call.parameters
                    .limitKeys("tape", "chapter", "network")

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
                call.redirect(RoutePaths.EDIT.path)
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

                if (foundTape != null && data["resumeEdit"] == "true") {
                    redirect(RoutePaths.EDIT.path) {
                        parameters["tape"] = data["tape"].orEmpty()
                    }
                } else redirect(RoutePaths.ALL.path)
            }

            "Clone" -> Action_Clone(data)

            "Edit" -> {
                redirect(RoutePaths.EDIT.path) {
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
                            redirect(RoutePaths.EDIT.path)
                            return
                        } ?: tapeCatalog.tapes.remove(tape)
                    }

                redirect(RoutePaths.ALL.path)
            }

            "Delete" -> {
                redirect(RoutePaths.DELETE.path) {
                    val filterKeys = listOf("tape", "chapter")
                    parameters.apply {
                        data.asSequence()
                            .filter { filterKeys.contains(it.key) }
                            .forEach { (t, u) -> append(t, u) }
                    }
                }
            }

            else -> redirect(RoutePaths.ALL.path)
        }
    }

    private suspend fun ApplicationCall.Action_SaveTape(data: Map<String, String>) {
        val tape = data.saveToTape()
        redirect(RoutePaths.EDIT.path) {
            parameters["tape"] = tape.name
            when (data["afterAction"]) {
                "newChapter" -> parameters["chapter"] = ""
                "addNew" -> parameters["tape"] = ""
                "allTapes" -> {
                    parameters.remove("tape")
                    encodedPath += encodedPath.substringBeforeLast("/") + RoutePaths.ALL.path
                }
            }
        }
    }

    private suspend fun ApplicationCall.Action_SaveChapter(data: Map<String, String>) {
        val tapeName = data["tape"]
        val foundTape = tapeCatalog.tapes
            .firstOrNull { it.name == tapeName }
            ?: let {
                BaseTape.reBuild { it.tapeName = tapeName }
                    .also { tapeCatalog.tapes.add(it) }
            }

        val chap = data.saveChapter(foundTape)

        redirect(RoutePaths.EDIT.path) {
            parameters["tape"] = foundTape.name
            parameters["chapter"] = chap.name
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
                BaseTape.reBuild { it.tapeName = tapeName }
                    .also { tapeCatalog.tapes.add(it) }
            }

        val foundChap = foundTape.chapters
            .firstOrNull { it.name == data["chapter"] }
            ?: let {
                foundTape.createNewInteraction { it.chapterName = data["chapter"] }
            }

        val network = when (data["network"]) {
            "request" -> (foundChap.requestData ?: RequestData()).also {
                it.method = data["requestMethod"]?.toUpperCase()
                it.url = data["requestUrl"]?.ensureHttpPrefix
            }

            "response" -> (foundChap.responseData ?: ResponseData()).also {
                it.code = data["responseCode"]?.toIntOrNull()
            }

            else -> null
        }
            ?.also { nData ->
                if (nData is RequestData) {
                    val queries = data["reqQuery"].toPairs()
                    nData.url = nData.httpUrl.reQuery(queries).toString()

                    if (nData.url.isNullOrBlank())
                        nData.url = null
                }

                val headersData = data["netHeaders"].toPairs().orEmpty()
                nData.headers = okhttp3.Headers.Builder().also { builder ->
                    headersData.forEach { (key, value) ->
                        val headName = key.toLowerCase()
                            .replace("""-([a-z])""".toRegex()) { it.value.toUpperCase() }
                            .uppercaseFirstLetter()
                        builder.add(headName, value)
                    }
                }.build()

                nData.body = data["networkBody"].let {
                    val mm = data["requestMethod"]?.toUpperCase().orEmpty()
                    when {
                        !HttpMethod.permitsRequestBody(mm) && it != null -> null
                        HttpMethod.requiresRequestBody(mm) && it == null -> ""
                        else -> it?.replace("[\r\n]+ {2,}".toRegex(), "")
                    }
                }
            }

        when (data["network"]) {
            "request" -> {
                foundChap.requestData = network as? RequestData
                if (data["parseAttractors"] == "on") {
                    foundChap.attractors = foundChap.requestData?.toAttractors
                    foundChap.cachedCalls.clear()
                }
            }
            "response" ->
                foundChap.responseData = network as? ResponseData
        }
        foundTape.saveIfExists()

        redirect(RoutePaths.EDIT.path) {
            parameters["tape"] = foundTape.name
            parameters["chapter"] = foundChap.name
            parameters["network"] = data["network"].orEmpty()
            when (data["afterAction"]) {
                "viewChapter" -> parameters.remove("network")
            }
        }
    }

    private suspend fun ApplicationCall.Action_Clone(data: Map<String, String>) {
        val tapeName = data["tape"]
        val tape = tapeCatalog.tapes.firstOrNull { it.name == tapeName }
        if (tape == null) {
            redirect(RoutePaths.ALL.path)
            return
        }

        val chapName = data["chapter"]
        val chap = tape.chapters.firstOrNull { it.name == chapName }

        var newTape: BaseTape? = null
        var newChap: RecordedInteractions? = null

        if (chap == null) {
            newTape = tape.clone { post ->
                var cName = post.name
                var loopCheck = 1
                while (tapeCatalog.tapes.any { it.name == cName }) {
                    cName = post.name + loopCheck
                    loopCheck++
                }
                post.tapeName = cName
            }
            newTape.saveIfExists()
            tapeCatalog.tapes.add(newTape)
        } else {
            newChap = chap.clone { post ->
                var cName = post.name
                var loopCheck = 1
                while (tape.chapters.any { it.name == cName }) {
                    cName = post.name + loopCheck
                    loopCheck++
                }
                post.chapterName = cName
            }
            tape.chapters.add(newChap)
            tape.saveIfExists()
        }

        redirect(RoutePaths.EDIT.path) {
            parameters["tape"] = tape.name

            when (data["afterAction"]) {
                "edit" -> {
                    if (newChap == null)
                        parameters["tape"] = newTape?.name.orEmpty()
                    else
                        parameters["chapter"] = newChap.name
                }
            }
        }
    }
}
