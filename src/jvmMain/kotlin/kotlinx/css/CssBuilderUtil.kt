package kotlinx.css

fun CssBuilder(
    indent: String = "",
    allowClasses: Boolean = true,
    parent: RuleContainer? = null,
    builder: RuleSet
) = CssBuilder(indent, allowClasses, parent).apply(builder)

/**
 * - Null strings: "*"
 * - Else: ensure prefix only contains 1 "."
 */
val String?.asClassStr: String
    get() {
        return when (this) {
            null -> "*"
            else -> ".${trimStart('.')}"
        }
    }

/**
 * Sets the [name] as a class, then appends to the CSS tree.
 *
 * Additional; adding multiple [name] will separate them by a comma
 */
fun CssBuilder.ruleClass(vararg name: String, block: RuleSet = {}) {
    val classNames = name.joinToString { it.asClassStr }
    rule(classNames, passStaticClassesToParent = false, repeatable = true, block)
}

/**
 *  Appends this [name] rule to the CSS tree.
 *
 * Additional; adding multiple [name] will separate them by a comma
 */
fun CssBuilder.rule(vararg name: String, block: RuleSet) {
    val classNames = name.joinToString()
    rule(classNames, passStaticClassesToParent = false, repeatable = true, block)
}

// Children & descendants
fun CssBuilder.classDescendant(selector: String? = null, block: RuleSet): Rule =
    "& ${selector.asClassStr}"(block)

// Combinators
fun CssBuilder.classAdjacentSibling(selector: String, block: RuleSet) =
    "+ ${selector.asClassStr}"(block)

fun CssBuilder.toString(asIndented: Boolean = true): String {
    if (!asIndented) return toString()

    return buildString {
        declarations.forEach {
            append("$indent${it.key.hyphenize()}: ${it.value};\n")
        }

        buildRules(this, indent)
    }
    // .replace("(\\n *)&".toRegex()) { it.groups[1]?.value.orEmpty() }
}

fun RuleContainer.buildRules(builder: StringBuilder, indent: String) {
    val resolvedRules = LinkedHashMap<String, CssBuilder>()
    rules.forEach { (selector, passStaticClassesToParent, block) ->
        if (!resolvedRules.containsKey(selector)) {
            resolvedRules[selector] = CssBuilder(
                "$indent  ",
                allowClasses = false,
                parent = if (passStaticClassesToParent) this else null
            )
        }
        val rule = resolvedRules[selector]!!
        rule.block()
    }

    resolvedRules.forEach { (key, value) ->
        builder.append("$indent$key {\n")
        builder.append(value.toString(true))
        builder.append("$indent}\n")
    }

    multiRules.forEach { (selector, passStaticClassesToParent, block) ->
        val blockBuilder = CssBuilder(
            "$indent  ",
            allowClasses = false,
            parent = if (passStaticClassesToParent) this else null
        ).apply(block)

        builder.append("$indent$selector {\n")
        builder.append(blockBuilder.toString(true))
        builder.append("$indent}\n")
    }
}
