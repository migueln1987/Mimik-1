import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer

object Versions {
    const val kotlin = "1.6.0"
    const val ktor = "1.6.7"
    const val fuel = "2.3.1"
    const val okReply = "1.6.0"
    const val kotlin_css = "1.0.0-pre.279-kotlin-1.6.0"
    const val objectbox = "3.0.1"
    const val KVision = "5.6.1"
}

group = "mimik"
version = "2.x_0321"

buildscript {
    dependencies {
//        classpath("io.objectbox:objectbox-gradle-plugin:2.8.1")
    }
}

plugins {
    kotlin("plugin.serialization") version "1.6.0"
    kotlin("multiplatform") version "1.6.0"
    application
    idea
    war
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

apply {
//    plugin("io.objectbox")
}

application {
    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }
    mainClass.set("mimik.ApplicationKt")
}

war {
    webAppDirName = "src/jvmMain/webapp"
}

repositories {
    mavenCentral()
//    maven("https://kotlin.bintray.com/ktor")
//    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
    mavenLocal()
}

kotlin {
    jvm {
        withJava()
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xjsr305=strict",
                    "-Xopt-in=kotlin.RequiresOptIn"
//                    "-XXLanguage:+InlineClasses"
                )
            }
        }
    }

    js(IR) {
        browser {
//            commonWebpackConfig {
//                devtool = org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool.EVAL_SOURCE_MAP
//            }
            runTask {
                outputFileName = "mimik.js"
                sourceMaps = true
                devServer = DevServer(
                    open = false,
                    port = 4884,
                    proxy = mutableMapOf(
                        "/mk/*" to "http://localhost:4884",
                        "/mkws/*" to mapOf("target" to "ws://localhost:4884", "ws" to true)
                    )
//                    static = mutableListOf("$buildDir/processedResources/js/main")
                )
            }
            webpackTask {
                outputFileName = "mimik.js"
            }
            binaries.executable()
        }
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("io.ktor:ktor-client-core", Versions.ktor)
                api("io.kvision:kvision-server-ktor", Versions.KVision)
            }
            kotlin.srcDir("build/generated-src/common")
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("reflect"))

//                implementation("ch.qos.logback:logback-classic:1.2.3")
//                implementation("io.github.microutils:kotlin-logging:1.7.6")

                implementation("io.ktor:ktor-server-core", Versions.ktor)
                //    implementation("io.ktor:ktor-server-netty", Versions.ktor)
                implementation("io.ktor:ktor-server-jetty", Versions.ktor)
                implementation("io.ktor:ktor-server-servlet", Versions.ktor)

                implementation("io.ktor:ktor-client-core-jvm", Versions.ktor)
                implementation("io.ktor:ktor-client-cio", Versions.ktor)
                implementation("io.ktor:ktor-client-okhttp", Versions.ktor)

                implementation("io.ktor:ktor-gson", Versions.ktor)
                implementation("io.ktor:ktor-html-builder", Versions.ktor)
                implementation("io.ktor:ktor-locations", Versions.ktor)
                implementation("org.jetbrains.kotlin-wrappers:kotlin-css", Versions.kotlin_css)
                implementation("de.inetsoftware:jlessc:1.10")

                implementation("com.github.kittinunf.fuel:fuel", Versions.fuel)
                implementation("com.airbnb.okreplay:okreplay", Versions.okReply)
                implementation("com.beust:klaxon:5.4")
                implementation("org.tukaani:xz:1.8")

                //    implementation("io.objectbox:objectbox-kotlin", Versions.objectbox)
                //    implementation("io.objectbox:objectbox-linux", Versions.objectbox)
                //    implementation("io.objectbox:objectbox-macos", Versions.objectbox)
                //    implementation("io.objectbox:objectbox-windows", Versions.objectbox)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("io.ktor:ktor-server-tests:${Versions.ktor}")
                implementation("io.mockk:mockk:1.10.0")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))

