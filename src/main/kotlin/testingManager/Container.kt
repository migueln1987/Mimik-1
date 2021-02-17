package testingManager

import tapeItems.BaseTape

/**
 * Class which holds:
 * - Accessible tapes
 * - Hooks (how calls are attracted to this container)
 * - Bounds (Session + connection/s)
 */
data class Container(var id: Long = 0L) {
    var name: String? = null
    var tapes: MutableList<BaseTape> = mutableListOf()
    var hooks: MutableList<Hook> = mutableListOf()
    var variables: MutableMap<String, String>? = null
}
