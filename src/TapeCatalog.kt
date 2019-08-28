package com.fiserv.ktmimic

import com.fiserv.ktmimic.tapeTypes.CFCTape
import com.fiserv.ktmimic.tapeTypes.GeneralTape
import com.fiserv.ktmimic.tapeTypes.NewTapes
import com.fiserv.ktmimic.tapeTypes.baseTape
import com.fiserv.ktmimic.tapeTypes.helpers.toChain
import com.google.gson.Gson
import io.ktor.application.ApplicationCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okreplay.OkReplayInterceptor
import okhttp3.Response
import java.util.logging.Logger

class TapeCatalog private constructor() : OkReplayInterceptor() {
    private val log by lazy { Logger.getLogger("TapeCatalog") }

    private val config = VCRConfig.getConfig

    private val defaultTape = NewTapes()

    val tapes by lazy {
        arrayOf(
            defaultTape,
            GeneralTape(),
            CFCTape()
        )
    }

    val tapeCalls: HashMap<String, baseTape> = hashMapOf()

    private var lastLoadedTape: String? = null

    companion object {
        var Instance = TapeCatalog()
    }

    init {
        baseTape.tapeRoot = config.tapeRoot
        loadTapeData()
        catalogTapeCalls()
    }

    /**
     * Parses all the tape into [opId, Tape] for easy tape recall
     */
    private fun catalogTapeCalls() {
        tapes.forEach { tape ->
            tape.chapterTitles.forEach { key ->
                if (tapeCalls.containsKey(key)) {
                    log.warning("Catalog already contains a tape chapter title of $key")
                } else {
                    tapeCalls[key] = tape
                }
            }
        }
    }

    private fun loadTapeData() {
        val gson = Gson()

        tapes.forEach {
            if (config.tapeRoot.tapeExists(it.tapeName)) {
                val reader = baseTape.tapeRoot.readerFor(it.tapeName)
                gson.fromJson(reader, it::class.java)
                    ?.also { loadFile ->
                        it.loadTapeData(loadFile.tapeChapters)
                    }
            }
        }
    }

    private fun getTape(opId: String) = tapeCalls.getOrDefault(opId, defaultTape)

    suspend fun processCall(call: ApplicationCall, tapeKey: () -> String): Response {
        val key = tapeKey.invoke()
        if (lastLoadedTape != key) {
            lastLoadedTape = key
            start(config, getTape(key))
        }

        return withContext(Dispatchers.IO) {
            intercept(call.toChain())
        }
    }
}
