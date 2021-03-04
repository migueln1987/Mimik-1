package unitTests.mockHelpers

import com.google.gson.GsonBuilder
import kotlinx.collections.toArrayList
import mimik.helpers.parser.Parser_v4
import mimik.mockHelpers.RecordedInteractions
import mimik.mockHelpers.SeqActionObject
import org.junit.Assert
import org.junit.Test

class ChapterTests {

    val gson by lazy {
        GsonBuilder()
            .apply { registerTypeAdapterFactory(SeqActionObject.typeFactory) }
            .create()
    }

    @Test
    fun importExportCommandTests() {
        val useClass = RecordedInteractions()

        // simulate data loaded from a file
        val cmdCommands = arrayListOf(
            arrayListOf("var[aa]->{e}", "var[bb]->{f}"),
            arrayListOf("var[cc]->{g}", "var[dd]->{h}")
        )

        useClass.seqActions = cmdCommands.map { cmbBlock ->
            SeqActionObject().apply {
                cmbBlock
                    .map { cmd -> Parser_v4.parseToCommand(cmd) }
                    .also { Commands = it.toArrayList() }
            }
        }.toArrayList()

        val cmd_Pre = useClass.seqActions!!
        val classAsJson = gson.toJsonTree(useClass).asJsonObject

        // Start test - parsing "loaded" data
        val jsonToClass = gson.fromJson(classAsJson, RecordedInteractions::class.java)
        val cmd_Post = jsonToClass.seqActions!!

        // do the tests
        Assert.assertEquals(cmd_Pre.size, cmd_Post.size)
        cmd_Pre.forEachIndexed { index_root, seqActionObject ->
            println("Asserting root index: $index_root")

            val cmd_gp = cmd_Post.getOrNull(index_root)
            Assert.assertNotNull(cmd_gp)
            requireNotNull(cmd_gp)
            Assert.assertTrue(seqActionObject.Commands.isNotEmpty())
            Assert.assertEquals(seqActionObject.ID, cmd_gp.ID)

            seqActionObject.Commands.forEachIndexed { index_item, p4Command ->
                println("Asserting item[$index_item]: $p4Command")
                val cmdObj = cmd_gp.Commands.getOrNull(index_item)
                Assert.assertNotNull(cmdObj)
                requireNotNull(cmdObj)
                Assert.assertEquals(p4Command.toString(), cmdObj.toString())
            }
        }
    }
}
