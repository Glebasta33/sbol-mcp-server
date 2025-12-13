package prototype

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import io.ktor.utils.io.streams.asInput

/**
 * MCP Server providing a simple "hello" tool.
 * Based on official MCP Kotlin SDK example.
 */
fun main() {
    val server = Server(
        serverInfo = Implementation(
            name = "hello-mcp-kotlin",
            version = "0.0.1"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools()
            )
        )
    )

    // Tool 1: Simple Hello
    server.addTool(
        name = "hello",
        description = "Returns Hello World message from Kotlin MCP server"
    ) { _ ->
        CallToolResult(
            content = listOf(
                TextContent(
                    text = "Hello World from Kotlin MCP!"
                )
            )
        )
    }
    
    // Tool 2: Echo (с параметром)
    server.addTool(
        name = "echo",
        description = "Echoes back the provided message"
    ) { request ->
        val message = request.arguments?.get("message")?.toString() ?: "No message provided"
        CallToolResult(
            content = listOf(
                TextContent(
                    text = "Echo: $message"
                )
            )
        )
    }
    
    // Tool 3: Current Time
    server.addTool(
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
    
    // Tool 4: Calculator
    server.addTool(
        name = "calculate",
        description = "Performs simple arithmetic operations (add, subtract, multiply, divide)"
    ) { request ->
        try {
            val a = request.arguments?.get("a")?.toString()?.trim('"')?.toDoubleOrNull() ?: 0.0
            val b = request.arguments?.get("b")?.toString()?.trim('"')?.toDoubleOrNull() ?: 0.0
            val operation = request.arguments?.get("operation")?.toString()?.trim('"') ?: "add"
            
            val result = when (operation.lowercase()) {
                "add", "+" -> a + b
                "subtract", "-" -> a - b
                "multiply", "*" -> a * b
                "divide", "/" -> {
                    if (b != 0.0) a / b
                    else throw IllegalArgumentException("Division by zero")
                }
                else -> throw IllegalArgumentException("Unknown operation: $operation")
            }
            
            CallToolResult(
                content = listOf(
                    TextContent(
                        text = "Result: $a $operation $b = $result"
                    )
                )
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent(
                        text = "Error: ${e.message}"
                    )
                ),
                isError = true
            )
        }
    }
    
    // Tool 5: System Info
    server.addTool(
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

    val transport = StdioServerTransport(
        inputStream = System.`in`.asInput(),
        outputStream = System.out.asSink().buffered()
    )

    runBlocking {
        val session = server.createSession(transport)
        val done = Job()
        session.onClose {
            done.complete()
        }
        done.join()
    }
}
