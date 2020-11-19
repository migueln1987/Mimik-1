package networkRouting.editorPages

import kotlinx.html.*
import mimikMockHelpers.SeqActionObject

fun FlowContent.SequenceViewer(seqGroups: ArrayList<SeqActionObject>) {
    script(src = "../assets/libs/v4Parser.js") {}
    script(src = "../assets/libs/LZMA.js") {}
    script(src = "../assets/libs/LZMA_Util.js") {}
    /*
    #level_root table:nth-of-type(odd) { background: #FF00FF; }
    https://css-tricks.com/examples/nth-child-tester/
    */

    div(classes = "sjs_group sjs_col nested-sortable") {
        id = "level_root"
    }

    div {
        id = "data_loader"

        seqGroups.forEach { (ID, Name, Data) ->
            unsafeScript { +"new parserEditor($ID).AddNewSeqList($Data, $Name)" }
        }
        unsafeScript {
            +"document.getElementById('data_loader').remove();"
        }
    }

    button(type = ButtonType.button) {
        onClick = "new parserEditor().AddNewSeqList()"
        +"Add new sequence list"
    }

    unsafeScript { +"enableSortRoot();" }
}
