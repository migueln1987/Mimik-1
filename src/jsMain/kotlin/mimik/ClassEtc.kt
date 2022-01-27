package mimik

import io.kvision.core.Style

//import mimik.helpers.firstNotExceptionOf
//import java.lang.reflect.ParameterizedType
//import kotlin.reflect.KClass
//
//private fun <T : Any> Class<*>.checkAction(checker: (Class<*>?) -> T?): T? {
//    var checkClass: Class<*>? = this
//    var result: T? = null
//    var done = false
//
//    do {
//        try {
//            result = checker(checkClass)
//            done = true
//        } catch (e: Exception) {
//            checkClass = checkClass?.superclass
//        }
//    } while (!done && checkClass != null)
//    return result
//}
//
///**
// * Returns the private field (variable) named [fieldName]
// */
//fun <T : Any> Any.accessField(fieldName: String): T? {
//    val checker: (Class<*>?) -> T? = { item ->
//        item?.getDeclaredField(fieldName)?.let {
//            it.isAccessible = true
//            @Suppress("UNCHECKED_CAST")
//            it.get(this) as? T
//        }
//    }
//
//    return javaClass.checkAction(checker)
//}
//
///**
// * Returns the private function named [funcName], then passes the following [args] to the function
// */
//fun Any.accessFunction(funcName: String, vararg args: Any): Any? {
//    val checker: (Class<*>?) -> Any? = { item ->
//        item?.getDeclaredMethod(funcName)?.let {
//            it.isAccessible = true
//            it.invoke(funcName, args)
//        }
//    }
//
//    return javaClass.checkAction(checker)
//}

val Style.cssClassName: String
    get() = this.selector.split(' ').last().split('.').last()