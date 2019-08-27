package com.fiserv.ktmimic.tapeTypes.helpers

import okhttp3.Headers

abstract class Tapedata {
    lateinit var headers: Headers
    lateinit var body: String
}
