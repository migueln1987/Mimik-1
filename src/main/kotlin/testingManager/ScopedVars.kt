package testingManager

class ScopedVars {

    val container: String? = null

    /**
     * Variables which persist across the whole state of this test
     */
    val bound: MutableMap<String, String> = mutableMapOf()

    val tape: MutableMap<String, String> = mutableMapOf()

    val chapter: MutableMap<String, String> = mutableMapOf()
}
