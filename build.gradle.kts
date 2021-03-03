import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object Versions {
    const val kotlin = "1.4.30"
    const val ktor = "1.5.1"
    const val fuel = "2.3.0"
    const val okReply = "1.6.0"
}

group = "mimik"
version = "0.8.0"

plugins {
    application
    idea
    war
    kotlin("jvm") version "1.4.30"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("org.gretty") version "3.0.3"
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
    webAppDirName = "src/main/webapp"
}

gretty {
    contextPath = "/"
    logbackConfigFile = "resources/logback.xml"
}

repositories {
    maven("https://jcenter.bintray.com")
    maven("https://kotlin.bintray.com/ktor")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8", Versions.kotlin))

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.7.6")

    implementation("io.ktor:ktor-server-core", Versions.ktor)
//    implementation("io.ktor:ktor-server-netty", Versions.ktor)
    implementation("io.ktor:ktor-server-jetty", Versions.ktor)
    implementation("io.ktor:ktor-server-servlet", Versions.ktor)

    implementation("io.ktor:ktor-client-core", Versions.ktor)
    implementation("io.ktor:ktor-client-core-jvm", Versions.ktor)
    implementation("io.ktor:ktor-client-cio", Versions.ktor)
    implementation("io.ktor:ktor-client-okhttp", Versions.ktor)

    implementation("io.ktor:ktor-gson", Versions.ktor)
    implementation("io.ktor:ktor-html-builder", Versions.ktor)
    implementation("io.ktor:ktor-locations", Versions.ktor)

    implementation("com.github.kittinunf.fuel:fuel", Versions.fuel)
    implementation("com.airbnb.okreplay:okreplay", Versions.okReply)
    implementation("com.beust:klaxon:5.4")
    implementation("org.tukaani:xz:1.8")

    testImplementation("io.ktor:ktor-server-tests:${Versions.ktor}")
    testImplementation("io.mockk:mockk:1.10.0")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        }
    }
}

kotlin {
    experimental {
//        coroutines = Coroutines.ENABLE
    }
    sourceSets {
        main { kotlin.srcDirs("src/main/kotlin") }
        test { kotlin.srcDirs("src/test/kotlin") }
    }
}

sourceSets {
    main { resources.srcDirs("src/main/resources") }
    test { resources.srcDirs("src/test/resources") }
}

fun DependencyHandler.implementation(dependencyNotation: String, version: String): Dependency? =
    implementation("$dependencyNotation:$version")
