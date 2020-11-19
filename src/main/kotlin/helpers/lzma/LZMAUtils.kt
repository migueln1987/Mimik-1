package helpers.lzma

import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.LZMAInputStream
import org.tukaani.xz.LZMAOutputStream
import java.lang.Exception

/**
 * converts a multi-line json into a single line (to reduce unneeded bytes)
 */
fun String.cleanJsonString(): String {
    return replace(" *\n".toRegex(), "\n") // end of line spaces
        .replace("\n *".toRegex(), "") // start of line spaces/ newlines
}

fun uncompressedSize(inData: () -> Any): Int {
    val data = when (val input = inData.invoke()) {
        is ByteArray -> input
        is String -> input.toByteArray()
        else -> byteArrayOf()
    }

    if (data.isEmpty())
        return 0

    // Uncompressed size is an unsigned 64-bit little endian integer.
    // The maximum 64-bit value is a special case (becomes -1 here)
    // which indicates that the end marker is used instead of knowing
    // the uncompressed size beforehand.
    // [0] = props, [1-4] = dictionary
    // [5-12] = uncompressed size
    return data.drop(5).take(8).map { it.toUByte().toInt() }
        .foldIndexed(0) { index, acc, v ->
            acc or (v shl (8 * index))
        }
}

/**
 * Using the input params [LZMA2Options] and LZMA compress the lambda result of [config]
 *
 * Note: If compression fails, the output will contain as much of the data as it could compress
 */
fun LZMA_Encode(config: (LZMA2Options) -> Any): ByteArray {
    val options = LZMA2Options()
    val streamData = when (val streamInput = config.invoke(options)) {
        is ByteArray -> streamInput
        is String -> streamInput.toByteArray()
        else -> byteArrayOf()
    }

    val streamSize = streamData.size

    val outStream = OpenByteArrayOutputStream()

    try {
        LZMAOutputStream(outStream, options, streamSize.toLong()).use { encoder ->
            encoder.write(streamData, 0, streamSize)
        }
    } catch (_: Exception) {
    }

    return outStream.Buffer
}

fun LZMA_Decode(config: () -> String): String = LZMA_Decode(config.invoke())

fun LZMA_Decode(stream: String): String {
    val btStream = stream.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
    return String(LZMA_Decode(btStream))
}

fun LZMA_Decode(config: () -> ByteArray): ByteArray = LZMA_Decode(config.invoke())

fun LZMA_Decode(stream: ByteArray): ByteArray {
    val inStream = OpenByteArrayInputStream(stream)
    val outSize = uncompressedSize { stream }
    val outBuffer = ByteArray(outSize)

    try {
        LZMAInputStream(inStream).use { decode ->
            decode.read(outBuffer, 0, outSize)
        }
    } catch (_: Exception) {
    }

    return outBuffer
}
