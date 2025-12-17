package prototype.domain.model

/**
 * Контекст для tool-а с данными и метаданными
 */
data class ToolContext(
    val content: String,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun withServiceName(content: String, serviceName: String): ToolContext {
            val intro = """
                📋 Контекст для генерации Data & Domain слоёв
                🎯 Сервис: $serviceName
                
                ℹ️ В правилах ниже используется переменная 'x' - она соответствует названию твоего сервиса: '$serviceName'
                
                ---
                
            """.trimIndent()
            
            return ToolContext(
                content = intro + content,
                metadata = mapOf("service_name" to serviceName)
            )
        }
        
        fun withoutServiceName(content: String): ToolContext {
            val intro = """
                📋 Контекст для генерации Data & Domain слоёв               
            """.trimIndent()
            
            return ToolContext(
                content = intro + content,
                metadata = emptyMap()
            )
        }
    }
}

