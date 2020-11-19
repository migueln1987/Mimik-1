package helpers

import java.util.concurrent.atomic.AtomicReference

val <T : Any> T.asAtomic: AtomicReference<T>
    get() = AtomicReference(this)
