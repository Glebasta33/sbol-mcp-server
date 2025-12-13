plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "prototype"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val mcpVersion = "0.8.1"
dependencies {
    // основной Kotlin MCP серверный SDK (можно взять весь пакет kotlin-sdk или только server)
    implementation("io.modelcontextprotocol:kotlin-sdk-server:$mcpVersion")
    
    // kotlinx.io для работы со STDIO
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")
    
    // SLF4J implementation для логирования
    implementation("org.slf4j:slf4j-simple:2.0.9")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("prototype.MainKt")
}