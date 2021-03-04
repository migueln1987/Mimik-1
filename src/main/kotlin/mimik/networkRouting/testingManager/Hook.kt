package mimik.networkRouting.testingManager

import helpers.attractors.HookAttractor
import mimik.mockHelpers.SeqActionObject

/**
 * Traiger for incoming calls
 * 1. How a call will be attracted to a container
 * 2. How to convert the call to a bounds
 *
 * Each connection is assumed to be from the same device/ source
 */
class Hook {
    var attractors: MutableList<HookAttractor>? = null
    var actions: MutableList<SeqActionObject>? = null
}
