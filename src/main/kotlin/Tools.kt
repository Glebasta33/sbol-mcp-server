package prototype

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

fun Server.addHelloWorldTool() {
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

