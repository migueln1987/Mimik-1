@file:UseContextualSerialization(Date::class)

package mimik.tabs

import io.kvision.core.AlignItems
import io.kvision.core.FlexWrap
import io.kvision.form.check.CheckBox
import io.kvision.form.check.Radio
import io.kvision.form.check.RadioGroup
import io.kvision.form.formPanel
import io.kvision.form.range.Range
import io.kvision.form.select.AjaxOptions
import io.kvision.form.select.Select
import io.kvision.form.select.SimpleSelect
import io.kvision.form.spinner.Spinner
import io.kvision.form.text.*
import io.kvision.form.time.DateTime
import io.kvision.form.upload.Upload
import io.kvision.html.ButtonStyle
import io.kvision.html.button
import io.kvision.i18n.I18n
import io.kvision.modal.Alert
import io.kvision.modal.Confirm
import io.kvision.panel.HPanel
import io.kvision.panel.SimplePanel
import io.kvision.progress.Progress
import io.kvision.progress.progressNumeric
import io.kvision.types.KFile
import io.kvision.utils.getDataWithFileContent
import io.kvision.utils.obj
import io.kvision.utils.px
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlin.js.Date

@Serializable
data class Form(
    val text: String? = null,
    val password: String? = null,
    val password2: String? = null,
    val textarea: String? = null,
    val richtext: String? = null,
    val typeahead: String? = null,
    val date: Date? = null,
    val time: Date? = null,
    val checkbox: Boolean = false,
    val radio: Boolean = false,
    val simpleSelect: String? = null,
    val select: String? = null,
    val ajaxselect: String? = null,
    val spinner: Double? = null,
    val range: Double? = null,
    val radiogroup: String? = null,
    val upload: List<KFile>? = null
)

class FormTab : SimplePanel() {
    init {

        this.marginTop = 10.px
        val formPanel = formPanel<Form> {
            add(
                Form::text,
                Text(label = I18n.tr("Required text field with regexp [0-9] validator")).apply {
                    placeholder = I18n.tr("Enter your age")
                },
                required = true,
                requiredMessage = I18n.tr("Value is required"),
                validatorMessage = { I18n.tr("Only numbers are allowed") }) {
                it.getValue()?.matches("^[0-9]+$")
            }
            add(Form::password, Password(label = I18n.tr("Password field with minimum length validator")),
                validatorMessage = { I18n.tr("Password too short") }) {
                (it.getValue()?.length ?: 0) >= 8
            }
            add(Form::password2, Password(label = I18n.tr("Password confirmation")),
                validatorMessage = { I18n.tr("Password too short") }) {
                (it.getValue()?.length ?: 0) >= 8
            }
            add(Form::textarea, TextArea(label = I18n.tr("Text area field")))
            add(
                Form::richtext,
                RichText(label = I18n.tr("Rich text field with a placeholder")).apply {
                    placeholder =
                        I18n.tr("Add some info")
                })
            add(
                Form::typeahead,
                Typeahead(
                    listOf("Alabama", "Alaska", "Arizona", "Arkansas", "California"),
                    label = I18n.tr("Typeahead")
                ).apply {
                    autoSelect = false
                }
            )
            add(
                Form::date,
                DateTime(format = "YYYY-MM-DD", label = I18n.tr("Date field with a placeholder")).apply {
                    placeholder = I18n.tr("Enter date")
                }, legend = I18n.tr("Date and time fieldset")
            )
            add(
                Form::time,
                DateTime(format = "HH:mm", label = I18n.tr("Time field")), legend = I18n.tr("Date and time fieldset")
            )
            add(
                Form::checkbox,
                CheckBox(label = I18n.tr("Required checkbox")),
                required = true,
                validatorMessage = { I18n.tr("Value is required") }
            ) { it.getValue() }
            add(Form::radio, Radio(label = I18n.tr("Radio button")))
            add(
                Form::simpleSelect, SimpleSelect(
                    options = listOf("first" to I18n.tr("First option"), "second" to I18n.tr("Second option")),
                    emptyOption = true,
                    label = I18n.tr("Simple select")
                ), required = true, requiredMessage = I18n.tr("Value is required")
            )
            add(
                Form::select, Select(
                    options = listOf("first" to I18n.tr("First option"), "second" to I18n.tr("Second option")),
                    label = I18n.tr("Advanced select")
                )
            )
            add(Form::ajaxselect, Select(label = I18n.tr("Select with remote data source")).apply {
                emptyOption = true
                ajaxOptions = AjaxOptions("https://api.github.com/search/repositories", preprocessData = {
                    it.items.map { item ->
                        obj {
                            this.value = item.id
                            this.text = item.name
                            this.data = obj {
                                this.subtext = item.owner.login
                            }
                        }
                    }
                }, data = obj {
                    q = "{{{q}}}"
                }, minLength = 3, requestDelay = 1000)
            })
            add(Form::spinner, Spinner(label = I18n.tr("Spinner field 10 - 20"), min = 10, max = 20))
            add(Form::range, Range(label = I18n.tr("Range field 10 - 20"), min = 10, max = 20))
            add(
                Form::radiogroup, RadioGroup(
                    listOf("option1" to I18n.tr("First option"), "option2" to I18n.tr("Second option")),
                    inline = true, label = I18n.tr("Radio button group")
                ), required = true
            )
            add(Form::upload, Upload("/", multiple = true, label = I18n.tr("Upload files (images only)")).apply {
                showUpload = false
                showCancel = false
                explorerTheme = true
                dropZoneEnabled = false
                allowedFileTypes = setOf("image")
            })
            validator = {
                val result = it[Form::password] == it[Form::password2]
                if (!result) {
                    it.getControl(Form::password)?.validatorError = I18n.tr("Passwords are not the same")
                    it.getControl(Form::password2)?.validatorError = I18n.tr("Passwords are not the same")
                }
                result
            }
            validatorMessage = { I18n.tr("The passwords are not the same.") }
        }
        formPanel.add(HPanel(spacing = 10, alignItems = AlignItems.CENTER, wrap = FlexWrap.WRAP) {
            val p = Progress(0, 100) {
                marginBottom = 0.px
                width = 300.px
                progressNumeric {
                    striped = true
                }
            }
            button(I18n.tr("Show data"), "fas fa-info", ButtonStyle.SUCCESS).onClick {
                console.log(formPanel.getDataJson())
                Alert.show(I18n.tr("Form data in plain JSON"), JSON.stringify(formPanel.getDataJson(), space = 1))
                AppScope.launch {
                    console.log(formPanel.getDataWithFileContent())
                }
            }
            button(I18n.tr("Clear data"), "fas fa-times", ButtonStyle.DANGER).onClick {
                Confirm.show(
                    I18n.tr("Are you sure?"),
                    I18n.tr("Do you want to clear your data?"),
                    yesTitle = I18n.tr("Yes"),
                    noTitle = I18n.tr("No"),
                    cancelTitle = I18n.tr("Cancel")
                ) {
                    formPanel.clearData()
                    p.getFirstProgressBar()?.value = 0
                    formPanel.clearValidation()
                }
            }
            button(I18n.tr("Validate"), "fas fa-check", ButtonStyle.INFO).onClick {
                AppScope.launch {
                    p.getFirstProgressBar()?.value = 100
                    delay(500)
                    formPanel.validate()
                }
            }
            add(p)
        })
    }
}
