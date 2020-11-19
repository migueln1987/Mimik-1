package unitTests.testManagerTests

import helpers.toHeaders_dupKeys
import okhttp3.Headers

/**
 * Class which holds all the input/ output variables of the testing bounds
 */
class P4MockData(config: (P4MockData) -> Unit = {}) {
    class DataScope {
        operator fun invoke(setup: DataScope.() -> Unit = {}) =
            setup.invoke(this)

        var useChapters: MutableMap<String, Int> = mutableMapOf()

        /**
         * How many uses this chapter will have
         */
        var use = -1

        /** Variables at a sequence level */
        var seqVars: MutableMap<String, String> = mutableMapOf()

        /** Variables at a chapter level */
        var chapVars: MutableMap<String, String> = mutableMapOf()

        /** Variables at a text bounds level */
        var testVars: MutableMap<String, String> = mutableMapOf()

        val containsVars: Boolean
            get() = seqVars.isNotEmpty() || chapVars.isNotEmpty() || testVars.isNotEmpty()

        class rTypeData(val isRequest: Boolean) {
            var headers_raw: MutableMap<String, List<String>> = mutableMapOf()
            var headers: Headers
                get() = headers_raw.toHeaders_dupKeys
                set(value) {
                    headers_raw = value.toMultimap() as MutableMap<String, List<String>>
                }
            val hasHeaders = headers_raw.isNotEmpty()
            var body: String? = null

            fun loadDefaultHeaders() {
                headers_raw = (if (isRequest)
                    mapOf(
                        "a2" to listOf("aaa2"),
                        "a1" to listOf("aaa1"),
                        "aa" to listOf("aaa2"),
                        "22" to listOf("aaaC")
                    )
                else
                    mapOf(
                        "b1" to listOf("bbb1"),
                        "b2" to listOf("bbb2"),
                        "qq" to listOf("dup_1", "dup_2"),
                        "33" to listOf("bbbC")
                    )
                        ).toMutableMap()
            }

            fun loadDefaultBody() {
                body = (if (isRequest)
                    """
                        code: 14
                        other: 12
                    """
                else
                    """
                        test: 22
                        final: 44
                    """
                        ).trimIndent()
            }
        }

        var request = rTypeData(true)
        var response = rTypeData(false)
    }

    fun loadDefaultVars(into: MutableMap<String, String>) {
        into.putAll(MockDataUtils.envBoundVars)
    }

    /**
     * Data which is used to set up the envinronment
     * and later result of the sequence commands
     */
    val activeData = DataScope()
    val expectedData = DataScope()

    init {
        config.invoke(this)
    }
}

object MockDataUtils {
    /**
     * Appended command to conditionals to ensure they were successful in matching
     */
    val p4CondChk = "%var[confirm]->{true}"

    val envBoundVars = mapOf(
        "wwA" to "zzA",
        "wwB" to "zzB",
        "qCC" to "xxC"
    )
}

/**
 * Creates an mock environment named [name] which
 */
fun List<String>.toMockSet(
    name: String = "",
    includeCondChk: Boolean = false,
    mockSetup: (P4MockData) -> Unit = {}
): MockSet = MockSet(name, this, includeCondChk, mockSetup)

class MockSet(
    val name: String,
    var commands: List<String>,
    includeCondChk: Boolean = false,
    mockDataConfig: (P4MockData) -> Unit
) {
    operator fun component1() = name
    operator fun component2() = commands
    operator fun component3() = mockData

    var mockData: P4MockData = P4MockData()
        private set

    init {
        if (includeCondChk) {
            commands = commands.toMutableList()
                .apply { add(MockDataUtils.p4CondChk) }
            mockData.expectedData.testVars["confirm"] = "true"
        }
        mockDataConfig.invoke(mockData)
    }
}
