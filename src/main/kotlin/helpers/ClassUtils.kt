package helpers

/**
 * Returns the private field (variable) named [fieldName]
 */
fun <T : Any> T.accessField(fieldName: String): Any? =
    javaClass.getDeclaredField(fieldName).let {
        it.isAccessible = true
        it.get(this)
    }

/**
 * Returns the private function named [funcName], then passes the following [args] to the function
 */
fun <T : Any> T.accessFunction(funcName: String, vararg args: Any): Any? =
    javaClass.getDeclaredMethod(funcName).let {
        it.isAccessible = true
        it.invoke(funcName, args)
    }
