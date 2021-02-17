val ktor_version = "1.4.1"
val fuel_version = "2.3.0"

group = "mimik"
version = "0.8.0"

plugins {
    idea
    application
    kotlin("jvm") version "1.4.0"
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
    implementation(kotlin("stdlib-jdk8", "1.4.0"))

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
    implementation("com.beust:klaxon:5.4")
    implementation("org.tukaani:xz:1.8")

    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
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
