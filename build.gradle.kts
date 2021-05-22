import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object Versions {
    const val kotlin = "1.4.30"
    const val ktor = "1.5.4"
    const val fuel = "2.3.0"
    const val okReply = "1.6.0"
    const val kotlin_css = "1.0.0-pre.148-kotlin-1.4.30"
    const val objectbox = "2.8.1"
}

group = "mimik"
version = "2.x_0321"

buildscript {
    dependencies {
//        classpath("io.objectbox:objectbox-gradle-plugin:2.8.1")
    }
}

plugins {
    kotlin("multiplatform") version "1.4.30"
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
    jcenter()
    maven("https://kotlin.bintray.com/ktor")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
    mavenCentral()
}

kotlin {
    jvm { withJava() }

    js(IR) {
        browser {
            binaries.executable()
            webpackTask {
                outputFileName = "mimik.js"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("io.ktor:ktor-client-core", Versions.ktor)
            }
        }

        val commonTest by getting

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8", Versions.kotlin))

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
                implementation("org.jetbrains:kotlin-css", Versions.kotlin_css)
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
                implementation("io.ktor:ktor-server-tests:${Versions.ktor}")
                implementation("io.mockk:mockk:1.10.0")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-XXLanguage:+InlineClasses"
            )
//            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        }
    }

    // include JS artifacts in any JAR/WAR we generate
    withType<Jar> {
        val isWar = archiveExtension.get() == War.WAR_EXTENSION
//        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        doFirst {
            manifest { attributes["Main-Class"] = application.mainClass }
            if (!isWar)
                from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        }

        var taskName = if (project.hasProperty("isProduction"))
            "jsBrowserProductionWebpack" else "jsBrowserDevelopmentWebpack"
//        taskName = "jsBrowserProductionWebpack"

        val webpackTask = getByName<KotlinWebpack>(taskName)
        dependsOn(webpackTask) // make sure JS gets compiled first

        val copyTask: CopySpec.() -> Unit = {
            val baseConventions = project.convention.plugins["base"] as BasePluginConvention?
            val distributionDir = project.buildDir.resolve(baseConventions?.distsDirName.orEmpty())

            // from(File(webpackTask.destinationDirectory, webpackTask.outputFileName)) {
            from(distributionDir) {
                // copy compiled js to libs folder
                include("*.js", "*.js.map")
                if (isWar) into("classes/libs")
                else into("libs")
            }
        }

        (this as? War)?.webInf(copyTask) ?: copyTask()
    }

    getByName<JavaExec>("run") {
        classpath(getByName<Jar>("jvmJar")) // so that the JS artifacts generated by `jvmJar` can be found and served
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
