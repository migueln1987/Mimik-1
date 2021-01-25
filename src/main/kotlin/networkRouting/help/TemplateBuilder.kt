package networkRouting.help

import helpers.append
import helpers.toJson

enum class CreateTypes {
    Tape, Chapter
}

object TemplateBuilder {
    object TemplateItems {
        enum class ItemCounts {
            One, Many
        }

        enum class FlagTypes {
            Int, Bool
        }

        class SuffixItems {
            val Optional = "~"
            val Except = "!"
            val isBase64 = "#"
        }

        class DataFormatItems {
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

            operator fun invoke(config: (DataFormatItems) -> Unit): DataFormatItems {
                config.invoke(this)
                return this
            }
        }

        class AttratorBuilder {
            var Description: String? = null
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
            constructor(config: (AttratorBuilder) -> Unit) {
                config(this)
            }

            fun postEdit(config: (AttratorBuilder) -> Unit): AttratorBuilder {
                config(this)
                return this
            }

            override fun toString() = this.toJson
        }

        class FlagBuilder(config: FlagBuilder.() -> Unit = {}) {
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
         * - mock${mockType}_$name
         *
         * Attributes are:
         * - Key Suffix: False
         * - Regex: False
         * - Style: String
         */
        fun header_Item(mockType: String = "", name: String): String {
            return AttratorBuilder {
                it.Key_Format = "mock${mockType}_$name"
                it.hasKeySuffix = false
                it.Data_Format { fmt ->
                    fmt.Regex = false
                    fmt.Style = "String"
                }
            }.toString()
        }

        fun header_attrItem(
            mockType: String,
            Key: String,
            edit: (AttratorBuilder) -> Unit
        ): String {
            return AttratorBuilder {
                it.Count = ItemCounts.Many
                it.Key_Format = "mock${mockType}Filter_$Key"
            }.postEdit(edit).toString()
        }

        inline fun header_flags(
            mockType: String,
            Key: String,
            edit: (FlagBuilder) -> Unit = {}
        ): String {
            val builder = FlagBuilder {
                this.Key = Key
                this.Format = "$prefix$mockType$Key".decapitalize()
            }
            edit(builder)
            return builder.toJson
        }
    }

    fun build(create: CreateTypes): String {
        val createType = create.name
        val sb = StringBuilder()
        sb.append(preAppend = "{", postAppend = "}") {
            append(preAppend = "\"Headers\":{", postAppend = "}") {
                append(preAppend = "\"Name\":", postAppend = ",") {
                    TemplateItems.header_Item(createType, "name")
                }

                if (create == CreateTypes.Tape)
                    append(preAppend = "\"Url\":", postAppend = ",") {
                        TemplateItems.header_Item(createType, "Url")
                    }

                append(preAppend = "\"Attractors\":{", postAppend = "},") {
                    append(preAppend = "\"Path\":", postAppend = ",") {
                        TemplateItems.header_attrItem(createType, "Path") {
                            it.Count = TemplateItems.ItemCounts.One
                            it.hasKeySuffix = false
                            it.Data_Format { fmt ->
                                fmt.Regex = true
                                fmt.Style = "String"
                            }
                        }
                    }

                    append(preAppend = "\"Query\":", postAppend = ",") {
                        TemplateItems.header_attrItem(createType, "Query") {
                            it.Required = false
                            it.Data_Format { fmt ->
                                fmt.Regex = true
                                fmt.Style_A = "Field=Value"
                                fmt.Style_B = "Field1=Value1&Field2=Value2"
                                fmt.hasAllowAny = true
                            }
                        }
                    }

                    append(preAppend = "\"Header\":", postAppend = ",") {
                        TemplateItems.header_attrItem(createType, "Header") {
                            it.Required = false
                            it.Data_Format { fmt ->
                                fmt.Regex = true
                                fmt.Style = "Key=Value"
                                fmt.hasAllowAny = true
                            }
                        }
                    }

                    append(preAppend = "\"Body\":") {
                        TemplateItems.header_attrItem(createType, "Body") {
                            it.Required = false
                            it.Data_Format { fmt ->
                                fmt.Regex = true
                                fmt.Style = "String"
                                fmt.hasAllowAny = true
                            }
                        }
                    }
                }

                append(preAppend = "\"Flags\":{", postAppend = "}") {
                    append("", "\"Save to File\":", ",") {
                        TemplateItems.header_flags("", "SaveToFile")
                    }

                    append("", "\"Live\":", ",") {
                        TemplateItems.header_flags(createType, "Live")
                    }

                    when (create) {
                        CreateTypes.Tape -> {
                            append("", "\"RecordByFilter\":") {
                                TemplateItems.header_flags(createType, "RecordNew")
                            }
                        }

                        CreateTypes.Chapter -> {
                            append("", "\"Enabled\":", ",") {
                                TemplateItems.header_flags(createType, "Enabled")
                            }

                            append("", "\"Usages\":", ",") {
                                TemplateItems.header_flags(createType, "Uses") {
                                    it.Type = TemplateItems.FlagTypes.Int
                                }
                            }

                            append("", "\"EnhancedBody\":") {
                                TemplateItems.header_flags(createType, "BodyPlus")
                            }
                        }
                    }
                }
            }

            if (create == CreateTypes.Chapter) {
                append(",")
                append(preAppend = "\"Body\":{", postAppend = "}") {
                    append(preAppend = "\"Standard\": {", postAppend = "},") {
                        """"Format": "Any""""
                    }
                    append(preAppend = "\"Enhanced\": {", postAppend = "}") {
                        """
                       "Format": "Json",
                        "Data": {
                            "Response": {
                                "Key": "Response",
                                "Data": "String"
                            },
                            "Sequence": {
                                "Output": {
                                    "Key": "Sequences",
                                    "Data": "Json"
                                }
                            }
                        }
                       """
                    }
                }
            }
        }

        return sb.toString()
    }
}
