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
