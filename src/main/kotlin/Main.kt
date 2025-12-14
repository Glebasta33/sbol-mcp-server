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
import java.util.logging.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Initialize logger with file handler
 */
private fun initLogger(): Logger {
    val logger = Logger.getLogger("MCPServer")
    logger.level = Level.ALL
    
    try {
        // Create log file in project root directory
        val logFile = File("mcp-server.log")
        val fileHandler = FileHandler(logFile.absolutePath, true) // true = append mode
        
        // Custom formatter for better readability
        fileHandler.formatter = object : Formatter() {
            private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            
            override fun format(record: LogRecord): String {
                val timestamp = LocalDateTime.now().format(dateFormat)
                return "[$timestamp] [${record.level}] ${record.message}\n"
            }
        }
        
        fileHandler.level = Level.ALL
        logger.addHandler(fileHandler)
        logger.useParentHandlers = false // Disable console output to avoid interfering with MCP protocol
        
    } catch (e: Exception) {
        System.err.println("Failed to initialize logger: ${e.message}")
        e.printStackTrace()
    }
    
    return logger
}

/**
 * MCP Server providing a simple "hello" tool.
 * Based on official MCP Kotlin SDK example.
 */
fun main() {
    val logger = initLogger()
    logger.info("=== MCP Server Starting ===")
    logger.info("Initializing server components...")
    
    try {
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
        logger.info("Server instance created successfully")
        logger.info("Registering tools...")

        // Tool 1: Simple Hello
        server.addTool(
            name = "hello",
            description = "Returns Hello World message from Kotlin MCP server"
        ) { _ ->
            logger.info("Tool 'hello' invoked")
            try {
                val result = CallToolResult(
                    content = listOf(
                        TextContent(
                            text = "Hello World from Kotlin MCP!"
                        )
                    )
                )
                logger.info("Tool 'hello' completed successfully")
                result
            } catch (e: Exception) {
                logger.severe("Error in 'hello' tool: ${e.message}")
                logger.severe("Stack trace: ${e.stackTraceToString()}")
                throw e
            }
        }
        logger.info("Tool 'hello' registered")
    
        // Tool 2: Echo (с параметром)
        server.addTool(
            name = "echo",
            description = "Echoes back the provided message"
        ) { request ->
            logger.info("Tool 'echo' invoked")
            try {
                logger.fine("Extracting 'message' parameter from request")
                val message = request.arguments?.get("message")?.toString() ?: "No message provided"
                logger.info("Tool 'echo' received message: $message")
                
                val result = CallToolResult(
                    content = listOf(
                        TextContent(
                            text = "Echo: $message"
                        )
                    )
                )
                logger.info("Tool 'echo' completed successfully")
                result
            } catch (e: Exception) {
                logger.severe("Error in 'echo' tool: ${e.message}")
                logger.severe("Stack trace: ${e.stackTraceToString()}")
                throw e
            }
        }
        logger.info("Tool 'echo' registered")
    
        // Tool 3: Current Time
        server.addTool(
            name = "get_time",
            description = "Returns current date and time"
        ) { _ ->
            logger.info("Tool 'get_time' invoked")
            try {
                logger.fine("Getting current date and time")
                val currentTime = java.time.LocalDateTime.now()
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                logger.fine("Formatting time with pattern: yyyy-MM-dd HH:mm:ss")
                val formattedTime = currentTime.format(formatter)
                logger.info("Tool 'get_time' retrieved time: $formattedTime")
                
                val result = CallToolResult(
                    content = listOf(
                        TextContent(
                            text = "Current time: $formattedTime"
                        )
                    )
                )
                logger.info("Tool 'get_time' completed successfully")
                result
            } catch (e: Exception) {
                logger.severe("Error in 'get_time' tool: ${e.message}")
                logger.severe("Stack trace: ${e.stackTraceToString()}")
                throw e
            }
        }
        logger.info("Tool 'get_time' registered")
    
        // Tool 4: Calculator
        server.addTool(
            name = "calculate",
            description = "Performs simple arithmetic operations (add, subtract, multiply, divide)"
        ) { request ->
            logger.info("Tool 'calculate' invoked")
            try {
                logger.fine("Extracting parameters from request")
                val a = request.arguments?.get("a")?.toString()?.trim('"')?.toDoubleOrNull() ?: 0.0
                val b = request.arguments?.get("b")?.toString()?.trim('"')?.toDoubleOrNull() ?: 0.0
                val operation = request.arguments?.get("operation")?.toString()?.trim('"') ?: "add"
                logger.info("Tool 'calculate' parameters: a=$a, b=$b, operation=$operation")
                
                logger.fine("Performing arithmetic operation: $operation")
                val result = when (operation.lowercase()) {
                    "add", "+" -> {
                        logger.fine("Performing addition: $a + $b")
                        a + b
                    }
                    "subtract", "-" -> {
                        logger.fine("Performing subtraction: $a - $b")
                        a - b
                    }
                    "multiply", "*" -> {
                        logger.fine("Performing multiplication: $a * $b")
                        a * b
                    }
                    "divide", "/" -> {
                        if (b != 0.0) {
                            logger.fine("Performing division: $a / $b")
                            a / b
                        } else {
                            logger.warning("Division by zero attempted: $a / $b")
                            throw IllegalArgumentException("Division by zero")
                        }
                    }
                    else -> {
                        logger.warning("Unknown operation requested: $operation")
                        throw IllegalArgumentException("Unknown operation: $operation")
                    }
                }
                logger.info("Tool 'calculate' result: $result")
                
                CallToolResult(
                    content = listOf(
                        TextContent(
                            text = "Result: $a $operation $b = $result"
                        )
                    )
                )
            } catch (e: Exception) {
                logger.severe("Error in 'calculate' tool: ${e.message}")
                logger.severe("Stack trace: ${e.stackTraceToString()}")
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
        logger.info("Tool 'calculate' registered")
    
        // Tool 5: System Info
        server.addTool(
            name = "system_info",
            description = "Returns basic system information"
        ) { _ ->
            logger.info("Tool 'system_info' invoked")
            try {
                logger.fine("Retrieving system properties")
                val info = buildString {
                    appendLine("System Information:")
                    try {
                        appendLine("- OS: ${System.getProperty("os.name")}")
                        appendLine("- OS Version: ${System.getProperty("os.version")}")
                        appendLine("- Architecture: ${System.getProperty("os.arch")}")
                        appendLine("- Java Version: ${System.getProperty("java.version")}")
                        appendLine("- Java Vendor: ${System.getProperty("java.vendor")}")
                        appendLine("- User: ${System.getProperty("user.name")}")
                        appendLine("- Working Directory: ${System.getProperty("user.dir")}")
                        logger.fine("System properties retrieved successfully")
                    } catch (e: SecurityException) {
                        logger.warning("Security exception while accessing system properties: ${e.message}")
                        appendLine("- Error: Unable to access some system properties due to security restrictions")
                    }
                }
                logger.info("Tool 'system_info' completed successfully")
                
                CallToolResult(
                    content = listOf(
                        TextContent(
                            text = info
                        )
                    )
                )
            } catch (e: Exception) {
                logger.severe("Error in 'system_info' tool: ${e.message}")
                logger.severe("Stack trace: ${e.stackTraceToString()}")
                throw e
            }
        }
        logger.info("Tool 'system_info' registered")
        logger.info("All tools registered successfully")

        logger.info("Creating stdio transport...")
        val transport = StdioServerTransport(
            inputStream = System.`in`.asInput(),
            outputStream = System.out.asSink().buffered()
        )
        logger.info("Transport created successfully")

        logger.info("Starting server session...")
        runBlocking {
            try {
                val session = server.createSession(transport)
                logger.info("Server session created successfully")
                logger.info("MCP Server is now running and ready to accept requests")
                
                val done = Job()
                session.onClose {
                    logger.info("Session close event received")
                    done.complete()
                }
                done.join()
                logger.info("Server session ended normally")
            } catch (e: Exception) {
                logger.severe("Error during server session: ${e.message}")
                logger.severe("Stack trace: ${e.stackTraceToString()}")
                throw e
            }
        }
    } catch (e: Exception) {
        logger.severe("FATAL ERROR: Server initialization or execution failed: ${e.message}")
        logger.severe("Stack trace: ${e.stackTraceToString()}")
        throw e
    } finally {
        logger.info("=== MCP Server Shutdown ===")
    }
}
