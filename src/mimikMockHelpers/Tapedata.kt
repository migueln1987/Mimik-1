package com.fiserv.mimik.mimikMockHelpers

import okhttp3.Headers

abstract class Tapedata {
    lateinit var headers: Headers
    var body: String? = null
}
