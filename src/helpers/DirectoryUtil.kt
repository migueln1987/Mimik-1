package com.fiserv.mimik.helpers

import java.io.File

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
 * Retrieves a list of files form the input file/ directory
 */
fun File.fileListing(): List<File> {
    return (if (isFile) File(canonicalPath) else this@fileListing)
        .walkTopDown()
        .filter { it.isFile && it.extension == "json" }
        .toList()
}
