package prototype.presentation.tools

import io.modelcontextprotocol.kotlin.sdk.server.Server
import prototype.domain.service.ContextService
import prototype.domain.service.FileService
import prototype.todo.domain.service.PlanService

/**
 * Реестр всех MCP Tools
 * Регистрирует все доступные tools в MCP сервере
 */
class ToolRegistry(
    private val contextService: ContextService,
    private val fileService: FileService
    private val planService: PlanService? = null
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
            server.addCreatePlanTool(service)
            server.addUpdateTaskStatusTool(service)
            server.addGetCurrentPlanTool(service)
        }
    }
}

