package unitTests.mockHelpers

import com.google.gson.Gson
import helpers.parser.Parser_v4
import helpers.toArrayList
import mimikMockHelpers.RecordedInteractions
import org.junit.Assert
import org.junit.Test

class ChapterTests {

    val gson by lazy { Gson() }

    @Test
    fun loadableSeqData() {
        val useClass = RecordedInteractions()
        val commands = arrayListOf("var[aa]->{e}", "var[bb]->{f}")
        useClass.seqActions_data?.add(commands)

        // setup actions
        val cmd_direct = useClass.seqActions
        val classAsJson = gson.toJsonTree(useClass).asJsonObject
        // result items
        val jsonToClass = gson.fromJson(classAsJson, RecordedInteractions::class.java)
        val cmd_Post = jsonToClass.seqActions

        // do the tests
        Assert.assertEquals(cmd_direct.orEmpty().size, cmd_Post?.size)
        cmd_direct.orEmpty().forEachIndexed { index_root, arrayList ->
            println("Asserting root index: $index_root")

            val cmd_gp = cmd_Post?.getOrNull(index_root)
            Assert.assertNotNull(cmd_gp)
            requireNotNull(cmd_gp)

            arrayList.forEachIndexed { index_item, p4Command ->
                println("Asserting[$index_item]: $p4Command")
                Assert.assertTrue(index_item < cmd_gp.size)
                Assert.assertEquals(commands[index_item], p4Command.toString())
                Assert.assertEquals(p4Command.toString(), cmd_gp[index_item].toString())
            }
        }
    }

    @Test
    fun dataToExport() {
        val useClass = RecordedInteractions()
        val commands = arrayListOf("var[aa]->{e}", "var[bb]->{f}")
            .map { Parser_v4.parseToSteps(it) }.toArrayList()
        useClass.seqActions?.add(commands)
        useClass.prepareSeqForExport()

        val classAsJson = gson.toJsonTree(useClass).asJsonObject
        // result items
        val jsonToClass = gson.fromJson(classAsJson, RecordedInteractions::class.java)
        val cmd_Post = jsonToClass.seqActions

        commands.forEachIndexed { index, item ->
            println("Asserting [$index]: $item")

            val cmdTest = cmd_Post?.getOrNull(0)?.getOrNull(index)
            Assert.assertNotNull(cmdTest)
            requireNotNull(cmdTest)

            Assert.assertEquals(item.toString(), cmdTest.toString())
        }
    }
}
