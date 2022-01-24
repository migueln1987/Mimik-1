package mimik.tapeItems

import com.google.gson.GsonBuilder
import mimik.helpers.attractors.Attractor
import io.ktor.http.*
import javax.io.jsonFiles
import kotlinx.collections.firstMatchNotNull
import kotlinx.isTrue
import mimik.helpers.*
import mimik.mockHelpers.*
import okhttp3.content
import okhttp3.toStringPairs
import java.io.File

class TapeCatalog {
    val tapes: MutableList<BaseTape> = mutableListOf()

    /**
     * List of tapes to load (from JSON files)
     */
    val tapeFiles: List<File>?
        get() = MimikContainer.config.tapeRoot.get()?.jsonFiles()

    companion object {
        var isTestRunning = false
        val gson by lazy {
            GsonBuilder().apply {
                registerTypeAdapterFactory(SeqActionObject.typeFactory)
            }.create()
        }

//        val Instance by lazy { TapeCatalog().also { it.loadTapeData() } }
    }

    /**
     * Loads all the *.json tapes within the okreplay.tapeRoot directory
     */
    fun loadTapeData() {
        val tapePath = MimikContainer.config.tapeRoot.get()?.absolutePath
        println("Searching for tapes at: $tapePath")
        val files = tapeFiles ?: return

        tapes.clear()
        files.asSequence()
            .map { it to it.readText() }
            .mapNotNull {
                try {
                    gson.fromJson(it.second, BaseTape::class.java)
                        ?.also { tape ->
                            tape.file = it.first
                        }
                } catch (e: Exception) {
                    println(e.toString())
                    null
                }
            }
            .forEach { tapes.add(it) }

        // repair duplicate UIDs
        val chapIDs = tapes.flatMap { tape ->
            tape.chapters.map { Triple(tape, it, it.UID.orEmpty()) }
        }

        val sameIDs = chapIDs.groupingBy { it.third }.eachCount().filter { it.value > 1 }
        if (sameIDs.isNotEmpty()) {
            val resaveTapes = mutableListOf<BaseTape>()
            sameIDs.forEach { (sID, _) ->
                chapIDs.filter { it.third == sID }
                    .forEach {
                        resaveTapes.add(it.first)
                        it.second.UID = null
                        it.second.UID
                    }
            }

            resaveTapes.forEach { it.saveFile() }
        }
    }

    /**
     * Finds the tape which contains this request (by query match)
     *
     * @return
     * - HttpStatusCode.Found (302) = item
     * - HttpStatusCode.NotFound (404) = item
     * - HttpStatusCode.Conflict (409) = null item
     */
    fun findResponseByQuery(
        request: okhttp3.Request,
        tapeLimit: List<String>? = null
    ): Pair<QueryResponse<BaseTape>, RecordedInteractions?> {
        if (tapes.isEmpty()) return Pair(QueryResponse(), null)

        val allowedTapes = tapes.asSequence()
            .filter { tapeLimit?.contains(it.name) ?: true }.toList()

        fun allowedChaps() = allowedTapes.asSequence()
            .flatMap { it.chapters.asSequence() }
            .filter { MockUseStates.isEnabled(it.mockUses) }

        val hashContent = request.contentHash

        val cachedChapter = allowedChaps()
            .firstOrNull { it.cachedCalls.contains(hashContent) }

        val bestChapter = when {
            cachedChapter != null -> cachedChapter

            else -> {
                val readyChapters = allowedChaps()
                    .filter { it.attractors != null }
                    .associateWith { it.attractors!! }

                val path = request.url.encodedPath.removePrefix("/")
                val queries = request.url.query
                val headers = request.headers.toStringPairs()
                val body = request.body?.content()

                val validChapters = Attractor.findBest_many(
                    readyChapters,
                    path, queries, headers, body
                )

                fun useMatchesTest(chap: RecordedInteractions, test: (Int) -> Boolean) =
                    allowedTapes.first { it.chapters.contains(chap) }
                        .run { test(chap.uses) }

                val items = validChapters.item
                when {
                    items == null -> null
                    items.size == 1 -> items.first()
                    else -> items.firstMatchNotNull(
                        { it.alwaysLive.isTrue },
                        { it.awaitResponse },
                        { useMatchesTest(it) { uses -> uses == MockUseStates.ALWAYS.state } },
                        { useMatchesTest(it) { uses -> uses in (1..Int.MAX_VALUE) } }
                    )
                }
            }
        }

        val foundTape = allowedTapes
            .firstOrNull { it.chapters.contains(bestChapter) }

        if (cachedChapter == null && bestChapter != null)
            bestChapter.cachedCalls.add(hashContent)

        return Pair(
            QueryResponse {
                item = foundTape
                status = foundTape?.let { HttpStatusCode.Found } ?: HttpStatusCode.NotFound
            },
            bestChapter
        )
    }

    /**
     * Returns the most likely tape which can accept the [request]
     *
     * @return
     * - HttpStatusCode.Found (302) = item
     * - HttpStatusCode.NotFound (404) = item
     * - HttpStatusCode.Conflict (409) = null item
     */
    fun findTapeByQuery(request: okhttp3.Request): QueryResponse<BaseTape> {
        val path = request.url.encodedPath.removePrefix("/")
        val queries = request.url.query
        val headers = request.headers.toStringPairs()

        val validTapes = tapes.asSequence()
            .filter { it.mode.isWritable }
            .filter { it.attractors != null }
            .associateBy({ it }, { it.attractors!! })

        return Attractor.findBest(
            validTapes,
            path, queries, headers
        )
    }
}
