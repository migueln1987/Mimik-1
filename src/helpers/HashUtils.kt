package helpers

import java.security.MessageDigest

private const val HEX_CHARS = "0123456789ABCDEF"

/**
 * Supported algorithms on Android:
 *
 * Algorithm	Supported API Levels
 * MD5          1+
 * SHA-1	    1+
 * SHA-224	    1-8,22+
 * SHA-256	    1+
 * SHA-384	    1+
 * SHA-512	    1+
 */
private fun hashString(type: String, input: String): String {
    val bytes = MessageDigest
        .getInstance(type)
        .digest(input.toByteArray())
    val result = StringBuilder(bytes.size * 2)

    bytes.forEach {
        val i = it.toInt()
        result.append(HEX_CHARS[i shr 4 and 0x0f])
        result.append(HEX_CHARS[i and 0x0f])
    }

    return result.toString()
}

val String.hashMD5
    get() = hashString("MD5", this)

val String.hashSHA1
    get() = hashString("SHA-1", this)

val String.hashSha256
    get() = hashString("SHA-256", this)

val String.hashSha512
    get() = hashString("SHA-512", this)
