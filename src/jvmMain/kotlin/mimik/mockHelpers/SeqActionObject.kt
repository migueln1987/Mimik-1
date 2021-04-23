package mimik.mockHelpers

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.*
import kotlinx.tryCast
import mimik.helpers.lzma.LZMA_Encode
import mimik.helpers.lzma.cleanJsonString
import mimik.helpers.parser.P4Command
import mimik.helpers.parser.Parser_v4
import mimik.helpers.toHexString
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * Named sequence group
 */
class SeqActionObject(initNew: Boolean = false, config: (SeqActionObject) -> Unit = {}) {

    var ID: Int = 0
        get() {
            if (field == 0)
                field = Random.nextInt().absoluteValue
            return field
        }
        set(value) {
            field = if (value == 0)
                Random.nextInt().absoluteValue else value
        }

    var Name = ""
        get() {
            if (field.isBlank())
                field = "Sequence " + Random.nextInt().absoluteValue.toString()
            return field
        }
        set(value) {
            field = if (value.isBlank())
                "Sequence " + Random.nextInt().absoluteValue.toString() else value
        }

    var Commands: ArrayList<P4Command> = arrayListOf()

    operator fun component1() = ID
    operator fun component2() = "\"$Name\""
    operator fun component3() = asJSObject

    init {
        if (initNew) initNew()
        config(this)
    }

    fun initNew() {
        ID = 0
        Name = ""
    }

    val asJSObject: String
        get() {
            val data = LZMA_Encode {
                Commands.joinToString(prefix = "[", postfix = "]") { it.asJSObject }
            }.toHexString()

            return """
                {
                    "ID": $ID,
                    "Name": "$Name",
                    "Commands": "$data"
                }
                """.trimIndent().cleanJsonString()
        }

    fun modify(actions: ArrayList<P4Command>.() -> Unit = {}) {
        actions(Commands)
    }

    operator fun invoke(config: (SeqActionObject) -> Unit): SeqActionObject {
        config(this)
        return this
    }

    companion object {
        val typeFactory: TypeAdapterFactory
            get() {
                return object : TypeAdapterFactory {
                    override fun <T : Any?> create(gson: Gson?, type: TypeToken<T>?): TypeAdapter<T>? =
                        if (type?.rawType == SeqActionObject::class.java)
                            typeAdapter.tryCast() else null
                }
            }

        val typeAdapter: TypeAdapter<SeqActionObject>
            get() {
                return object : TypeAdapter<SeqActionObject>() {
                    override fun read(input: JsonReader): SeqActionObject {
                        return SeqActionObject(true).also { newObj ->
                            if (input.peek() != JsonToken.BEGIN_OBJECT) return@also

                            input.readObject {
                                while (input.peek() != JsonToken.END_OBJECT) {
                                    val varName = if (input.peek() == JsonToken.NAME)
                                        input.nextName()
                                    else {
                                        println("Expected JsonToken.NAME, Received ${input.peek()}")
                                        return@readObject
                                    }

                                    when (varName) {
                                        "ID" -> input.readData_nonNull<Int>(varName) { _, data ->
                                            newObj.ID = data
                                        }

                                        "Name" -> input.readData_nonNull<String>(varName) { _, data ->
                                            newObj.Name = data
                                        }

                                        "Commands" -> input.readArray {
                                            newObj.Commands = arrayListOf<P4Command>().also { newCommands ->
                                                while (input.peek() != JsonToken.END_ARRAY)
                                                    input.readData_nonNull<String> { _, Data ->
                                                        newCommands.add(Parser_v4.parseToCommand(Data))
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    override fun write(out: JsonWriter, value: SeqActionObject?) {
                        if (value == null) {
                            out.nullValue()
                            return
                        }

                        out.writeObject {
                            out.writeData("ID", value.ID)
                            out.writeData("Name", value.Name)

                            if (value.Commands.isNotEmpty()) {
                                out.writeData("Commands") {
                                    out.writeArray {
                                        value.Commands.forEach {
                                            out.value(it.toString())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}
