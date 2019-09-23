package helpers.attractors

import helpers.isFalse

/**
 * A request attractor which could be optional
 */
class RequestAttractorBit(builder: (RequestAttractorBit) -> Unit = {}) {
    var optional: Boolean? = false
    var value: String = ""

    init {
        builder.invoke(this)
    }

    val required: Boolean
        get() = optional.isFalse()
}
