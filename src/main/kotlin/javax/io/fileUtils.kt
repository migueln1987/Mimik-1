package javax.io

import com.google.gson.JsonElement
import kotlinx.tryOrNull
import java.io.File
import kotlin.math.log
import kotlin.math.pow

/**
 * Returns a list of directories from this file's directory
 */
val File.foldersPaths: List<String>
    get() {
        return (if (isFile) File(canonicalPath) else this)
            .walkTopDown()
            .filter { it.isDirectory }
            .map { it.path }
            .toList()
    }

/**
 * Retrieves a list of json files from the input file/ directory
 */
fun File.jsonFiles(): List<File> = filesByType("json")

/**
 * Retrieves a list of files with the extension, [ext]
 */
fun File.filesByType(ext: String): List<File> {
    return (if (isFile) File(canonicalPath) else this@filesByType)
        .walkTopDown()
        .filter { it.isFile && it.extension == ext }
        .toList()
}

// todo; rename fileSize to be more specific of result data

/**
 * Returns this [File] in the form of "B/ KB/ MB, etc."
 */
fun File.fileSize(): String =
    tryOrNull { length().toDouble().fileSize() } ?: "- B"

/**
 * Returns this [Double] in the form of "B/ KB/ MB, etc."
 */
fun JsonElement.fileSize() =
    toString().length.toDouble().fileSize()

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
