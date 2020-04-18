package helpers

fun <T : Any> T.accessField(fieldName: String): Any? =
    javaClass.getDeclaredField(fieldName).let {
        it.isAccessible = true
        it.get(this)
    }

fun <T : Any> T.accessFunction(funcName: String, vararg args: Any): Any? =
    javaClass.getDeclaredMethod(funcName).let {
        it.isAccessible = true
        it.invoke(funcName, args)
    }
