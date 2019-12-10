package helpers

import com.google.gson.JsonElement
import java.io.File
import kotlin.math.log
import kotlin.math.pow

/**
 * Returns a list of directories from this file's directory
 */
fun File.getFolders(): List<String> {
    return (if (isFile) File(canonicalPath) else this)
        .walkTopDown()
        .filter { it.isDirectory }
        .map { it.path }
        .toList()
}

/**
 * Retrieves a list of json files form the input file/ directory
 */
fun File.jsonFiles(): List<File> {
    return (if (isFile) File(canonicalPath) else this@jsonFiles)
        .walkTopDown()
        .filter { it.isFile && it.extension == "json" }
        .toList()
}

/**
 * Returns this [Double] in the form of "B/ KB/ MB, etc."
 */
fun JsonElement.fileSize() =
    toString().length.toDouble().fileSize()

/**
 * Returns this [File] in the form of "B/ KB/ MB, etc."
 */
fun File.fileSize(): String {
    return try {
        length().toDouble().fileSize()
    } catch (e: Exception) {
        "- B"
    }
}

/**
 * Returns this [Double] in the form of "B/ KB/ MB, etc."
 */
fun Double.fileSize(): String {
    val unit = 1024.0
    if (this < unit) return "$this bytes"
    val exp = log(this, unit).toInt()
    val pre = "KMGTPE"[exp - 1]
    return "%.1f %sB".format(this / unit.pow(exp), pre)
}
