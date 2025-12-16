package prototype.presentation.tools

import io.modelcontextprotocol.kotlin.sdk.server.Server
import prototype.domain.service.ContextService

/**
 * Реестр всех MCP Tools
 * Регистрирует все доступные tools в MCP сервере
 */
class ToolRegistry(
    private val contextService: ContextService
) {
    /**
     * Регистрирует все tools в сервере
     */
    fun registerAllTools(server: Server) {
        server.addHelloTool()
        server.addDataDomainContextTool(contextService)
    }
}

