package apiTests

import mimik.TapeCatalog
import org.junit.After
import org.junit.Before

interface ApiTests {
    @Before
    @After
    fun clearTapes() {
        TapeCatalog.Instance.tapes.forEach {
            if (it.savingFile.get())
                println("Waiting to delete file: ${it.name}")
            while (it.savingFile.get()) {
                Thread.sleep(2)
            }
            println("Deleting tape: ${it.name}")
            it.file?.delete()
        }
        TapeCatalog.Instance.tapes.clear()
    }
}
