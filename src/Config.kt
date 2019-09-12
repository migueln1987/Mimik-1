package com.fiserv.mimik

import okhttp3.HttpUrl
import okreplay.OkReplayConfig

object Project {
    @Deprecated("move into a tape")
    val outboundUrl: HttpUrl = HttpUrl.get("https://dit1-cardvalet-m.fiservapps.com")

    @Deprecated("move into a tape")
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
