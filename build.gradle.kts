plugins {
    kotlin("jvm") version "2.4.20"
    `maven-publish`
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name = "trigg"
                description = "Spwn like kotlin DSL to make complex trigger stuff in Geometry Dash"

                developers {
                    developer {
                        id = "geming400"
                        name = "Geming400"
                    }
                }
            }

            groupId = group as String
            artifactId = "trigg"
            version = version

            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://github.com/gd-kt/trigg")
            name = "trigg"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}