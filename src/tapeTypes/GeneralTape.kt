package com.fiserv.ktmimic.tapeTypes

class GeneralTape : baseTape() {
    override fun getName() = "GeneralTape"

    override val chapterTitles = arrayOf(
        "LOGIN_MULTISTEP"
    )
}
