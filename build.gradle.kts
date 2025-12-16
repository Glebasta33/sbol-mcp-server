plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    kotlin("plugin.compose") version "2.2.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jetbrains.compose") version "1.7.0"
    application
}

group = "prototype"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

tasks.shadowJar {
    archiveFileName.set("my-app.jar")
}
val mcpVersion = "0.8.1"
val ktorVersion = "3.2.3"

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:0.6.0")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.slf4j:slf4j-nop:2.0.9")

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("prototype.MainKt")
}
