package com.fiserv.mimik.tapeItems

import com.fiserv.mimik.VCRConfig
import com.fiserv.mimik.mimikMockHelpers.RecordedInteractions
import com.fiserv.mimik.tapeTypes.helpers.filteredBody
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import okhttp3.Protocol
import okreplay.* // ktlint-disable no-wildcard-imports
import java.io.File
import java.io.Writer
import java.nio.charset.Charset

class BlankTape private constructor(
    var tapeName: String = hashCode().toString()
) : Tape {
    @Suppress("unused", "UNUSED_PARAMETER")
    class Builder {
        constructor()
        constructor(config: Builder.() -> Unit) {
            config.invoke(this)
        }

        var subDirectory: String? = ""
        var tapeName: String? = null

        fun build() = BlankTape(
            tapeName ?: hashCode().toString()
        ).also {
            it.file = File(
                VCRConfig.getConfig.tapeRoot.get(),
                (subDirectory ?: "") + "/" + it.jsonFileName
            )
        }
    }

    companion object {
        var tapeRoot: TapeRoot = VCRConfig.getConfig.tapeRoot
        internal val gson = Gson()

        @Transient
        private val defaultResponse = object : Response {
            override fun code() = 0
            override fun protocol() = Protocol.HTTP_1_1

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

    var file: File? = null

    val jsonFileName
        get() = tapeName
            .replace(" ", "_") + ".json"

    var attractors: RequestAttractors? = null
    var RoutingUrl: String? = null

    override fun getName() = tapeName

    /**
     * How the API calls will be separated within this tape
     */
//    val chapterTitles: Array<String>

    val tapeChapters: MutableList<RecordedInteractions> = mutableListOf()

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

    fun containsRecording(request: Request) = tapeChapters.any {
        it.attractors?.matchesRequest(request) ?: false
    }

    override fun seek(request: Request): Boolean {
        // todo; finish merging mockResponses variable with tapes
        tapeChapters
            .filter { it.mockUses > 0 }

//        val hasRequestMock =
//            requestMockResponses.any {
//                it.mockUses > 0 &&
//                        it.chapterName.startsWith(request.chapterNameHead)
//            }
//
//        if (hasRequestMock)
//            return true

        return tapeChapters.any {
            matchRule.isMatch(request, it.request)
        }
    }

    override fun play(request: Request): Response {
        // try returning the first requested response
//        requestMockResponses.firstOrNull {
//            it.mockUses > 0 &&
//                    it.chapterName.startsWith(request.chapterNameHead)
//        }?.also {
//            it.mockUses--
//            return it.response
//        }

        return tapeChapters.firstOrNull { matchRule.isMatch(request, it.request) }
            ?.response
            ?: defaultResponse
    }

    override fun record(request: Request, response: Response) {
        tapeChapters.add(
            RecordedInteractions(request, response)
        )

        val tree = gson.toJsonTree(this)

        // todo; use 'file'
        tapeRoot.writerFor(jsonFileName).jWriter
            .also {
                gson.toJson(this, this::class.java, it)
            }.close()
    }

    /**
     * Creates a JsonWriter from an input Writer
     */
    private val Writer.jWriter: JsonWriter
        get() = JsonWriter(this).apply {
            setIndent(" ")
            isHtmlSafe = false
        }

    override fun isDirty() = false
}
