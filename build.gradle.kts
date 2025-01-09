plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.designpatterns"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.xerial:sqlite-jdbc:3.40.1.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}