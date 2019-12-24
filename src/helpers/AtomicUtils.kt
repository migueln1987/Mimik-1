package helpers

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic

val <T : Any> T.asAtomic: AtomicRef<T>
    get() = atomic(this)
