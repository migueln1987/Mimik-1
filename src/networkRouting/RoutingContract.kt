package com.fiserv.mimik.networkRouting

import io.ktor.routing.Routing

abstract class RoutingContract(val path: String) {
    companion object {
        internal var selfPath = ""
    }

    init {
        selfPath = path
    }

    abstract fun init(route: Routing)
}
