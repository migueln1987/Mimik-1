package helpers

import io.ktor.util.StringValues
import okhttp3.Headers
import okhttp3.HttpUrl

fun StringValues.toHeaders(): Headers {
    return Headers.Builder().also { build ->
        entries().forEach { entry ->
            entry.value.forEach { value ->
                build.add(entry.key, value)
            }
        }
    }.build()
}

fun Map<String, String>.toHeaders(): Headers {
    return Headers.Builder().also { build ->
        forEach { entry ->
            build.add(entry.key, entry.value)
        }
    }.build()
}

fun Headers.contains(key: String, value: String) = values(key).contains(value)

fun HttpUrl.containsPath(vararg path: String) =
    pathSegments().containsAll(path.toList())
