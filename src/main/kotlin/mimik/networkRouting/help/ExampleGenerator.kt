package mimik.networkRouting.help

import kotlinUtils.beautifyJson
import kotlinUtils.toBase64
import mimik.helpers.parser.P4Command
import mimik.helpers.RandomHost
import mimik.helpers.isNotNull
import kotlin.math.absoluteValue

object ExampleGenerator {
    private val rHost = RandomHost()

    fun build(create: CreateTypes): String {
        val createName = create.name.toLowerCase()
        val sb_heads = StringBuilder()
        val rHost = RandomHost()

        /**
         * Set
         * - append to tape/ both "mockTapeUrl"
         *
         * Unset
         * - tape pass-through is false
         */
        val tapeUrl = rHost.actionOrDefault(null) {
            val urlSubPage = rHost.actionOrDefault("") {
                rHost.valueAsChars(Pool = RandomHost.pool_Letters) + "."
            }
            val urlHost = rHost.valueAsChars(Pool = RandomHost.pool_Letters)
            "%s://%s%s.com".format(
                rHost.actionOrDefault("http") { "https" },
                urlSubPage,
                urlHost
            )
        }

        /**
         * True
         * - mockTapeLive = true
         * - no chapters can be added
         * - no sequences can be added
         *
         * False
         * - can add chapters
         * - can add sequences
         */
        val isTapePassthrough = rHost.nextBool() && tapeUrl.isNotNull() &&
            create == CreateTypes.Tape

        sb_heads.appendLine("PUT /mock/$createName HTTP/1.1")
        sb_heads.appendLine("Host: 0.0.0.0:4321")
        sb_heads.appendLine("Content-Type: application/json")
        sb_heads.appendLine("mockTape_Name: ${rHost.valueAsChars()}")
        if (create == CreateTypes.Chapter)
            sb_heads.appendLine("mockChapter_Name: ${rHost.valueAsChars()}")

        if (create == CreateTypes.Tape && tapeUrl.isNotNull())
            sb_heads.appendLine("mockTapeUrl: $tapeUrl")

        // Create Attractors
        // 80% of the time, include the attractors in the example
        // 50% of the time, include each sub item
        if (rHost.nextInt(10, 0) > 2) {
            fun addFilterItem(
                title: String,
                hasMods: Boolean = true,
                body: () -> String
            ): String? {
                if (rHost.nextBool()) return null

                var isBase64 = false
                val mods = if (hasMods) {
                    isBase64 = rHost.nextBool()
                    val b64 = if (isBase64) "#" else ""
                    val opt = rHost.actionOrDefault("") { "~" }
                    val not = rHost.actionOrDefault("") { "!" }
                    "$opt$not$b64".toCharArray().run {
                        shuffle()
                        String(this)
                    }
                } else ""
                val bodyStr = body().run {
                    if (isBase64) toBase64 else this
                }

                return "mock${createName}Filter_%s%s: %s".format(
                    title, mods, bodyStr
                )
            }

            // Path
            repeat(rHost.nextInt(3, 1)) {
                addFilterItem("Path", false) {
                    (1..rHost.nextInt(4, 1)).joinToString("/") { rHost.valueAsChars() }
                }?.also { sb_heads.appendLine(it) }
            }

            // Query
            repeat(rHost.nextInt(3, 1)) {
                addFilterItem("Query") {
                    (1..rHost.nextInt(4, 1)).joinToString("&") {
                        rHost.valueAsChars(Pool = RandomHost.pool_Letters) + "=" +
                            rHost.valueAsChars(Pool = RandomHost.pool_Letters)
                    }
                }?.also { sb_heads.appendLine(it) }
            }

            // Header
            repeat(rHost.nextInt(3, 1)) {
                addFilterItem("Header") {
                    rHost.valueAsChars(Pool = RandomHost.pool_Letters) + ":" + rHost.valueAsChars()
                }?.also { sb_heads.appendLine(it) }
            }

            // Body
            repeat(rHost.nextInt(3, 1)) {
                addFilterItem("Body") {
                    rHost.valueAsChars()
                }?.also { sb_heads.appendLine(it) }
            }
        }

        if (rHost.nextBool())
            sb_heads.appendLine("mockSaveToFile: true")

        if (rHost.nextBool())
            sb_heads.appendLine("mockLive: ${rHost.nextBool()}")

        var bodyStr = ""
        if (create == CreateTypes.Chapter) {
            sb_heads.appendLine("\n")
            bodyStr = buildBody(rHost.nextBool()).beautifyJson
        }

        return sb_heads.toString() + bodyStr
    }

    fun buildBody(useAdvBody: Boolean): String {
        val baseResponse = """
             "userId": %s,
             "id": %s,
             "title": "%s",
             "completed": %s
        """.format(
            rHost.value,
            rHost.value,
            rHost.valueAsChars(),
            rHost.nextBool()
        )

        if (!useAdvBody) return "{$baseResponse}"

        return """{
            "Response": {$baseResponse},
            "Sequence": [${seqGenerator()}]
        }"""
    }

    /**
     * ID: Int
     * Name: String
     * Commands: Array of P4Command.toString
     */
    fun seqGenerator(): String {
        return (1..rHost.nextInt(4, 1)).joinToString {
            """{
                "ID": %d,
                "Name": "%s",
                "Commands": [%s]
            }""".format(
                rHost.nextInt().absoluteValue,
                rHost.valueAsChars(),
                (1..rHost.nextInt(4, 1)).joinToString {
                    "\"${P4Command().jumble()}\""
                }
            )
        }
    }
}
