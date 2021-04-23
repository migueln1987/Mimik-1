import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

object Versions {
    const val kotlin = "1.4.30"
    const val ktor = "1.5.1"
    const val fuel = "2.3.0"
    const val okReply = "1.6.0"
    const val kotlin_css = "1.0.0-pre.148-kotlin-1.4.21"
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
    jvm {
        withJava()
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
            }
        }
        tasks.withType<Jar> {
            doFirst {
                manifest { attributes["Main-Class"] = "mimik.ApplicationKt" }
                from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
            }
        }
    }

    js {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
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
                //    implementation("io.bit3:jsass:5.10.4")
                implementation("org.lesscss:lesscss:1.7.0.1.1")

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

        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

fun KotlinDependencyHandler.implementation(dependencyNotation: String, version: String): Dependency? =
    implementation("$dependencyNotation:$version")
