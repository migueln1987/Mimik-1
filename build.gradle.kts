val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val fuel_version: String by project
val ktlint by configurations.creating

plugins {
    application
    kotlin("jvm") version "1.3.50"
}

group = "com.fiserv.mimik"
version = "0.0.1"

application {
    mainClassName = "$group.ApplicationKt"
    //"io.ktor.server.netty.EngineMain"
}

repositories {
    mavenLocal()
    maven("http://jcenter.bintray.com")
    maven("https://kotlin.bintray.com/ktor")
}

dependencies {
    implementation(kotlin("stdlib-jdk8", kotlin_version))
    implementation("io.ktor:ktor-server-netty:$ktor_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")

    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-html-builder:$ktor_version")
    implementation("io.ktor:ktor-locations:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")

    implementation("com.airbnb.okreplay:okreplay:1.4.0")
    implementation("com.beust:klaxon:5.0.1")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("io.mockk:mockk:1.9.3")

    ktlint("com.github.shyiko:ktlint:0.29.0")
}

kotlin {
    sourceSets["main"].kotlin.srcDirs("src")
    sourceSets["test"].kotlin.srcDirs("test")
}

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

tasks {
    register<JavaExec>("ktlint") {
        group = "verification"
        description = "Check Kotlin code style."
        classpath = ktlint
        main = "com.github.shyiko.ktlint.Main"
        args("--android", "src/**/*.kt", "--verbose")
    }

    named<Task>("check") {
        dependsOn(ktlint)
    }

    register<JavaExec>("ktlintFormat") {
        group = "formatting"
        description = "Fix Kotlin code style deviations."
        classpath = ktlint
        main = "com.github.shyiko.ktlint.Main"
        args("--android", "-F", "src/**/*.kt")
    }
}
