plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "me.gigabyte"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(19)
}

application {
    mainClass.set("MainKt")
}