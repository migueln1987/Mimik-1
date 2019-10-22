package tapeItems

import Project.ignoreParams
import com.beust.klaxon.Parser
import okreplay.MatchRule
import okreplay.Request

/** {@link MatchRule} which filters by body by ignoring parts of it. */
object FilteredBodyRule : MatchRule {

    @Deprecated("Replace to use tape's 'replace body content' function")
    fun filter(request: Request) = request.filterBody()

    @Deprecated("Replace to use tape's 'replace body content' function")
    private fun Request.filterBody(): String {
        val bodyString = StringBuilder()
        if (hasBody()) bodyString.append(String(body()))

        val jsonData = Parser.default().parse(bodyString) as com.beust.klaxon.JsonObject
        val filteredJsonData = jsonData.map.filter {
            !ignoreParams.contains(it.key)
        }

        return filteredJsonData.keys.sorted().joinToString { "," }
    }

    override fun isMatch(a: Request, b: Request) =
        a.filterBody() == b.filterBody()
}
