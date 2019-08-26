package com.fiserv.ktmimic

import io.ktor.util.url
import okhttp3.HttpUrl
import okreplay.OkReplayConfig
import java.io.File

object Project {
    val tapeDir = "src/tapes"
    val outboundUrl: HttpUrl = HttpUrl.get("https://dit1-cardvalet-m.fiservapps.com")

    val ignoreParams = listOf(
        "appVersion",
        "language",
        "osVersion",
        "timestamp",
        "transactionEndDate",
        "transactionStartDate",
        "deviceUniqueId",
        "countryCode",
        "deviceType",
        "currentCountryCode",
        "currentNetwork",
        "rememberUser",
        "fiToken",
        "subscriberReferenceIds",
//         "subscriberReferenceId",
        "longitude",
        "latitude",
        "accuracy"
    )
}

object VCRConfig {
    val getConfig: OkReplayConfig = OkReplayConfig.Builder()
        .build()
}
