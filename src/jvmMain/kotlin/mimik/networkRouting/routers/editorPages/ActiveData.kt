package mimik.networkRouting.routers.editorPages

import io.ktor.http.*
import kotlinx.collections.toArrayList
import mimik.helpers.RandomHost
import mimik.mockHelpers.RecordedInteractions
import mimik.mockHelpers.SeqActionObject
import mimik.tapeItems.BaseTape
import mimik.tapeItems.MimikContainer
import okhttp3.NetworkData
import okhttp3.RequestData
import okhttp3.ResponseData

class ActiveData(private val params: Parameters) {
    private val tapeCatalog get() = MimikContainer.tapeCatalog

    var tape: BaseTape? = null
    var chapter: RecordedInteractions? = null
    var networkData: NetworkData? = null

    /**
     * tape is null
     */
    val newTape
        get() = tape == null

    /**
     * chapter is null
     */
    val newChapter
        get() = chapter == null

    val newNetworkData
        get() = networkData == null

    val networkIsRequest
        get() = networkData is RequestData

    val networkIsResponse
        get() = networkData is ResponseData

    /**
     * Parameter data (trimmed) for 'tape', else null
     */
    val expectedTapeName
        get() = params["tape"]?.trim()
            ?.let { if (it.isBlank()) null else it }

    /**
     * Expected tape name, or a generated name (optional [default])
     */
    fun hardTapeName(default: String = RandomHost().value_abs.toString()) =
        expectedTapeName ?: default

    /**
     * Parameter data (trimmed) for 'chapter', else null
     */
    val expectedChapName
        get() = params["chapter"]?.trim()
            ?.let { if (it.isBlank()) null else it }

    /**
     * Expected chapter name, or a generated name (optional [default])
     */
    fun hardChapName(default: String = RandomHost().valueAsUUID) =
        expectedChapName ?: default

    /**
     * All the active sequences for this chapter
     */
    val seqActions: ArrayList<SeqActionObject>
        get() = chapter?.seqActions.orEmpty().toArrayList()

    val expectedNetworkType
        get() = params["network"]?.trim().orEmpty()

    /**
     * Params passed in a tape name, but no tape was found by that name
     */
    val loadTape_Failed
        get() = newTape && expectedTapeName != null

    /**
     * Params passed in a tape name, but no tape was found by that name
     */
    val loadChap_Failed
        get() = newChapter && expectedChapName != null

    fun hrefMake(
        tape: String? = null,
        chapter: String? = null,
        network: String? = null
    ): String {
        val builder = StringBuilder().append("edit?")
        if (tape != null)
            builder.append("tape=%s".format(tape))
        if (chapter != null)
            builder.append("&chapter=%s".format(chapter))
        if (network != null)
            builder.append("&network=%s".format(network))
        return builder.toString()
    }

    fun hrefEdit(
        hTape: String? = null,
        hChapter: String? = null,
        hNetwork: String? = null
    ): String {
        val builder = StringBuilder().append("edit?")
        if (hTape != null || !expectedTapeName.isNullOrEmpty())
            builder.append("tape=%s".format(hTape ?: hardTapeName()))
        else return builder.toString()

        if (hChapter != null || !expectedChapName.isNullOrEmpty())
            builder.append("&chapter=%s".format(hChapter ?: hardChapName()))
        else return builder.toString()

        if (hNetwork != null || expectedNetworkType.isNotBlank())
            builder.append("&network=%s".format(hNetwork ?: expectedNetworkType))
        return builder.toString()
    }

    init {
        tape = tapeCatalog.tapes
            .firstOrNull { it.name == params["tape"] }
        chapter = tape?.chapters
            ?.firstOrNull { it.name == params["chapter"] }
        networkData = when (params["network"]) {
            "request" -> chapter?.requestData
            "response" -> chapter?.responseData
            else -> null
        }
    }
}
