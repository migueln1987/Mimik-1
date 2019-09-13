import helpers.fileListing
import helpers.toReplayRequest
import tapeItems.BlankTape
import tapeItems.helpers.toChain
import com.google.gson.Gson
import io.ktor.application.ApplicationCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Response
import okreplay.OkReplayInterceptor

class TapeCatalog private constructor() : OkReplayInterceptor() {
    private val config = VCRConfig.getConfig

    val tapes: MutableList<BlankTape> = mutableListOf()

    companion object {
        var Instance = TapeCatalog()

        private val defaultBlankTape = BlankTape
            .Builder() { tapeName = "Default Tape" }
            .build()
    }

    init {
        BlankTape.tapeRoot = config.tapeRoot
        loadTapeData()
    }

    /**
     * Loads all the *.json tapes within the okreplay.tapeRoot directory
     */
    @Suppress("USELESS_ELVIS")
    private fun loadTapeData() {
        val root = config.tapeRoot.get() ?: return
        val gson = Gson()

        root.fileListing().asSequence()
            .map { it to it.readText() }
            .mapNotNull {
                // try converting into a tape
                try {
                    gson.fromJson(it.second, BlankTape::class.java)
                        ?.also { tape ->
                            tape.file = it.first
                            tape.tapeName = tape.tapeName ?: tape.hashCode().toString()
                        }
                } catch (e: Exception) {
                    println(e.toString())
                    null
                }
            }
            .forEach { tapes.add(it) }
    }

    private fun getTape(request: okreplay.Request): BlankTape {
        return tapes.firstOrNull {
            it.containsRecording(request)
        } ?: defaultBlankTape
    }

    suspend fun processCall(call: ApplicationCall): Response {
        val callChain = call.toChain()
        val loadTape = getTape(callChain.toReplayRequest)
        start(config, loadTape)

        return withContext(Dispatchers.IO) {
            intercept(callChain)
        }
    }
}
