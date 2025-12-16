package prototype

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import prototype.core.config.AppConfig
import prototype.data.service.ContextServiceImpl
import prototype.data.service.FileServiceImpl
import prototype.data.service.PlanFileWatcher
import prototype.data.service.PlanServiceImpl
import prototype.presentation.tools.ToolRegistry
import prototype.presentation.ui.launchTaskManagerApp

/**
 * MCP Server на основе Kotlin SDK
 * 
 * Предоставляет набор tools для работы с контекстом, файлами и утилитами
 * 
 * Архитектура: Clean Architecture с разделением на слои:
 * - presentation: MCP Tools и UI
 * - domain: бизнес-логика и интерфейсы
 * - data: реализации сервисов
 * - core: общие утилиты и конфигурация
 * 
 * Фичи:
 * - MCP tools для работы с контекстом и файлами
 * - Система управления задачами с поддержкой MD файлов
 * - Compose Desktop UI для визуализации задач
 * - Автоматическая синхронизация между MD файлами и UI
 *
 * Сборка:
 * ./gradlew installDist
 * 
 * Запуск:
 * ./build/install/sbol-mcp-server/bin/sbol-mcp-server
 *
 * Переменные окружения:
 * - LAUNCH_UI=true - автоматически запустить Task Manager UI при старте
 */
fun main() {
    runBlocking {
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.Default + job)

        // Создать сервисы
        val fileService = FileServiceImpl()
        val contextService = ContextServiceImpl(fileService)
        val planService = PlanServiceImpl(fileService)

        // Инициализировать и запустить FileWatcher для отслеживания изменений планов
        val fileWatcher = PlanFileWatcher(planService, coroutineScope)
        fileWatcher.start()
        println("PlanFileWatcher started and monitoring ${AppConfig.PLANS_BASE_PATH}")

        // Создать и настроить MCP сервер
        val server: Server = createServer(contextService, planService)
        val stdioServerTransport = StdioServerTransport(
            System.`in`.asSource().buffered(),
            System.out.asSink().buffered()
        )

        // Опционально запустить UI при старте (если установлена переменная окружения)
        val shouldLaunchUI = System.getenv("LAUNCH_UI")?.toBoolean() ?: false
        if (shouldLaunchUI) {
            coroutineScope.launch {
                println("Launching Task Manager UI...")
                launchTaskManagerApp(planService)
            }
        }

        // Подключить сервер и ожидать завершения
        server.onClose {
            fileWatcher.stop()
            job.complete()
        }
        server.connect(stdioServerTransport)
        job.join()
    }
}

/**
 * Создаёт и конфигурирует MCP Server с переданными сервисами
 *
 * @param contextService Сервис для работы с контекстом
 * @param planService Сервис для работы с планами задач
 */
fun createServer(
    contextService: ContextServiceImpl,
    planService: PlanServiceImpl
): Server {
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

    // Регистрация всех tools через ToolRegistry
    val toolRegistry = ToolRegistry(contextService, planService)
    toolRegistry.registerAllTools(server)

    return server
}

