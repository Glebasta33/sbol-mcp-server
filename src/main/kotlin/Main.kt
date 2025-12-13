package prototype

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.awaitCancellation
import kotlinx.io.asSource
import kotlinx.io.asSink
import kotlinx.io.buffered
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Логирование в файл, чтобы не засорять STDOUT/STDERR.
 * STDOUT используется для MCP протокола (JSON-RPC), любой вывод туда ломает протокол!
 */
private val logFile = File(System.getProperty("java.io.tmpdir"), "mcp-hello-kotlin.log")
private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

private fun log(message: String) {
    val timestamp = LocalDateTime.now().format(timeFormatter)
    logFile.appendText("[$timestamp] $message\n")
}

fun main(): Unit = runBlocking {
    try {
        log("=== MCP Server Starting ===")
        log("Log file: ${logFile.absolutePath}")
        
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
        
        log("Server instance created")

        server.addTool(
            name = "hello",
            description = "Returns Hello World message from Kotlin MCP server"
        ) { request ->
            log("Tool 'hello' called with arguments: ${request.arguments}")
            CallToolResult(
                content = listOf(
                    TextContent(
                        text = "Hello World from Kotlin MCP!"
                    )
                )
            )
        }
        
        log("Tool 'hello' registered")

        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )
        
        log("Transport created, creating session...")
        server.createSession(transport)
        log("Session created successfully, server is running and waiting for requests")
        
        // Держим сервер активным до получения сигнала завершения
        awaitCancellation()
    } catch (e: Exception) {
        log("Server error: ${e.message}")
        log("Stack trace: ${e.stackTraceToString()}")
        throw e
    }
}
