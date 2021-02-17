object Versions {
    const val kotlin = "1.4.30"
    const val ktor = "1.5.1"
    const val fuel = "2.3.0"
    const val okReply = "1.6.0"
}

group = "mimik"
version = "0.8.0"

plugins {
    idea
    application
    kotlin("jvm") version "1.4.30"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

application {
    idea
    mainClassName = "mimik.ApplicationKt"
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
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
    implementation("io.ktor:ktor-server-netty", Versions.ktor)

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
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

kotlin.sourceSets {
    get("main").kotlin.srcDirs("src/main/kotlin")
    get("test").kotlin.srcDirs("src/test/kotlin")
}

sourceSets {
    get("main").resources.srcDirs("src/main/resources")
    get("test").resources.srcDirs("src/test/resources")
}

fun DependencyHandler.implementation(dependencyNotation: String, version: String = ""): Dependency? =
    add("implementation", "$dependencyNotation:$version")
