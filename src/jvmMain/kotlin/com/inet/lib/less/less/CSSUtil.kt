package com.inet.lib.less.less

import com.inet.lib.less.Less
import kotlinx.css.CSSBuilder
import kotlinx.css.toString

fun CSSBuilder.toCSS(): String = Less.compile(null, toString(true), false)
