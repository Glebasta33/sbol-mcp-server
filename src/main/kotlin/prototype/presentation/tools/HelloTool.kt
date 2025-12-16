package prototype.presentation.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.JsonNull.content
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import prototype.data.service.MdFiles
import prototype.domain.service.FileService
import java.io.File
import kotlin.getOrThrow

/**
 * Tool для приветствия пользователя
 */
fun Server.addHelloTool() {
    addTool(
        name = "hello",
        description = """
            Используй данную функцию, когда тебя просят передать привет. 
            Функция принимает аргумент name_arg в виде строки String.
            name_arg - имя человека, которому нужно передать привет.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "name_arg" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("name_arg")
        )
    ) { request ->

        val helloArg = request.arguments["name_arg"]?.jsonPrimitive?.content ?: return@addTool CallToolResult(
            content = listOf(TextContent("The 'state' parameter is required.")),
        )

        CallToolResult(
            content = listOf(
                TextContent(
                    text = " Привет, $helloArg! ^_^"
                )
            )
        )
    }
}


fun Server.testArgumentsTool(fileService: FileService) {
    addTool(
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put(
                    "название функции",
                    buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("любой текст"))
                    },
                )
            },
            required = listOf("название функции"),
        ),
        name = "hello",
        description = "Returns Hello World message from Kotlin MCP server"
    ) { request ->
        var data = ""
        with(fileService) { MdFiles.DATA.readPromptFileEx() }.onSuccess { data = it }
        CallToolResult(
            content = listOf(
                TextContent(
                    text = "Hello World from SBOL MCP Server ! ^_^" + data.take(100)
                )
            )
        )
    }
}
