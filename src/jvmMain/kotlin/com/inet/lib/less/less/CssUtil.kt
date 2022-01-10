package com.inet.lib.less.less

import com.inet.lib.less.Less
import kotlinx.css.CssBuilder
import kotlinx.css.toString

fun CssBuilder.toCSS(): String = Less.compile(null, toString(true), false)
