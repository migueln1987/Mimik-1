package io.ktor.response

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.util.*

/**
 * Redirects to an internal path.
 *
 * Note: url path is not updated; the redirected path handles the response instead
 */
suspend fun ApplicationCall.redirectInternally(path: String) {
    // source: https://stackoverflow.com/questions/60443412/how-to-redirect-internally-in-ktor
    val cp = object : RequestConnectionPoint by request.local {
        override val uri: String = path
    }
    val req = object : ApplicationRequest by request {
        override val local: RequestConnectionPoint = cp
    }
    val call = object : ApplicationCall by this {
        override val request: ApplicationRequest = req
    }

    this.application.execute(call, Unit)
}

enum class Redirect {
    /**
     * Replaces the path with [rootPath] + [path]
     */
    Absolute,

    /**
     * Appends the new [path] to the current path
     */
    Relative
}

/**
 * Redirects to a new path
 *
 * @param path New path to be redirected to
 * - prefix "/" is not needed
 * @param redirect How to apply [path]
 * - Absolute: Replaces the path with [rootPath] + [path]
 * - Relative (default): Appends the new [path] to the current path
 */
suspend fun ApplicationCall.redirect(
    path: String = "",
    redirect: Redirect = Redirect.Relative,
    permanent: Boolean = false,
    block: URLBuilder.() -> Unit = {}
) {
    val rootPath = application.environment.rootPath
    val url = url {
        println("currentPath: $currentPath")
        encodedPath = when (redirect) {
            Redirect.Absolute -> {
                if (path.isEmpty())
                    rootPath else "$rootPath/$path"
            }
            Redirect.Relative -> {
                if (rootPath.isEmpty()) {
                    encodedPath.substringBeforeLast("/") + "/$path"
                } else {
                    val subString = encodedPath.substringAfter(rootPath).substringBeforeLast("/").removePrefix("/")
                    "$rootPath/$subString/$path"
                }
            }
        }.replace("//", "/")
        println("Redirect to: $currentPath")
        block(this)
    }

    respondRedirect(url, permanent)
}

suspend fun ApplicationCall.redirect_abs(
    path: String = "",
    permanent: Boolean = false,
    block: URLBuilder.() -> Unit = {}
) = redirect(path, Redirect.Absolute, permanent, block)
