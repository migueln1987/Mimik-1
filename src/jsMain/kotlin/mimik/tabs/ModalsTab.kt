package mimik.tabs

import io.kvision.core.onEvent
import io.kvision.form.check.checkBox
import io.kvision.form.text.TextInput
import io.kvision.form.text.TextInputType
import io.kvision.form.text.textInput
import io.kvision.html.*
import io.kvision.i18n.I18n
import io.kvision.i18n.gettext
import io.kvision.modal.Alert
import io.kvision.modal.Confirm
import io.kvision.modal.Modal
import io.kvision.panel.SimplePanel
import io.kvision.panel.vPanel
import io.kvision.toast.Toast
import io.kvision.utils.px
import io.kvision.window.Window
import kotlin.random.Random

class ModalsTab : SimplePanel() {
    init {
        this.marginTop = 10.px
        this.minHeight = 400.px
        vPanel(spacing = 30) {
            button(I18n.tr("Alert dialog"), style = ButtonStyle.DANGER).onClick {
                Alert.show(
                    I18n.tr("Alert dialog"),
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce nec fringilla turpis, vel molestie dolor. Vestibulum ut ex eget orci porta gravida eu sit amet tortor."
                )
            }
            button(I18n.tr("Confirm dialog"), style = ButtonStyle.WARNING).onClick {
                Confirm.show(
                    I18n.tr("Confirm dialog"),
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce nec fringilla turpis, vel molestie dolor. Vestibulum ut ex eget orci porta gravida eu sit amet tortor.",
                    yesTitle = I18n.tr("Yes"),
                    noTitle = I18n.tr("No"),
                    cancelTitle = I18n.tr("Cancel"),
                    noCallback = {
                        Alert.show(I18n.tr("Result"), I18n.tr("You pressed NO button."))
                    }) {
                    Alert.show(I18n.tr("Result"), I18n.tr("You pressed YES button."))
                }
            }
            button(I18n.tr("Cancelable confirm dialog"), style = ButtonStyle.INFO).onClick {
                Confirm.show(
                    I18n.tr("Cancelable confirm dialog"),
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce nec fringilla turpis, vel molestie dolor. Vestibulum ut ex eget orci porta gravida eu sit amet tortor.",
                    align = Align.CENTER,
                    cancelVisible = true,
                    yesTitle = I18n.tr("Yes"),
                    noTitle = I18n.tr("No"),
                    cancelTitle = I18n.tr("Cancel"),
                    noCallback = {
                        Alert.show(I18n.tr("Result"), I18n.tr("You pressed NO button."))
                    }) {
                    Alert.show(I18n.tr("Result"), I18n.tr("You pressed YES button."))
                }
            }
            val modal = Modal(I18n.tr("Custom modal dialog"))
            modal.add(
                H4(
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce nec fringilla turpis, vel molestie dolor. Vestibulum ut ex eget orci porta gravida eu sit amet tortor."
                )
            )
//            modal.add(Image(io.kvision.require("img/dog.jpg")))
            modal.addButton(Button(I18n.tr("Close")).onClick {
                modal.hide()
            })
            button(I18n.tr("Custom modal dialog"), style = ButtonStyle.SUCCESS).onClick {
                modal.show()
            }
            button(I18n.tr("Alert dialog without animation"), style = ButtonStyle.PRIMARY).onClick {
                Alert.show(
                    I18n.tr("Alert dialog without animation"),
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce nec fringilla turpis, vel molestie dolor. Vestibulum ut ex eget orci porta gravida eu sit amet tortor.",
                    animation = false
                )
            }
            var counter = 1
            button(I18n.tr("Open new window"), style = ButtonStyle.PRIMARY, icon = "far fa-window-maximize").onClick {
                val sw = ShowcaseWindow(I18n.gettext("Window") + " " + counter++) {
                    left = ((Random.nextDouble() * 800).toInt()).px
                    top = ((Random.nextDouble() * 300).toInt()).px
                }
                this@ModalsTab.add(sw)
                sw.focus()
            }
            button(I18n.tr("Show toast message"), style = ButtonStyle.PRIMARY, icon = "fas fa-info-circle").onClick {
                Toast.info(gettext("This is a toast message"))
            }
        }
    }
}

class ShowcaseWindow(caption: String?, init: (ShowcaseWindow.() -> Unit)? = null) :
    Window(caption, 600.px, 300.px, closeButton = true) {

    lateinit var captionInput: TextInput

    init {
        init?.invoke(this)
        vPanel {
            margin = 10.px
            this@ShowcaseWindow.captionInput = textInput(TextInputType.TEXT, caption) {
                onEvent {
                    change = {
                        this@ShowcaseWindow.caption = self.value
                    }
                }
            }
            checkBox(true, label = I18n.tr("Draggable")).onClick {
                this@ShowcaseWindow.isDraggable = this.value
            }
            checkBox(true, label = I18n.tr("Resizable")).onClick {
                this@ShowcaseWindow.isResizable = this.value
            }
            checkBox(true, label = I18n.tr("Close button")).onClick {
                this@ShowcaseWindow.closeButton = this.value
            }
        }
    }

    override fun focus() {
        super.focus()
        captionInput.focus()
    }
}
