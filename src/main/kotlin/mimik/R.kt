import java.util.*

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
}
