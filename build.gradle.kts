@file:Suppress("UNUSED_VARIABLE")

val ktor_version = "1.3.2"
val fuel_version = "2.2.1"

version = "0.8.0"

plugins {
    idea
    // https://github.com/jlleitschuh/ktlint-gradle
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    kotlin("multiplatform") version embeddedKotlinVersion
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

ktlint {
    // Optionally configure plugin
    debug.set(true)
}

repositories {
    maven("https://jcenter.bintray.com")
    maven("https://kotlin.bintray.com/ktor")
    maven("https://kotlin.bintray.com/kotlin-js-wrappers")
    mavenCentral()
}

kotlin {
//    jvm { withJava() }
    jvm()
    js { browser {} }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))

                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
                implementation("io.ktor:ktor-client-cio:$ktor_version")
                implementation("io.ktor:ktor-client-okhttp:$ktor_version")

                implementation("ch.qos.logback:logback-classic:1.2.3")
                implementation("io.github.microutils:kotlin-logging:1.7.9")
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

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8", embeddedKotlinVersion))

                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-server-netty:$ktor_version")

                implementation("io.ktor:ktor-gson:$ktor_version")
                implementation("io.ktor:ktor-html-builder:$ktor_version")
                implementation("io.ktor:ktor-locations:$ktor_version")

                implementation("com.github.kittinunf.fuel:fuel:$fuel_version")
                implementation("com.airbnb.okreplay:okreplay:1.6.0")
                implementation("com.beust:klaxon:5.2")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))

                implementation("io.ktor:ktor-server-tests:$ktor_version")
                implementation("io.mockk:mockk:1.9.3")
            }
        }
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    create("run", JavaExec::class) {
        group = "application"
        main = "mimik.ApplicationKt"
        kotlin {
            val main = targets["jvm"].compilations["main"]
            dependsOn(main.compileAllTaskName)
            classpath(
                { main.output.allOutputs.files },
                { configurations["jvmRuntimeClasspath"] }
            )
        }
        // disable app icon on macOS
        systemProperty("java.awt.headless", "true")
    }
}
