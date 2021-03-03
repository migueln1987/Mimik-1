package mimik.networkRouting.help

import kotlinUtils.text.appendItem
import kotlinUtils.text.appendObject
import mimik.helpers.toJson

enum class CreateTypes {
    Tape, Chapter
}

@Suppress("PropertyName")
object TemplateBuilder {
    object TemplateItems {
        enum class ItemCounts {
            One, Many
        }

        enum class FlagTypes {
            Int, Bool
        }

        interface TemplateItem {
            var Description: String?
        }

        class SuffixItems {
            val Optional = "~"
            val Except = "!"
            val isBase64 = "#"
        }

        class DataFormatItems : TemplateItem {
            override var Description: String? = null
            var Regex: Boolean? = null
            var Style: String? = null
            var Style_A: String? = null
            var Style_B: String? = null
            var Allow_Any: String? = null
                private set

            @Transient
            var hasAllowAny = false
                set(value) {
                    field = value
                    if (value)
                        Allow_Any = ".*"
                }

            operator fun invoke(config: DataFormatItems.() -> Unit): DataFormatItems {
                config(this)
                return this
            }
        }

        class AttractorBuilder : TemplateItem {
            override var Description: String? = null
            var Count: ItemCounts = ItemCounts.One
            var Required: Boolean = true
            var Key_Format: String? = null
            var Key_Suffix: SuffixItems? = SuffixItems()
                private set

            @Transient
            var hasKeySuffix = true
                set(value) {
                    field = value
                    Key_Suffix = if (field)
                        SuffixItems() else null
                }

            var Data_Format: DataFormatItems = DataFormatItems()

            @Suppress("unused")
            private constructor()
            constructor(config: (AttractorBuilder) -> Unit) {
                config(this)
            }

            fun postEdit(config: (AttractorBuilder) -> Unit): AttractorBuilder {
                config(this)
                return this
            }

            override fun toString() = this.toJson
        }

        class FlagBuilder(config: FlagBuilder.() -> Unit = {}) : TemplateItem {
            override var Description: String? = null

            @Transient
            var prefix: String = "mock"
            var Format: String = ""

            /**
             * Type of variable
             *
             * Default: Bool
             */
            var Type: FlagTypes = FlagTypes.Bool
            var Key: String = ""

            init {
                config(this)
            }
        }

        /**
         * Creates a string in the format of:
         * - mock${prefix}$name
         *
         * Attributes are:
         * - Key Suffix: False
         * - Regex: False
         * - Style: String
         */
        inline fun header_Item(
            name: String,
            prefix: String = "",
            config: AttractorBuilder.() -> Unit = {}
        ): String {
            return AttractorBuilder {
                it.Key_Format = "mock${prefix}$name"
                it.hasKeySuffix = false
                it.Data_Format {
                    Regex = false
                    Style = "String"
                }
            }.also { config(it) }.toString()
        }

        fun header_attrItem(
            Key: String,
            edit: (AttractorBuilder) -> Unit
        ): String {
            return AttractorBuilder {
                it.Count = ItemCounts.Many
                it.Key_Format = "mockFilter_$Key"
            }.postEdit(edit).toString()
        }

        inline fun header_flags(
            Key: String,
            edit: (FlagBuilder) -> Unit = {}
        ): String {
            val builder = FlagBuilder {
                this.Key = Key
                this.Format = "$prefix$Key"
            }
            edit(builder)
            return builder.toJson
        }
    }

    fun build(create: CreateTypes): String {
        return buildString {
            appendObject("") {
                appendObject("Headers") {
                    if (create == CreateTypes.Chapter)
                        appendItem("TapeName", ",") {
                            TemplateItems.header_Item("Name", "Tape")
                        }
                    appendItem("Name", ",") {
                        TemplateItems.header_Item("Name") {
                            Required = false
                        }
                    }

                    if (create == CreateTypes.Tape)
                        appendItem("Url", ",") {
                            TemplateItems.header_Item("Url") {
                                Required = false
                            }
                        }

                    appendObject("Attractors", ",") {
                        appendObject("Path", ",") { objName ->
                            TemplateItems.header_attrItem(objName) {
                                it.Count = TemplateItems.ItemCounts.One
                                it.hasKeySuffix = false
                                it.Data_Format {
                                    Regex = true
                                    Style = "String"
                                }
                            }
                        }

                        appendObject("Query", ",") { objName ->
                            TemplateItems.header_attrItem(objName) {
                                it.Required = false
                                it.Data_Format {
                                    Regex = true
                                    Style_A = "Field=Value"
                                    Style_B = "Field1=Value1&Field2=Value2"
                                    hasAllowAny = true
                                }
                            }
                        }

                        appendObject("Header", ",") { objName ->
                            TemplateItems.header_attrItem(objName) {
                                it.Required = false
                                it.Data_Format {
                                    Regex = true
                                    Style = "Key=Value"
                                    hasAllowAny = true
                                }
                            }
                        }

                        appendObject("Body") { objName ->
                            TemplateItems.header_attrItem(objName) {
                                it.Required = false
                                it.Data_Format {
                                    Regex = true
                                    Style = "String"
                                    hasAllowAny = true
                                }
                            }
                        }
                    }

                    appendObject("Flags") {
                        appendItem("Save to File", ",") {
                            TemplateItems.header_flags("SaveToFile")
                        }

                        appendItem("Live", ",") {
                            TemplateItems.header_flags("Live")
                        }

                        when (create) {
                            CreateTypes.Tape -> {
                                appendItem("RecordByFilter") {
                                    TemplateItems.header_flags("RecordNew")
                                }
                            }

                            CreateTypes.Chapter -> {
                                appendItem("Enabled", ",") {
                                    TemplateItems.header_flags("Enabled")
                                }

                                appendItem("Usages", ",") {
                                    TemplateItems.header_flags("Uses") {
                                        it.Type = TemplateItems.FlagTypes.Int
                                    }
                                }
                            }
                        }
                    }
                }

                if (create == CreateTypes.Chapter) {
                    append(",")
                    appendObject("Body") {
                        appendObject("Standard", ",") {
                            appendItem("Format") { "Any" }
                        }
                        appendObject("Enhanced") {
                            appendItem("Format", ",") { "Json" }
                            appendObject("Data") {
                                appendObject("Response", ",") {
                                    appendItem("Key", ",") { "Response" }
                                    appendItem("Data") { "String" }
                                }
                                appendObject("Sequence") {
                                    appendObject("Output") {
                                        appendItem("Key", ",") { "Sequences" }
                                        appendItem("Data") { "Json" }
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
