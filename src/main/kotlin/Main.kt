package prototype

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlinx.io.asSource
import kotlinx.io.asSink
import kotlinx.io.buffered

fun main() = runBlocking {
    val log = LoggerFactory.getLogger("HelloMcp")

    val server = Server(
        serverInfo = Implementation(
            name = "hello-mcp-kotlin",
            version = "0.0.1"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
//                resources = ServerCapabilities.Resources(
//                    subscribe = true,
//                    listChanged = true
//                ),
                tools = ServerCapabilities.Tools()
            )
        )
    )

    server.addTool(
        name = "hello",
        description = "Returns Hello World"
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
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered()
    )
    server.createSession(transport)

    log.info("MCP server stopped")
}
