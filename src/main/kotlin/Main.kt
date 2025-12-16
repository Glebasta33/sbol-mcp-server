package prototype

import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Prompt
import io.modelcontextprotocol.kotlin.sdk.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.Role
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered


/**
 * MCP Server providing a simple "hello" tool.
 * Based on official MCP Kotlin SDK example.
 *
 * ./gradlew shadowJar
 * java -jar /Users/20732431/IdeaProjects/sbol-mcp-server/build/libs/my-app.jar
 *
 * ./gradlew installDist
 *
 */
fun main() {
    val server: Server = createServer()
    val stdioServerTransport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        System.out.asSink().buffered()
    )
    runBlocking {
        val job = Job()
        server.onClose { job.complete() }
        server.connect(stdioServerTransport)
        job.join()
    }
}


fun createServer(): Server {
    val info = Implementation(
        "hello-mcp-kotlin",
        "1.0.0"
    )

    val options = ServerOptions(
        capabilities = ServerCapabilities(
            prompts = ServerCapabilities.Prompts(listChanged = true),
            tools = ServerCapabilities.Tools(true),
        )
    )

    val server = Server(info, options)

    server.addHelloWorldTool()
    server.testArgumentsTool()

    return server
}