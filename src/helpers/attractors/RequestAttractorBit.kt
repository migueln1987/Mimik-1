package helpers.attractors

import helpers.isNotTrue
import helpers.isTrue

/**
 * A request attractor which could be optional
 */
class RequestAttractorBit {
    /**
     * When true, the parent RequestAttractor category will match any passed in parameter
     */
    var allowAllInputs: Boolean? = null

    var value: String? = null

    var hardValue: String
        get() = value.orEmpty()
        set(newValue) {
            value = newValue
        }

    var optional: Boolean? = null

    var required: Boolean
        get() = optional.isNotTrue()
        set(value) {
            optional = !value
        }

    /**
     * when true, the regex must not find a match
     */
    var except: Boolean? = null

    val regex
        get() = hardValue.toRegex()

    constructor(builder: (RequestAttractorBit) -> Unit = {}) {
        builder.invoke(this)
    }

    constructor(input: String, builder: (RequestAttractorBit) -> Unit = {}) {
        value = input.removePrefix("/")
        builder.invoke(this)
    }

    fun clone() = RequestAttractorBit {
        it.value = value
        it.optional = optional
        it.except = except
    }

    override fun toString(): String {
        return "Req: %b %s {%s}".format(
            required,
            if (except.isTrue()) "-!" else "+",
            hardValue
        )
    }

    override fun equals(other: Any?) =
        (other is RequestAttractorBit) && (other.toString() == toString())

    override fun hashCode() = toString().hashCode()
}
