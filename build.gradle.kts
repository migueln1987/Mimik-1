val ktor_version = "1.3.0"
val fuel_version = "2.2.1"

version = "0.8.0"

plugins {
    idea
    application
    kotlin("jvm") version embeddedKotlinVersion
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
    implementation(kotlin("stdlib-jdk8", embeddedKotlinVersion))

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.7.6")

    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")

    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-html-builder:$ktor_version")
    implementation("io.ktor:ktor-locations:$ktor_version")

    implementation("com.github.kittinunf.fuel:fuel:$fuel_version")
    implementation("com.airbnb.okreplay:okreplay:1.6.0")
    implementation("com.beust:klaxon:5.2")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("io.mockk:mockk:1.10.0")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
