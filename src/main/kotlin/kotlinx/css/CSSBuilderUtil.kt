package kotlinx.css
// 
// fun CSSBuilder(
//     indent: String = "",
//     allowClasses: Boolean = true,
//     parent: RuleContainer? = null,
//     builder: RuleSet
// ) = CSSBuilder(indent, allowClasses, parent).apply(builder)
// 
// fun CSSBuilder.classRule(name: String, block: RuleSet): Rule {
//     return rule(".$name", block)
// }
// 
// // Children & descendants
// fun CSSBuilder.classDescendant(selector: String? = null, block: RuleSet) = "& ${selector?.let { ".$it" } ?: "*"}"(block)
// 
// // Combinators
// fun CSSBuilder.classAdjacentSibling(selector: String, block: RuleSet) = "+ .$selector"(block)
// 
// fun CSSBuilder.toString(asIndented: Boolean = true): String {
//     if (!asIndented) return this.toString()
// 
//     return buildString {
//         declarations.forEach {
//             append("$indent${it.key.hyphenize()}: ${it.value};\n")
//         }
// 
//         buildRules(this, indent)
//     }
// //        .replace("(\\n *)&".toRegex()) { it.groups[1]?.value.orEmpty() }
// }
// 
// fun RuleContainer.buildRules(builder: StringBuilder, indent: String) {
//     val resolvedRules = LinkedHashMap<String, CSSBuilder>()
//     rules.forEach { (selector, passStaticClassesToParent, block) ->
//         if (!resolvedRules.containsKey(selector)) {
//             resolvedRules[selector] = CSSBuilder(
//                 "$indent  ",
//                 allowClasses = false,
//                 parent = if (passStaticClassesToParent) this else null
//             )
//         }
//         val rule = resolvedRules[selector]!!
//         rule.block()
//     }
// 
//     resolvedRules.forEach { (key, value) ->
//         builder.append("$indent$key {\n")
//         builder.append(value.toString(true))
//         builder.append("$indent}\n")
//     }
// 
//     multiRules.forEach { (selector, passStaticClassesToParent, block) ->
//         val blockBuilder = CSSBuilder(
//             "$indent  ",
//             allowClasses = false,
//             parent = if (passStaticClassesToParent) this else null
//         ).apply(block)
// 
//         builder.append("$indent$selector {\n")
//         builder.append(blockBuilder.toString(true))
//         builder.append("$indent}\n")
//     }
// }
