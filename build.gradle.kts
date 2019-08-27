val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val fuel_version: String by project
val ktlint by configurations.creating

plugins {
    application
    kotlin("jvm") version "1.3.41"
}

group = "com.fiserv.ktmimic"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenLocal()
//    jcenter()
    maven("http://jcenter.bintray.com")
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    compile("io.ktor:ktor-server-netty:$ktor_version")
    compile("ch.qos.logback:logback-classic:$logback_version")
    compile("io.ktor:ktor-server-core:$ktor_version")
    compile("io.ktor:ktor-html-builder:$ktor_version")
    compile("io.ktor:ktor-gson:$ktor_version")
    compile("io.ktor:ktor-client-core:$ktor_version")
    compile("io.ktor:ktor-client-core-jvm:$ktor_version")
    compile("io.ktor:ktor-client-cio:$ktor_version")
    compile("io.ktor:ktor-client-okhttp:$ktor_version")
    compile("com.airbnb.okreplay:okreplay:1.4.0")
    compile("com.beust:klaxon:5.0.1")
    testCompile("io.ktor:ktor-server-tests:$ktor_version")

    ktlint("com.github.shyiko:ktlint:0.29.0")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

tasks.register<JavaExec>("ktlint") {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = ktlint
    main = "com.github.shyiko.ktlint.Main"
    args("--android", "src/**/*.kt")
}

//tasks.named("check") {
//    dependsOn(ktlint)
//}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    main = "com.github.shyiko.ktlint.Main"
    args("--android", "-F", "src/**/*.kt")
}
