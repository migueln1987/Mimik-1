package com.fiserv.ktmimic.tapeTypes

import com.fiserv.ktmimic.tapeTypes.helpers.RecordedInteractions
import com.fiserv.ktmimic.tapeTypes.helpers.filteredBody
import com.google.gson.Gson
import io.ktor.html.each
import okhttp3.Headers
import okhttp3.Protocol
import okreplay.*
import okreplay.OkReplayConfig.DEFAULT_TAPE_ROOT
import java.io.File
import java.nio.charset.Charset

abstract class baseTape : Tape {
    abstract val opIds: Array<String>

    private val tapeChapters: MutableList<RecordedInteractions> = mutableListOf()

    private val tapeName
        get() = "$name.json"

    companion object {
        var tapeRoot: TapeRoot = DefaultTapeRoot(File(DEFAULT_TAPE_ROOT))
        val gson = Gson()
    }

    init {
        if (tapeRoot.tapeExists(tapeName)) {
            val reader = tapeRoot.readerFor(tapeName)
            gson.fromJson(reader, this::class.java)
                ?.also {
                    tapeChapters.clear()
                    tapeChapters.addAll(0, it.tapeChapters)
                }
        }
    }

    override fun setMode(mode: TapeMode?) {}

    override fun getMode(): TapeMode = TapeMode.READ_WRITE

    override fun setMatchRule(matchRule: MatchRule?) {}

    override fun getMatchRule(): MatchRule {
        return ComposedMatchRule.of(MatchRules.method, MatchRules.queryParams, filteredBody)
    }

    override fun isReadable(): Boolean = mode.isReadable
    override fun isWritable() = mode.isWritable
    override fun isSequential(): Boolean = mode.isSequential

    override fun size() = tapeChapters.size

    override fun seek(request: Request?) = tapeChapters.any {
        matchRule.isMatch(request, it.request)
    }

    override fun play(request: Request?): Response {
        return tapeChapters.firstOrNull { matchRule.isMatch(request, it.request) }
            ?.response
            ?: defaultResponse
    }

    override fun record(request: Request, response: Response) {
        tapeChapters.add(
            RecordedInteractions(request, response)
        )

        tapeRoot.writerFor(tapeName).also {
            it.write(gson.toJson(this))
        }.close()
    }

    override fun isDirty() = false


    @Transient
    private val defaultResponse = object : Response {
        override fun getEncoding() = "None"

        override fun body() = byteArrayOf()

        override fun newBuilder() = TODO()

        override fun getContentType() = "None"

        override fun hasBody() = false

        override fun toYaml(): YamlRecordedMessage = TODO()

        override fun protocol() = Protocol.HTTP_1_0

        override fun bodyAsText() = ""

        override fun getCharset() = Charset.defaultCharset()

        override fun header(name: String?) = ""

        override fun code(): Int = 0

        override fun headers(): Headers = TODO()
    }
}
