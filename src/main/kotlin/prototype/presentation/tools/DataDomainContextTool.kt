package prototype.presentation.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import prototype.core.result.Result
import prototype.domain.service.ContextService

/**
 * Tool для получения контекста Data & Domain слоя
 */
fun Server.addDataDomainContextTool(contextService: ContextService) {
    addTool(
        name = "get_data_domain_context",
        description = """
            Используй эту функцию для получения контекста и правил генерации Data и Domain слоёв в Clean Architecture и тестов для них.
            
            Функция возвращает полный контекст с примерами существующих классов (DTO, Repository, UseCase, Mappers) 
            и правилами их создания для нового сервиса.
            
            Применяй эту функцию когда:
            - Нужно создать новый сервис по API-контракту, JSON-схеме или документации
            - Требуется сгенерировать Data/Domain слои для новой фичи
            - Необходимо понять структуру и паттерны организации кода в проекте
            - Необходимо тесты для данных слоёв
            
            Аргументы:
            - service_name: название сервиса/фичи (опционально). Используется для подстановки в именования классов 
              и пакетов вместо переменной 'x' из правил.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put(
                    "service_name",
                    buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Название сервиса/фичи для генерации кода (опционально)"))
                    }
                )
            },
            required = emptyList()
        )
    ) { request ->
        val serviceName = request.arguments["service_name"]?.jsonPrimitive?.content
        
        when (val result = contextService.getDataDomainContext(serviceName)) {
            is Result.Success -> {
                CallToolResult(
                    content = listOf(
                        TextContent(text = result.data.content)
                    )
                )
            }
            is Result.Error -> {
                CallToolResult(
                    content = listOf(
                        TextContent(text = result.error.message)
                    ),
                    isError = true
                )
            }
        }
    }
}

