package com.fiserv.ktmimic

import okhttp3.HttpUrl
import okreplay.OkReplayConfig

object Project {
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
