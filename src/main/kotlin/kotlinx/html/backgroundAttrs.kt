package kotlinx.html

val CommonAttributeGroupFacade.disabledBG: Unit
    get() = appendStyles("background-color: #E0E0E0")

val CommonAttributeGroupFacade.readonlyBG: Unit
    get() = appendStyles("background-color: #F0F0F0")

class BackgroundConfigs(private val attributeGroup: CommonAttributeGroupFacade) {
    var color: String
        get() = attributeGroup.styleProxy("background-color", null)
        set(value) {
            attributeGroup.styleProxy("background-color", value)
        }
}

fun CommonAttributeGroupFacade.background(backgroundConfig: BackgroundConfigs.() -> Unit) =
    backgroundConfig.invoke(BackgroundConfigs(this))
