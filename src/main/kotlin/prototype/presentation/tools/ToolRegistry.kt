package prototype.presentation.tools

import io.modelcontextprotocol.kotlin.sdk.server.Server
import prototype.domain.service.ContextService
import prototype.domain.service.FileService

/**
 * Реестр всех MCP Tools
 * Регистрирует все доступные tools в MCP сервере
 */
class ToolRegistry(
    private val contextService: ContextService,
    private val fileService: FileService
) {
    /**
     * Регистрирует все tools в сервере
     */
    fun registerAllTools(server: Server) {
        server.addHelloTool()
        server.testArgumentsTool(fileService)
        server.addDataDomainContextTool(contextService)
    }
}

