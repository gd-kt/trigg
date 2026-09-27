plugins {
    kotlin("jvm") version "2.4.20"
}

group = "fr.geming400.gddotkt.trigg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    val gdDotKtVersion = project.property("gdDotKtVersion")

    testImplementation(kotlin("test"))
    implementation("com.github.gd-kt:gd.kt:$gdDotKtVersion")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}