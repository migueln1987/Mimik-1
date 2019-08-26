package com.fiserv.ktmimic.tapeTypes

class CFCTape : baseTape() {
    override fun getName() = "CFC_Tape"

    override val opIds = arrayOf(
        "CFC_ELIGIBILITY_AND_GET_TOKEN",
        "CFC_GEN_NEW_TOKEN",
        "CFC_REGISTER_DEVICE",
        "CFC_ACCEPT_TNC"
    )

}
