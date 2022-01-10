package mimik.tabs

import io.kvision.core.*
import io.kvision.data.BaseDataComponent
import io.kvision.data.DataContainer
import io.kvision.html.Align
import io.kvision.html.Div
import io.kvision.i18n.I18n
import io.kvision.panel.SimplePanel
import io.kvision.panel.VPanel
import io.kvision.panel.hPanel
import io.kvision.state.observableListOf
import io.kvision.utils.px

class DragDropTab : SimplePanel() {
    init {
        this.marginTop = 10.px
        this.minHeight = 400.px

        class DataModel(text: String) : BaseDataComponent() {
            var text: String by obs(text)
        }

        val listGreen = observableListOf(
            DataModel(I18n.tr("January")),
            DataModel(I18n.tr("February")),
            DataModel(I18n.tr("March")),
            DataModel(I18n.tr("April")),
            DataModel(I18n.tr("May")),
            DataModel(I18n.tr("June")),
            DataModel(I18n.tr("July")),
            DataModel(I18n.tr("August")),
            DataModel(I18n.tr("September")),
            DataModel(I18n.tr("October")),
            DataModel(I18n.tr("November"))
        )

        val listBlue = observableListOf(
            DataModel(I18n.tr("December"))
        )

        val dataContainer1 = DataContainer(listGreen, { _, index, _ ->
            Div(listGreen[index].text, align = Align.CENTER) {
                padding = 3.px
                border = Border(1.px, BorderStyle.DASHED)
                setDragDropData("text/plain", "$index")
            }
        }, container = VPanel(spacing = 10) {
            width = 200.px
            padding = 10.px
            border = Border(2.px, BorderStyle.SOLID, Color.name(Col.GREEN))
            setDropTargetData("text/xml") { data ->
                if (data != null) {
                    val element = listBlue[data.toInt()].text
                    listBlue.removeAt(data.toInt())
                    listGreen.add(DataModel(element))
                }
            }
        })

        val dataContainer2 = DataContainer(listBlue, { _, index, _ ->
            Div(listBlue[index].text, align = Align.CENTER) {
                padding = 3.px
                border = Border(1.px, BorderStyle.DASHED)
                setDragDropData("text/xml", "$index")
            }
        }, container = VPanel(spacing = 10) {
            width = 200.px
            padding = 10.px
            border = Border(2.px, BorderStyle.SOLID, Color.name(Col.BLUE))
            setDropTargetData("text/plain") { data ->
                if (data != null) {
                    val element = listGreen[data.toInt()].text
                    listGreen.removeAt(data.toInt())
                    listBlue.add(DataModel(element))
                }
            }
        })

        val panel = hPanel(justify = JustifyContent.CENTER, alignItems = AlignItems.FLEXSTART, spacing = 50)
        panel.add(dataContainer1)
        panel.add(dataContainer2)

    }
}
