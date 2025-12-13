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
    // основной Kotlin MCP серверный SDK
    implementation("io.modelcontextprotocol:kotlin-sdk-server:$mcpVersion")
    
    // kotlinx.io для работы со STDIO
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.2")
    
    // Ktor streams для работы с InputStream (нужно для asInput())
    implementation("io.ktor:ktor-utils:3.0.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    // Используем Java 17 (глобальная версия на машине)
    jvmToolchain(17)
}

application {
    mainClass.set("prototype.MainKt")
}