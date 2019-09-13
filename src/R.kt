import java.util.Properties

object R : Properties() {
    init {
        javaClass.getResource("Strings.properties")?.also {
            try {
                load(it.openStream())
            } catch (e: Exception) {
                System.out.println(e.toString())
            }
        }
    }
}
