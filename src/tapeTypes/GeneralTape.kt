package com.fiserv.ktmimic.tapeTypes

class GeneralTape : baseTape() {
    override fun getName() = "GeneralTape"

    override val opIds = arrayOf(
        "LOGIN_MULTISTEP"
    )
}
