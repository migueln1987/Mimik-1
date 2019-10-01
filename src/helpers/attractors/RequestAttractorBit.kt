package helpers.attractors

import helpers.isTrue

/**
 * A request attractor which could be optional
 */
class RequestAttractorBit {
    lateinit var value: String

    var hardValue: String
        get() = if (::value.isInitialized) value else ""
        set(newValue) {
            value = newValue
        }

    var optional: Boolean? = false

    var required: Boolean
        get() {
            return optional.isTrue().not()
        }
        set(value) {
            optional = !value
        }

    val regex
        get() = hardValue.toRegex()

    constructor(builder: (RequestAttractorBit) -> Unit = {}) {
        builder.invoke(this)
    }

    constructor(input: String) {
        value = input.removePrefix("/")
    }

    override fun toString(): String {
        return "Req: $required - {$hardValue}"
    }
}
