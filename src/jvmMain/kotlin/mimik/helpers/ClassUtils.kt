package helpers

import mimik.helpers.firstNotExceptionOf
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

private fun <T : Any> Class<*>.checkAction(checker: (Class<*>?) -> T?): T? {
    var checkClass: Class<*>? = this
    var result: T? = null
    var done = false

    do {
        try {
            result = checker(checkClass)
            done = true
        } catch (e: Exception) {
            checkClass = checkClass?.superclass
        }
    } while (!done && checkClass != null)
    return result
}

/**
 * Returns the private field (variable) named [fieldName]
 */
fun <T : Any> Any.accessField(fieldName: String): T? {
    val checker: (Class<*>?) -> T? = { item ->
        item?.getDeclaredField(fieldName)?.let {
            it.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            it.get(this) as? T
        }
    }

    return javaClass.checkAction(checker)
}

/**
 * Returns the private function named [funcName], then passes the following [args] to the function
 */
fun Any.accessFunction(funcName: String, vararg args: Any): Any? {
    val checker: (Class<*>?) -> Any? = { item ->
        item?.getDeclaredMethod(funcName)?.let {
            it.isAccessible = true
            it.invoke(funcName, args)
        }
    }

    return javaClass.checkAction(checker)
}

@Suppress("UNCHECKED_CAST")
fun <X> Any.typeArgs(index: Int): Class<X>? {
    return (javaClass.genericSuperclass as? ParameterizedType)
        ?.actualTypeArguments?.getOrNull(index) as? Class<X>
}

/**
 * Returns an instance of [T] (with optional [args]), or null.
 *
 * Exceptions are ignored.
 *
 * Example:
 * ```
 * val a = instanceOf(Foo::class, "input1", 5)
 * ```
 */

fun <T : Any> instanceOf(klass: KClass<T>, vararg args: Any): T? {
    val constructors = klass.java.constructors

    @Suppress("UNCHECKED_CAST")
    val item = constructors.firstNotExceptionOf { it.newInstance(args) as T }
        ?: constructors.firstNotExceptionOf { it.newInstance() as T }

    if (item == null)
        println("Unable to create instance of: ${klass.java.canonicalName}")

    return item
}

/**
 * Returns an instance of [T] (with optional [args]), or null.
 *
 * Exceptions are ignored.
 *
 *  * Examples:
 * ```
 *  val a = instanceOf<Foo>("input1", 5)
 *  val b: Foo = instanceOf("input1", 5)
 * ```
 */
inline fun <reified T : Any> instanceOf(vararg args: Any): T? =
    instanceOf(T::class, args)
