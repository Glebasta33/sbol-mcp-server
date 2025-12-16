package prototype

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import prototype.core.config.AppConfig
import prototype.data.service.ContextServiceImpl
import prototype.data.service.FileServiceImpl
import prototype.presentation.tools.ToolRegistry

/**
 * MCP Server на основе Kotlin SDK
 * 
 * Предоставляет набор tools для работы с контекстом, файлами и утилитами
 * 
 * Архитектура: Clean Architecture с разделением на слои:
 * - presentation: MCP Tools
 * - domain: бизнес-логика и интерфейсы
 * - data: реализации сервисов
 * - core: общие утилиты и конфигурация
 * 
 * Сборка:
 * ./gradlew installDist
 * 
 * Запуск:
 * ./build/install/sbol-mcp-server/bin/sbol-mcp-server
 */
fun main() {
    val server: Server = createServer()
    val stdioServerTransport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        System.out.asSink().buffered()
    )
    runBlocking {
        val job = Job()
        server.onClose { job.complete() }
        server.connect(stdioServerTransport)
        job.join()
    }
}

/**
 * Создаёт и конфигурирует MCP Server
 */
fun createServer(): Server {
    val info = Implementation(
        AppConfig.SERVER_NAME,
        AppConfig.SERVER_VERSION
    )

    val options = ServerOptions(
        capabilities = ServerCapabilities(
            prompts = ServerCapabilities.Prompts(listChanged = true),
            tools = ServerCapabilities.Tools(true),
        )
    )

    val server = Server(info, options)

    // Инициализация сервисов (простая DI без фреймворка)
    val fileService = FileServiceImpl()
    val contextService = ContextServiceImpl(fileService)
    
    // Регистрация всех tools через ToolRegistry
    val toolRegistry = ToolRegistry(contextService)
    toolRegistry.registerAllTools(server)

    return server
}

