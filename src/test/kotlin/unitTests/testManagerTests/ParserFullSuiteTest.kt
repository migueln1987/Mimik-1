package unitTests.testManagerTests

import helpers.ensurePrefix
import helpers.isNotNull
import helpers.parser.P4Action
import helpers.parser.Parser_v4
import helpers.toJson
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kolor.blue
import kolor.cyan
import kolor.green
import kolor.yellow
import testingManager.BoundChapterItem
import org.junit.Assert

class ParserFullSuiteTest {

    /**
     * Broad list of command combinations and expected results
     *
     * returns List<Input commands, expected output env>
     */
    private val mockSuiteCommands: List<MockSet>
        get() {
            val runTests: MutableList<MockSet> = mutableListOf()

            // 0. Baseline
            arrayOf(
                // expect no changes from input env to result env
                listOf("").toMockSet("Baseline", true)
            )
                .also { runTests.addAll(it) }

            // 1. Requests
            arrayOf(
                // 1.1 Heads
                // 1.1.1 Conditionals
                listOf("?request:head").toMockSet("1.1.1.a", true) {
                    it.activeData.request.loadDefaultHeaders()
                },
                listOf("?request:head:{a1}").toMockSet("1.1.1.b", true) {
                    it.activeData.request.loadDefaultHeaders()
                },
                listOf("?request:head[a1]").toMockSet("1.1.1.c", true) {
                    it.activeData.request.loadDefaultHeaders()
                },
                listOf("?request:head[a1]:{a+1}").toMockSet("1.1.1.d", true) {
                    it.activeData.request.loadDefaultHeaders()
                },

                // 1.1.2 Actions
                listOf("request:head->&result").toMockSet("1.1.2.a") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars["result"] = "a2"
                },
                listOf("request:head:{a\\d}->&result").toMockSet("1.1.2.b") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars["result"] = "a2"
                },
                listOf("request:head[a1]->&result").toMockSet("1.1.2.c") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars["result"] = "aaa1"
                },
                listOf("request:head[a1]:{a+}->&result").toMockSet("1.1.2.d") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars["result"] = "aaa"
                },

                // 1.2 Body
                // 1.2.0 Body has data
                listOf("request:body:{\\w+.+\\d}->&result@").toMockSet("1.2.0") {
                    it.activeData.request.loadDefaultBody()
                    it.expectedData.chapVars["result@"] = "true"
                },

                // 1.2.1 Conditionals
                listOf("?request:body").toMockSet("1.2.1.a", true) {
                    it.activeData.request.loadDefaultBody()
                },
                listOf("?request:body:{code.+\\d+}").toMockSet("1.2.1.b", true) {
                    it.activeData.request.loadDefaultBody()
                },

                // 1.2.2 Actions
                listOf("request:body->&result").toMockSet("1.2.2.a") {
                    it.activeData.request.loadDefaultBody()
                    it.expectedData.chapVars["result"] = it.activeData.request.body.orEmpty()
                },
                listOf("request:body:{code.+?(\\d+)}->&result").toMockSet("1.2.2.b") {
                    it.activeData.request.loadDefaultBody()
                    it.expectedData.chapVars["result"] = "14"
                }
            )
                .also { runTests.addAll(it) }

            // 2. Response
            arrayOf(
                // 2.1 Heads
                // 2.1.1 Conditionals
                listOf("?response:head").toMockSet("2.1.1.a", true) {
                    it.activeData.response.loadDefaultHeaders()
                },
                listOf("?response:head:{b\\d}").toMockSet("2.1.1.b", true) {
                    it.activeData.response.loadDefaultHeaders()
                },
                listOf("?response:head[b1]").toMockSet("2.1.1.c", true) {
                    it.activeData.response.loadDefaultHeaders()
                },
                listOf("?response:head[b1]:{b+\\d}").toMockSet("2.1.1.d", true) {
                    it.activeData.response.loadDefaultHeaders()
                },

                // 2.1.2 Actions
                // 2.1.2.A to Var
                listOf("response:head->&result").toMockSet("2.1.2.A.a") {
                    it.activeData.response.loadDefaultHeaders()
                    it.expectedData.chapVars["result"] = "b1"
                },
                listOf("response:head:{b\\d}->&result").toMockSet("2.1.2.A.b") {
                    it.activeData.response.loadDefaultHeaders()
                    it.expectedData.chapVars["result"] = "b1"
                },
                listOf("response:head[b1]->&result").toMockSet("2.1.2.A.c") {
                    it.activeData.response.loadDefaultHeaders()
                    it.expectedData.chapVars["result"] = "bbb1"
                },
                listOf("response:head[b1]:{b+}->&result").toMockSet("2.1.2.A.d") {
                    it.activeData.response.loadDefaultHeaders()
                    it.expectedData.chapVars["result"] = "bbb"
                },

                // 2.1.2.B to Self
                listOf("response:head[b1]->{dd}").toMockSet("2.1.2.B.a") {
                    it.activeData.response.loadDefaultHeaders()
                    it.expectedData.response.headers_raw["b1"] = listOf("dd")
                },
                listOf("response:head[b1]:{b+}->{dd}").toMockSet("2.1.2.B.b") {
                    it.activeData.response.loadDefaultHeaders()
                    it.expectedData.response.headers_raw["b1"] = listOf("dd")
                },
                listOf("response:head:{[a-z]\\d}->{d1}").toMockSet("2.1.2.B.c") {
                    it.activeData.response.loadDefaultHeaders()
                    it.expectedData.response.headers_raw["d1"] = listOf("bbb1", "bbb2")
                },

                // 2.2 Body
                // 2.2.0 Body has data
                listOf("response:body:{\\w+.+\\d}->&result@").toMockSet("2.2.0") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars["result@"] = "true"
                },

                // 2.2.1 Conditionals
                listOf("?response:body").toMockSet("2.2.1.a", true) {
                    it.activeData.response.loadDefaultBody()
                },
                listOf("?response:body:{test: \\d+}").toMockSet("2.2.1.b", true) {
                    it.activeData.response.loadDefaultBody()
                },

                // 2.2.2 Actions
                // 2.2.2.A to Var
                listOf("response:body->&result").toMockSet("2.2.2.A.a") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars["result"] = it.activeData.response.body.orEmpty()
                },
                listOf("response:body:{test: (\\d+)}->&result").toMockSet("2.2.2.A.b") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars["result"] = "22"
                },

                // 2.2.2.B to Self
                listOf("response:body->{body text}").toMockSet("2.2.2.B.a") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.response.body = "body text"
                },
                listOf("response:body:{(test: )(\\d+)}->{@{1}done}").toMockSet("2.2.2.B.b") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.response.body = """
                            test: done
                            final: 44
                        """.trimIndent()
                }
            )
                .also { runTests.addAll(it) }

            // 3. Variables
            arrayOf(
                // 3.1 Conditionals
                listOf("?&var").toMockSet("3.1.a", true) {
                    it.loadDefaultVars(it.activeData.chapVars)
                },
                listOf("?&var[wwA]").toMockSet("3.1.b", true) {
                    it.loadDefaultVars(it.activeData.chapVars)
                },
                listOf("?&var:{w+A}").toMockSet("3.1.c", true) {
                    it.loadDefaultVars(it.activeData.chapVars)
                },
                listOf("?&var[wwA]:{.+A}").toMockSet("3.1.d", true) {
                    it.loadDefaultVars(it.activeData.chapVars)
                },

                // 3.2 Actions
                // 3.2.1 to Var
                listOf("&var->%result").toMockSet("3.2.1.a") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.testVars["result"] = "wwA"
                },
                listOf("&var:{w+A}->%result").toMockSet("3.2.1.b") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.testVars["result"] = "wwA"
                },
                listOf("&var[wwA]->%result").toMockSet("3.2.1.c") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.testVars["result"] = "true"
                },
                listOf("&var[wwA]:{z+}->%result").toMockSet("3.2.1.d") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.testVars["result"] = "zz"
                },

                // 3.2.2 to Self
                // 3.2.2.1 Basic
                listOf("&var:{w+}->{dd}").toMockSet("3.2.2.1.a") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars.also { cv ->
                        cv["ddA"] = "zzA"
                        cv["ddB"] = "zzB"
                    }
                },
                listOf("&var[wwA]->{dd}").toMockSet("3.2.2.1.b") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars["wwA"] = "dd"
                },

                listOf("&var[wwA]:{zz}->{dd}").toMockSet("3.2.2.1.c") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars["wwA"] = "ddA"
                },

                // 3.2.2.2 adding a new key
                listOf("!&var[zzz]->{hh}").toMockSet("3.2.2.2.a") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars["zzz"] = "hh"
                },
                listOf("!&var[zzz]:{qq}->{hh}").toMockSet("3.2.2.2.b") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars["zzz"] = "hh"
                },

                // 3.2.2.3 Clearing the variable
                listOf(
                    // variable we will clear
                    "&var[testA]->{123}",
                    // proof the variables are being set
                    "&var[testB]->{456}",
                    // clear the variable
                    "&var[testA]:{.+}->{}"
                ).toMockSet("3.2.2.3.a") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars["testB"] = "456"
                }
            )
                .also { runTests.addAll(it) }

            // 4. Uses
            arrayOf(
                // 4.1 Conditionals
                // 4.1.1 Basic maths
                listOf("?use:{3}").toMockSet("4.1.1.a", true) {
                    it.activeData.use = 3
//                    it.envChapterUse = 3
                },
                listOf("?use:{>3}").toMockSet("4.1.1.b", true) {
                    it.activeData.use = 5
                },
                listOf("?use:{>=3}").toMockSet("4.1.1.c", true) {
                    it.activeData.use = 5
                },
                listOf("?use:{<3}").toMockSet("4.1.1.d", true) {
                    it.activeData.use = 2
                },
                listOf("?use:{3..4}").toMockSet("4.1.1.e", true) {
                    it.activeData.use = 3
                },
                listOf("?use:{3,5..6,9}").toMockSet("4.1.1.f", true) {
                    it.activeData.use = 6
                },

                // 4.1.2 Accessing "other"'s use info
                // 4.1.2.1 Testing is another chapter exists
                // 4.1.2.1.A Pass
                listOf("?use[other]").toMockSet("4.1.2.1.A", true) {
                    it.activeData.useChapters["other"] = 3
                },

                // 4.1.2.1.B Fail
                listOf("!use[something]").toMockSet("4.1.2.1.B", true) {
                    it.activeData.useChapters["none"] = 3
                },

                // 4.1.2.2 Testing the value of another chapter
                // 4.1.2.2.A Pass
                listOf("?use[other]:{3}").toMockSet("4.1.2.2.A", true) {
                    it.activeData.useChapters["other"] = 3
                },

                // 4.1.2.2.B Fail
                listOf("!use[other]:{5}").toMockSet("4.1.2.2.B", true) {
                    it.activeData.useChapters["other"] = 3
                },

                // 4.2 Actions
                // 4.2.1 to Var
                // 4.2.1.A Pass, save to var
                listOf("use:{3,5..6,9}->&result").toMockSet("4.2.1.A") {
                    it.activeData.use = 9
                    it.expectedData.chapVars["result"] = "9"
                },

                // 4.2.1.B Fail, use default var
                listOf(
                    "use:{>6}->&result", // '2' is not GT '6', answer 'false' is saved to result
                    "&var[result]:{false}->{}", // empty strings are seen as "no data"
                    "&var[test]->{@{&result|'none'}}"
                ).toMockSet("4.2.1.B") {
                    it.activeData.use = 2
                    it.expectedData.chapVars["test"] = "none"
                },

                // 4.2.2 to Self
                // 4.2.2.1 set the value only
                // 4.2.2.1.A Pass
                listOf("use->{8}").toMockSet("4.2.2.1.A") {
                    it.activeData.use = 4
                    it.expectedData.use = 8
                },
                // 4.2.2.1.B Fail
                listOf("use->{w}").toMockSet("4.2.2.1.B") {
                    it.activeData.use = 4
                    it.expectedData.use = 4
                },

                // 4.2.2.2 Ensure range, then set
                // 4.2.2.2.A Pass
                listOf("use:{3,5..6,9}->{2}").toMockSet("4.2.2.2.A") {
                    it.activeData.use = 5
                    it.expectedData.use = 2
                },

                // 4.2.2.2.B Fail
                listOf("use:{3,5..6,9}->{w}").toMockSet("4.2.2.2.B") {
                    it.activeData.use = 5
                    it.expectedData.use = 5
                },

                // 4.2.3 to Other
                // 4.2.3.1 set the value only
                // 4.2.3.1.A Pass
                listOf("use[other]->{8}").toMockSet("4.2.3.1.A") {
                    it.activeData.useChapters["other"] = 4
                    it.expectedData.useChapters["other"] = 8
                },
                // 4.2.3.1.B Fail
                listOf("use->{w}").toMockSet("4.2.3.1.B") {
                    it.activeData.useChapters["other"] = 4
                    it.expectedData.useChapters["other"] = 4
                },

                // 4.2.3.2 Ensure range, then set
                // 4.2.3.2.A Pass
                listOf("use[other]:{3,5..6,9}->{2}").toMockSet("4.2.3.2.A", true) {
                    it.activeData.useChapters["other"] = 5
                    it.expectedData.useChapters["other"] = 2
                },

                // 4.2.3.B Fail
                listOf("use[other]:{3,5..6,9}->{w}").toMockSet("4.2.3.2.B", true) {
                    it.activeData.useChapters["other"] = 5
                    it.expectedData.useChapters["other"] = 5
                }
            )
                .also { runTests.addAll(it) }

            // 5. Conditional Types
            arrayOf(
                // 5.1 Require true
                // 5.1.A & continue
                listOf(
                    // collect the value to scoped
                    "?response:body:{test: (\\d+)}->hold",
                    // export variable for assert testing
                    "var[hold]:{\\d+}->&result"
                ).toMockSet("5.1.A") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars["result"] = "22"
                },

                // 5.1.B no continue
                listOf(
                    "&var[result]->{4}",
                    // true: return matched result, false: return "false"
                    "?response:body:{other: (\\d+)}->hold",
                    "var[hold]:{\\d+}->&result"
                ).toMockSet("5.1.B") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars["result"] = "4"
                },

                // 5.2 Require false
                // 5.2.A & continue
                listOf(
                    // no match (as requested),'true' -> hold
                    "!response:body:{other: (\\d+)}->hold",
                    // if we saved to hold, then export it
                    "?var[hold]->&result"
                ).toMockSet("5.2.A") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars["result"] = "true"
                },

                // 5.2.B no continue
                listOf(
                    "&var[result]->{4}",
                    // source will passed, which we don't want
                    "!response:body:{test: (\\d+)}->FTest",
                    // `hold` will have no data, so this will fail. Thus `result` will not be set
                    "?var[FTest]->&result"
                ).toMockSet("5.2.B") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars["result"] = "4"
                },

                // 5.3 Optional Require true
                // 5.3.A pass, continue
                listOf(
                    "&var[FTest]->{none}",
                    // We expect this to pass; `FTest` will be set
                    "~?response:body:{test: (\\d+)}->&FTest",
                    // and this should still run
                    "&var[confirm]->{true}"
                ).toMockSet("5.3.A") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars.also { cv ->
                        cv["FTest"] = "22"
                        cv["confirm"] = "true"
                    }
                },

                // 5.3.B fail, continue
                listOf(
                    // We expect this to fail, and `FTest` NOT be set
                    "~?response:body:{none: (\\d+)}->FTest",
                    // but this should still run
                    "&var[confirm]->{true}"
                ).toMockSet("5.3.B") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars["confirm"] = "true"
                },

                // 5.4 Optional Require false
                // 5.4.A pass, continue
                listOf(
                    // setup `FTest`
                    "&var[FTest]->{something}",
                    // expect this to pass;
                    // but since it's a `not`, the source result will be the data
                    "~!response:body:{none: (\\d+)}->&FTest",
                    // and this should still run
                    "&var[confirm]->{true}"
                ).toMockSet("5.4.A") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars.also { cv ->
                        cv["FTest"] = "false"
                        cv["confirm"] = "true"
                    }
                },

                // 5.4.B fail, continue
                listOf(
                    "~!response:body:{test: (\\d+)}->FTest",
                    "&var[confirm]->{true}"
                ).toMockSet("5.4.B") {
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.chapVars["confirm"] = "true"
                },

                // 5.5 Dependent optionals
                // 5.5.A Parent passes, dependent runs, continue
                listOf(
                    "&var[test]->{none}",
                    // this must pass (which it will)
                    "~?request:body:{code.+?(\\d+)}->hold",
                    // to trigger this step
                    "~&var[test]->{@{hold}}",
                    // this will always run
                    "&var[extra]->{true}"
                ).toMockSet("5.5.A") {
                    it.activeData.request.loadDefaultBody()
                    it.expectedData.chapVars.also { cv ->
                        cv["test"] = "14"
                        cv["extra"] = "true"
                    }
                },

                // 5.5.B Parent fails, dependent does not run, continue
                listOf(
                    // expect this to fail
                    "~?request:body:{none.+?(\\d+)}->hold",
                    // thus this won't run
                    "~var[test]->{@{hold}}",
                    // this will always run
                    "&var[extra]->{true}"
                ).toMockSet("5.5.B") {
                    it.activeData.request.loadDefaultBody()
                    it.expectedData.chapVars["extra"] = "true"
                }
            )
                .also { runTests.addAll(it) }

            // 6. Data manipulation
            arrayOf(
                // 6.1 Data collection to output
                listOf(
                    // grab "code" data
                    "request:body:{code.+?(\\d+)}->hold1",
                    // grab "other" data
                    "request:body:{other.+?(\\d+)}->hold2",
                    // ensure we have both items of data
                    "?var[hold1]",
                    "?var[hold2]",
                    // Create a local var which we do var processing on
                    "var[hold3]->{_@{hold1}@{hold2}}",
                    // use the new var, plus add extra chars
                    "response:body:{(test.+?)(\\d+)}->{@{1}+@{hold3}+}"
                ).toMockSet("6.1.A") {
                    it.activeData.request.loadDefaultBody()
                    it.activeData.response.loadDefaultBody()
                    it.expectedData.response.body = """
                        test: +_1412+
                        final: 44
                    """.trimIndent()
                }
            )
                .also { runTests.addAll(it) }

            // 7. Result variable flags
            arrayOf(
                // 7.1 Exists
                // 7.1.a Request/ Response
                listOf("request:head->&result?").toMockSet("7.1.a") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars["result?"] = "true"
                },
                // 7.1.b Variable
                listOf("&var->&result?").toMockSet("7.1.b") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars["result?"] = "true"
                },

                // 7.2 Count
                // 7.2.a Request/ Response
                listOf("request:head->&result#").toMockSet("7.2.a") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars["result#"] = "4"
                },
                // 7.2.b Variable
                listOf("&var->&result#").toMockSet("7.2.b") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars["result#"] = "3"
                },

                // 7.3 Conditional Result (of the source)
                // 7.3.1.a Request/ Response
                listOf("request:head[a1]->&result@").toMockSet("7.3.1.a") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars.also { cv ->
                        cv["result"] = "aaa1"
                        cv["result@"] = "true"
                    }
                },
                // 7.3.1.b Variable
                listOf("&var->&result_#").toMockSet("7.3.1.b") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars.also { cv ->
                        cv["result_0"] = "wwA"
                        cv["result_1"] = "wwB"
                        cv["result_2"] = "qCC"
                    }
                },

                // 7.4 Spread
                // 7.4.1 All
                // 7.4.1.a Request/ Response
                listOf("request:head->&result_#").toMockSet("7.4.1.a") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars.also { cv ->
                        cv["result_0"] = "a2"
                        cv["result_1"] = "a1"
                        cv["result_2"] = "aa"
                        cv["result_3"] = "22"
                    }
                },
                // 7.4.1.b Variable
                listOf("&var->&result_#").toMockSet("7.4.1.b") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars.also { cv ->
                        cv["result_0"] = "wwA"
                        cv["result_1"] = "wwB"
                        cv["result_2"] = "qCC"
                    }
                },

                // 7.4.2 Index
                // 7.4.2.a Request/ Response
                listOf("request:head->&result").toMockSet("7.4.2.a") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars["result"] = "a2"
                },
                // 7.4.2.b Variable
                listOf("&var->&result").toMockSet("7.4.2.B") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars["result"] = "wwA"
                },

                // 7.4.3 Last
                // 7.4.3.a Request/ Response
                listOf("request:head->&result_?").toMockSet("7.4.3.a") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars["result_?"] = "22"
                },
                // 7.4.3.b Variable
                listOf("&var->&result_?").toMockSet("7.4.3.b") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars["result_?"] = "qCC"
                },

                // 7.4.4 Out of bounds
                // 7.4.4.a Request/ Response
                listOf(
                    "request:head->&result_#99",
                    "!&var[result_99]->&lastIdx"
                ).toMockSet("7.4.4.a") {
                    it.activeData.request.loadDefaultHeaders()
                    it.expectedData.chapVars.also { cv ->
                        cv["result#"] = "4"
                        cv["result_?"] = "22"
                        cv["lastIdx"] = "false"
                    }
                },
                // 7.4.4.b Variable
                listOf(
                    "&var->&result_#99",
                    "!&var[result_99]->&lastIdx"
                ).toMockSet("7.4.4.b") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.chapVars.also { cv ->
                        cv["result#"] = "3"
                        cv["result_?"] = "qCC"
                        cv["lastIdx"] = "false"
                    }
                },

                // 7.5 Variable hierarchy search
                // 7.5.a Seq and up
                listOf(
                    "var[var_s]->{4}",
                    "&var[var_c]->{8}",
                    "%var[var_t]->{12}",
                    "~?^var[var_s]->%found_s",
                    "~?^var[var_c]->%found_c",
                    "~?^var[var_t]->%found_t"
                ).toMockSet("7.5.a") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.testVars.also { tv ->
                        tv["found_s"] = "true"
                        tv["found_c"] = "true"
                        tv["found_t"] = "true"
                    }
                },

                // 7.5.b Chap and up
                listOf(
                    "var[var_s]->{4}",
                    "&var[var_c]->{8}",
                    "%var[var_t]->{12}",
                    "~!&^var[var_s]->%notfound_s",
                    "~?&^var[var_c]->%found_c",
                    "~?&^var[var_t]->%found_t"
                ).toMockSet("7.5.b") {
                    it.loadDefaultVars(it.activeData.chapVars)
                    it.expectedData.testVars.also { tv ->
                        tv["notfound_s"] = "false"
                        tv["found_c"] = "true"
                        tv["found_t"] = "true"
                    }
                }

            ).also { runTests.addAll(it) }

            // X. Misc
            arrayOf(
                // including "}->" in the "save to" and "source match"
                listOf(
                    "response:body:{test: 22}->{qqq}->c}",
                    "response:body:{qqq}\\->c}->&result"
                ).toMockSet("X.A") {
//                    it.boundVars = (envBoundVars + ("result" to "qqq}->c")).toMutableMap()
                }
            ).also { runTests.addAll(it) }

            return runTests
        }

    // @Test
    fun subTestSuite() {
        val tests = mockSuiteCommands.filter {
            it.name.startsWith("7.5.b")
        }
        suiteRunner(tests)
    }

    // @Test
    fun fullTestSuite() {
        suiteRunner(mockSuiteCommands)
    }

    private fun suiteRunner(toTest: List<MockSet>) {
        val testObj = P4Action()
        var testsRun = 0

        val testMax = toTest.size

        println("Running full-suit test: $testMax items".cyan())
        println("=".repeat(30))

        toTest.forEach { (setName, commands, suiteEnv) ->
            val sb = StringBuilder()

            val testStr = "Test #%d%s".format(
                testsRun,
                if (setName.isEmpty()) "" else ": $setName"
            )
            sb.appendLine("-".repeat(testStr.length + 10))
            sb.appendLine(testStr.yellow())
            sb.appendLine("Testing commands:")
            val printCmdLines = commands.joinToString("\n") {
                (if (it.isEmpty()) "{empty string}" else it)
                    .ensurePrefix("- ")
                    .cyan()
            }
            sb.appendLine(printCmdLines)
            println(sb.toString())

            val inputData = suiteEnv.activeData
            val mockChaps = inputData.useChapters.map { (chapter, use) ->
                Pair(
                    chapter,
                    BoundChapterItem().also { it.stateUse = use }
                )
            }.toMap().toMutableMap()

            testObj.setup { setup ->
                setup.testBounds = mockk {
                    val dataSlot = slot<String>()
                    every { boundData[capture(dataSlot)] } answers {
                        mockChaps[dataSlot.captured]
                    }
                    every { boundData } returns mockChaps

                    every { scopeVars } returns inputData.testVars
                }

                setup.chapBounds = BoundChapterItem {
                    it.stateUse = inputData.use
                    it.scopeVars = inputData.chapVars
                }

                inputData.request.apply {
                    setup.in_headers = headers
                    setup.in_body = body.orEmpty()
                }

                inputData.response.apply {
                    setup.out_headers = headers
                    setup.out_body = body.orEmpty()
                }
            }

            // Process inputs for results
            val steps = commands.asSequence()
                .map { it to Parser_v4.parseToCommand(it) }
                .onEach { (input, parsed) ->
                    fun errorMsg() = "$testStr\nProcessed command does not match input" +
                            "\nInput: $input" +
                            "\nParsed: $parsed\n"

                    if (parsed.toString() != "Invalid")
                        Assert.assertEquals(
                            errorMsg(),
                            input,
                            parsed.toString()
                        )
                }
                .map { it.second }
                .toList()

            testObj.processCommands(steps)

            // Test params with expected outputs
            val variableTests = listOf(
                Triple("Sequence", suiteEnv.expectedData.seqVars, testObj.scopeVars),
                Triple("Chapter", suiteEnv.expectedData.chapVars, suiteEnv.activeData.chapVars),
                Triple("Test Bounds", suiteEnv.expectedData.testVars, suiteEnv.activeData.testVars)
            )

            variableTests.forEach { (name, expect, actual) ->
                expect.forEach { (key, value) ->
                    fun errorMsg_1() = ("%s\n== %s\n" +
                            "Missing:\n [%s] = %s" +
                            "\n\nResult Vars:\n%s\n").format(
                        testStr,
                        name,
                        key, value,
                        actual.toJson
                    )

                    Assert.assertTrue(
                        errorMsg_1(),
                        actual.containsKey(key)
                    )
                    fun errorMsg_2() = "$testStr\nResult's $name Variables don't match" +
                            "\nExpected: [$key] = $value" +
                            "\nFound: [$key] = ${actual[key]}" +
                            "\n\nResult vars:\n${actual.toJson}\n\n"

                    Assert.assertEquals(
                        errorMsg_2(),
                        value, actual[key]
                    )
                }
                println("$name variables -> pass".green())

                val lingerKeys = actual.keys.minus(expect.keys)
                if (lingerKeys.isNotEmpty()) {
                    println("Lingering keys {$name}".blue())
                    lingerKeys.forEach { key ->
                        val kVal = actual[key]
                            .let {
                                if (it.isNullOrEmpty())
                                    "{Empty string}" else it
                            }
                        println("+ [$key] = $kVal".blue())
                    }
                    println()
                }
            }

            if (suiteEnv.expectedData.response.hasHeaders) {
                val resultHeaders = suiteEnv.activeData.response.headers_raw
                suiteEnv.expectedData.response.headers_raw.forEach { (t, u) ->
                    Assert.assertTrue(
                        "$testStr\nMissing Header key: $t",
                        resultHeaders.containsKey(t)
                    )

                    val rHeadVals = resultHeaders[t] ?: listOf()
                    fun errorMsg(expKey: String) = "$testStr\nMissing header value: {$t : $expKey}" +
                            "\n\n${resultHeaders.toJson}"

                    u.forEach {
                        Assert.assertTrue(
                            errorMsg(it),
                            rHeadVals.contains(it)
                        )
                    }
                }
                println("Headers -> pass".green())
            }

            if (suiteEnv.expectedData.response.body.isNotNull()) {
                suiteEnv.activeData.response.body = testObj.out_body
                Assert.assertEquals(
                    "$testStr\nResponse body does not match the expected body\n",
                    suiteEnv.expectedData.response.body,
                    suiteEnv.activeData.response.body
                )
                println("Body -> pass".green())
            }

            if (suiteEnv.activeData.use != -1 && suiteEnv.expectedData.use != -1) {
                testObj.chapBounds.stateUse?.also { suiteEnv.activeData.use = it }
                Assert.assertEquals(
                    "$testStr\nActual uses does not match expected uses",
                    suiteEnv.expectedData.use,
                    suiteEnv.activeData.use
                )
                println("Uses -> pass".green())
            }

            suiteEnv.expectedData.useChapters.forEach { (chapName, Use) ->
                val actualUse = testObj.testBounds.boundData[chapName]?.stateUse?.toLong()
                Assert.assertEquals(
                    "$testStr\nChapter [$chapName]; actual use is not the expected value",
                    Use.toLong(),
                    actualUse
                )
            }
            if (suiteEnv.expectedData.useChapters.isNotEmpty())
                println("Chapter uses -> pass".green())

            println("$testStr -> Success".green())
            testsRun++
        }
        println("Full test suite complete: $testsRun / $testMax".green())
    }
}
