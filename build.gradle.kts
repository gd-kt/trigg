plugins {
    kotlin("jvm") version "2.4.20"
}

group = "fr.geming400.gddotkt.trigg"
version = project.property("version")!!

java {
    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    val gdDotKtVersion = project.property("gdDotKtVersion")

    testImplementation(kotlin("test"))
    implementation("com.github.gd-kt:gd.kt:$gdDotKtVersion")
    implementation(kotlin("reflect"))
}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}