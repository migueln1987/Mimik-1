package com.fiserv.mimik.tapeTypes

class GeneralTape : baseTape() {
    override fun getName() = "General Tape"

    override val chapterTitles = arrayOf(
        "GET_APP_INFO",
        "LOGIN_MULTISTEP"
    )
}
