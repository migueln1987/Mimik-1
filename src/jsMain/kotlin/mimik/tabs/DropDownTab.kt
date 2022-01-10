package mimik.tabs

import io.kvision.dropdown.*
import io.kvision.form.check.checkBox
import io.kvision.form.text.text
import io.kvision.html.ButtonStyle
import io.kvision.html.button
import io.kvision.html.image
import io.kvision.html.span
import io.kvision.i18n.I18n
import io.kvision.navbar.nav
import io.kvision.navbar.navForm
import io.kvision.navbar.navLink
import io.kvision.navbar.navbar
import io.kvision.panel.SimplePanel
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.utils.px

class DropDownTab : SimplePanel() {
    init {
        this.marginTop = 10.px
        this.minHeight = 600.px
        vPanel(spacing = 30) {
            navbar("NavBar") {
                nav {
                    navLink(I18n.tr("File"), icon = "fas fa-file")
                    navLink(I18n.tr("Edit"), icon = "fas fa-bars")
                    dropDown(
                        I18n.tr("Favourites"),
                        listOf(I18n.tr("HTML") to "#!/basic", I18n.tr("Forms") to "#!/forms"),
                        icon = "fas fa-star",
                        forNavbar = true
                    )
                }
                navForm {
                    text(label = I18n.tr("Search:"))
                    checkBox(label = I18n.tr("Search")) {
                        inline = true
                    }
                }
                nav(rightAlign = true) {
                    navLink(I18n.tr("System"), icon = "fab fa-windows")
                }
            }
            dropDown(
                I18n.tr("Dropdown with navigation menu"), listOf(
                    I18n.tr("HTML") to "#!/basic",
                    I18n.tr("Forms") to "#!/forms",
                    I18n.tr("Buttons") to "#!/buttons",
                    I18n.tr("Dropdowns") to "#!/dropdowns",
                    I18n.tr("Containers") to "#!/containers"
                ), "fas fa-arrow-right", style = ButtonStyle.SUCCESS
            ).apply {
                minWidth = 250.px
            }
            dropDown(I18n.tr("Dropdown with custom list"), icon = "far fa-image", style = ButtonStyle.WARNING) {
                minWidth = 250.px
                image(io.kvision.require("img/cat.jpg")) { height = 170.px; margin = 10.px; title = "Cat" }
                separator()
                image(io.kvision.require("img/dog.jpg")) { height = 170.px; margin = 10.px; title = "Dog" }
            }
            hPanel(spacing = 5) {
                val fdd = dropDown(
                    I18n.tr("Dropdown with special options"), listOf(
                        I18n.tr("Header") to DD.HEADER.option,
                        I18n.tr("HTML") to "#!/basic",
                        I18n.tr("Forms") to "#!/forms",
                        I18n.tr("Buttons") to "#!/buttons",
                        I18n.tr("Separator") to DD.SEPARATOR.option,
                        I18n.tr("Dropdowns (disabled)") to DD.DISABLED.option,
                        I18n.tr("Separator") to DD.SEPARATOR.option,
                        I18n.tr("Containers") to "#!/containers"
                    ), "fas fa-asterisk", style = ButtonStyle.PRIMARY
                ) {
                    direction = Direction.DROPUP
                    minWidth = 250.px
                }
                button(I18n.tr("Toggle dropdown"), style = ButtonStyle.INFO).onClick { e ->
                    fdd.toggle()
                    e.stopPropagation()
                }
            }
            span(I18n.tr("Open the context menu with right mouse click."))
            contextMenu {
                header(I18n.tr("Menu header"))
                cmLink(I18n.tr("HTML"), "#!/basic")
                cmLink(I18n.tr("Forms"), "#!/forms")
                cmLink(I18n.tr("Buttons"), "#!/buttons")
                cmLink(I18n.tr("Dropdowns"), "#!/dropdowns")
                separator()
                dropDown(I18n.tr("Dropdown"), forDropDown = true) {
                    ddLink(I18n.tr("Containers"), "#!/containers")
                    ddLink(I18n.tr("Layout"), "#!/layout")
                }
            }
        }
    }
}