//                implementation("io.kvision:kvision", Versions.KVision)
//                implementation("io.kvision:kvision-bootstrap", Versions.KVision)
//                implementation("io.kvision:kvision-bootstrap-css", Versions.KVision)
//                implementation("io.kvision:kvision-bootstrap-select", Versions.KVision)
//                implementation("io.kvision:kvision-chart", Versions.KVision)
                implementation(npm("react-awesome-button", "*"))
                implementation(npm("prop-types", "*"))
                implementation("io.kvision:kvision", Versions.KVision)
                implementation("io.kvision:kvision-bootstrap", Versions.KVision)
                implementation("io.kvision:kvision-bootstrap-css", Versions.KVision)
                implementation("io.kvision:kvision-bootstrap-datetime", Versions.KVision)
                implementation("io.kvision:kvision-bootstrap-select", Versions.KVision)
                implementation("io.kvision:kvision-bootstrap-spinner", Versions.KVision)
                implementation("io.kvision:kvision-bootstrap-upload", Versions.KVision)
                implementation("io.kvision:kvision-bootstrap-dialog", Versions.KVision)
                implementation("io.kvision:kvision-bootstrap-typeahead", Versions.KVision)
                implementation("io.kvision:kvision-fontawesome", Versions.KVision)
                implementation("io.kvision:kvision-i18n", Versions.KVision)
                implementation("io.kvision:kvision-richtext", Versions.KVision)
                implementation("io.kvision:kvision-handlebars", Versions.KVision)
                implementation("io.kvision:kvision-datacontainer", Versions.KVision)
                implementation("io.kvision:kvision-chart", Versions.KVision)
                implementation("io.kvision:kvision-tabulator", Versions.KVision)
                implementation("io.kvision:kvision-pace", Versions.KVision)
                implementation("io.kvision:kvision-toast", Versions.KVision)
                implementation("io.kvision:kvision-react", Versions.KVision)
                implementation("io.kvision:kvision-routing-navigo", Versions.KVision)
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
                implementation("io.kvision:kvision-testutils", Versions.KVision)
            }
        }
    }
}

val webpackType: String
    get() = if (project.hasProperty("isProduction") && project.property("isProduction") == true)
        "jsBrowserProductionWebpack" else "jsBrowserDevelopmentWebpack"

afterEvaluate {
    tasks {
        withType<AbstractCopyTask> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        fun Jar.webpackRun() {
            val isWar = archiveExtension.get() == War.WAR_EXTENSION

            doFirst {
                manifest { attributes["Main-Class"] = application.mainClass }
                if (!isWar) {
                    from(configurations.runtimeClasspath.get().map {
                        if (it.isDirectory) it else zipTree(it)
                    })
                }
            }

            val webpackTask = getByName<KotlinWebpack>(webpackType)
            dependsOn(webpackTask) // make sure JS gets compiled first

            val copyTask: CopySpec.() -> Unit = {
                from(webpackTask.destinationDirectory) {
                    // copy compiled js to libs folder
                    include("*.js", "*.js.map")
                    if (isWar) into("classes/libs")
                    else into("libs")
                }

                from(webpackTask.destinationDirectory) {
                    // copy compiled js to libs folder
                    exclude("*.js", "*.js.map")
                    if (isWar) into("classes/libs")
                    else into("assets")
                }
            }

            (this as? War)?.webInf(copyTask) ?: copyTask()
        }

        // include JS artifacts in any JAR/WAR we generate
        withType<Jar> { webpackRun() }

        register<Copy>("copyJsBundleToKtor") {
            mustRunAfter(webpackType)
            dependsOn(webpackType)

            project.tasks.getByName<KotlinWebpack>(webpackType) {
                from(destinationDirectory) { include("*.*") }
            }

            into("$buildDir/processedResources/jvm/main/libs")
        }

        getByName<JavaExec>("run") {
            dependsOn("compileKotlinJvm", "copyJsBundleToKtor")
            group = "run"
            main = application.mainClass.get()
            classpath =
                configurations["jvmRuntimeClasspath"] + project.tasks["compileKotlinJvm"].outputs.files +
                        project.tasks["jvmProcessResources"].outputs.files
            workingDir = buildDir
        }
    }
}

distributions {
    main {
        contents {
//            from("$buildDir/libs") {
//                println("rename: ${rootProject.name}-jvm.* from [$buildDir/libs]")
//                rename("${rootProject.name}-jvm.*", rootProject.name)
//                into("lib")
//            }
        }
    }
}

fun KotlinDependencyHandler.implementation(dependencyNotation: String, version: String): Dependency? =
    implementation("$dependencyNotation:$version")

fun KotlinDependencyHandler.api(dependencyNotation: String, version: String): Dependency? =
    api("$dependencyNotation:$version")
