package kotlinx.atomicfu

val <T : Any> T.asAtomic: AtomicRef<T>
    get() = atomic(this)
