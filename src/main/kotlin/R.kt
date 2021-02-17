import java.util.Properties

object R : Properties() {
    init {
        javaClass.getResource("Strings.properties")?.also {
            try {
                load(it.openStream())
            } catch (e: Exception) {
                println(e.toString())
            }
        }
    }

    @Deprecated("Use array access instead", ReplaceWith("R[key]"))
    override fun getProperty(key: String) = ""

    operator fun get(s: String): String? = super.getProperty(s)

    operator fun get(s: String, default: String = ""): String =
        super.getProperty(s) ?: default
}
