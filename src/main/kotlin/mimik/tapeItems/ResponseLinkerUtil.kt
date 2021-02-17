package mimik.tapeItems

import java.util.*

class ResponseLinkerUtil {
    val responseLinks = mutableMapOf<String, String>()

    val uniqueUUID: String
        get() {
            var newUAID: String
            do {
                newUAID = UUID.randomUUID().toString()
            } while (responseLinks.containsKey(newUAID))
            return newUAID
        }

    inline fun <reified R> newLink(LinkAction: (String) -> R): R {
        val currentKey = uniqueUUID
        responseLinks[currentKey] = ""
        return LinkAction(currentKey)
    }
}
