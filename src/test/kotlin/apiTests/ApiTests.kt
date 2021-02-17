package apiTests

import MimikContainer
import org.junit.After
import org.junit.Before

interface ApiTests {
    @Before
    @After
    fun clearTapes() {
        MimikContainer.tapeCatalog.tapes.forEach {
            if (it.savingFile.get())
                println("Waiting to delete file: ${it.name}")
            while (it.savingFile.get()) {
                Thread.sleep(2)
            }
            println("Deleting tape: ${it.name}")
            it.file?.delete()
        }
        MimikContainer.tapeCatalog.tapes.clear()
    }
}
