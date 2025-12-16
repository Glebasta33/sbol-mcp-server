package prototype.core.config

/**
 * Конфигурация приложения MCP Server
 */
object AppConfig {
    const val SERVER_NAME: String = "sbol-mcp-server"
    const val SERVER_VERSION: String = "1.0.0"
    
    const val PROMPTS_BASE_PATH: String = "src/main/kotlin/prototype/prompts"
    
    object Prompts {
        const val DATA_DOMAIN_LAYER_FILE: String = "data-domain-layer.md"
    }
}

