package org.lesscss

import kotlinx.css.CSSBuilder
import kotlinx.css.toString

object CSSUtil {
    val compiler = LessCompiler()
}

fun CSSBuilder.toCSS(): String = CSSUtil.compiler.compile(toString(true))
