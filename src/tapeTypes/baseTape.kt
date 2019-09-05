package com.fiserv.mimik.tapeTypes

import com.fiserv.mimik.tapeTypes.helpers.RecordedInteractions
import com.fiserv.mimik.tapeTypes.helpers.chapterName
import com.fiserv.mimik.tapeTypes.helpers.chapterNameHead
import com.fiserv.mimik.tapeTypes.helpers.filteredBody
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import okhttp3.Protocol
import okreplay.ComposedMatchRule
import okreplay.DefaultTapeRoot
import okreplay.MatchRule
import okreplay.MatchRules
import okreplay.OkReplayConfig.DEFAULT_TAPE_ROOT
import okreplay.Request
import okreplay.Response
import okreplay.Tape
import okreplay.TapeMode
import okreplay.TapeRoot
import java.io.File
import java.nio.charset.Charset

abstract class baseTape : Tape {
    /**
     * How the API calls will be separated within this tape
     */
    abstract val chapterTitles: Array<String>

    internal val tapeChapters: MutableList<RecordedInteractions> = mutableListOf()

    internal val requestMockResponses: MutableList<RecordedInteractions> = mutableListOf()

    /**
     * Tape name in the form of: {name}.json
     */
    val tapeName
        get() = "$name.json".replace(" ", "_")

    companion object {
        var tapeRoot: TapeRoot = DefaultTapeRoot(File(DEFAULT_TAPE_ROOT))
        val gson = Gson()
    }

    fun loadTapeData(data: Collection<RecordedInteractions>) {
        tapeChapters.clear()
        tapeChapters.addAll(0, data)
        tapeChapters.forEach { it.updateReplayData() }
    }

    override fun setMatchRule(matchRule: MatchRule?) {}

    override fun getMatchRule(): MatchRule =
        ComposedMatchRule.of(MatchRules.method, MatchRules.queryParams, filteredBody)

    override fun setMode(mode: TapeMode?) {}
    override fun getMode() = TapeMode.READ_WRITE
    override fun isReadable() = mode.isReadable
    override fun isWritable() = mode.isWritable
    override fun isSequential() = mode.isSequential

    override fun size() = tapeChapters.size

    override fun seek(request: Request): Boolean {
        val hasRequestMock =
            requestMockResponses.any {
                it.mockUses > 0 &&
                    it.chapterName.startsWith(request.chapterNameHead)
            }

        if (hasRequestMock)
            return true

        return tapeChapters.any {
            matchRule.isMatch(request, it.request)
        }
    }

    override fun play(request: Request): Response {
        // try returning the first requested response
        requestMockResponses.firstOrNull {
            it.mockUses > 0 &&
                it.chapterName.startsWith(request.chapterNameHead)
        }?.also {
            it.mockUses--
            return it.response
        }

        return tapeChapters.firstOrNull { matchRule.isMatch(request, it.request) }
            ?.response
            ?: defaultResponse
    }

    override fun record(request: Request, response: Response) {
        tapeChapters.add(
            RecordedInteractions(request, response)
        )

        tapeRoot.writerFor(tapeName).let {
            JsonWriter(it).apply {
                setIndent(" ")
                isHtmlSafe = false
            }
        }.also {
            gson.toJson(this, this::class.java, it)
        }.close()
    }

    override fun isDirty() = false

    @Transient
    private val defaultResponse = object : Response {
        override fun code() = 0
        override fun protocol() = Protocol.HTTP_2

        override fun getEncoding() = ""
        override fun getCharset() = Charset.defaultCharset()

        override fun headers() = okhttp3.Headers.of("", "")
        override fun header(name: String?) = ""
        override fun getContentType() = ""

        override fun hasBody() = false
        override fun body() = byteArrayOf()
        override fun bodyAsText() = ""

        override fun newBuilder() = TODO()
        override fun toYaml() = TODO()
    }
}
