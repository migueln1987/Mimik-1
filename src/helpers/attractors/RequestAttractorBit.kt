package helpers.attractors

import helpers.isFalse

/**
 * A request attractor which could be optional
 */
class RequestAttractorBit {
    var optional: Boolean? = false
    var value: String = ""

    val regex
        get() = value.toRegex()

    constructor(builder: (RequestAttractorBit) -> Unit = {}) {
        builder.invoke(this)
    }

    constructor(input: String) {
        value = input.removePrefix("/")
    }

    val required: Boolean
        get() = optional.isFalse()
}
