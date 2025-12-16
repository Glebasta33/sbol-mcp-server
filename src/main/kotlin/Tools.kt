package prototype

import io.ktor.http.parameters
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.*

fun Server.addHelloWorldTool() {
    addTool(
        name = "hello",
        description = "Returns Hello World message from Kotlin MCP server"
    ) { _ ->
        CallToolResult(
            content = listOf(
                TextContent(
                    text = "Hello World from SBOL MCP Server ! ^_^"
                )
            )
        )
    }
}

fun Server.addGetCurrentTimeTool() {
    addTool(
        name = "get_time",
        description = "Returns current date and time"
    ) { _ ->
        val currentTime = java.time.LocalDateTime.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        CallToolResult(
            content = listOf(
                TextContent(
                    text = "Current time: ${currentTime.format(formatter)}"
                )
            )
        )
    }
}

fun Server.addSystemInfoTool() {
    addTool(
        name = "system_info",
        description = "Returns basic system information"
    ) { _ ->
        val info = buildString {
            appendLine("System Information:")
            appendLine("- OS: ${System.getProperty("os.name")}")
            appendLine("- OS Version: ${System.getProperty("os.version")}")
            appendLine("- Architecture: ${System.getProperty("os.arch")}")
            appendLine("- Java Version: ${System.getProperty("java.version")}")
            appendLine("- Java Vendor: ${System.getProperty("java.vendor")}")
            appendLine("- User: ${System.getProperty("user.name")}")
            appendLine("- Working Directory: ${System.getProperty("user.dir")}")
        }
        CallToolResult(
            content = listOf(
                TextContent(
                    text = info
                )
            )
        )
    }
}

