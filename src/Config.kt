import okreplay.OkReplayConfig

object Project {
    @Deprecated("move into a tape config")
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
    val getConfig: OkReplayConfig
        get() = OkReplayConfig.Builder().build()
}
