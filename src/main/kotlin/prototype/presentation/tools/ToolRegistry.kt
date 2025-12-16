package prototype.presentation.tools

import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.CoroutineScope
import prototype.domain.service.ContextService
import prototype.domain.service.FileService
import prototype.todo.domain.service.PlanService
import prototype.todo.tools.addCreatePlanTool
import prototype.todo.tools.addGetCurrentPlanTool
import prototype.todo.tools.addUpdateTaskStatusTool

/**
 * Реестр всех MCP Tools
 * Регистрирует все доступные tools в MCP сервере
 */
class ToolRegistry(
    private val contextService: ContextService,
    private val fileService: FileService,
    private val planService: PlanService? = null,
    private val coroutineScope: CoroutineScope
) {
    /**
     * Регистрирует все tools в сервере
     */
    fun registerAllTools(server: Server) {
        // Базовые tools
        server.addHelloTool()
        server.testArgumentsTool(fileService)
        server.addDataDomainContextTool(contextService)

        // Plan management tools
        planService?.let { service ->
            server.addCreatePlanTool(service, coroutineScope)
            server.addUpdateTaskStatusTool(service)
            server.addGetCurrentPlanTool(service)
        }
    }
}

