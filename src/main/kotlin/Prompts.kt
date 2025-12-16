package prototype

import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.Role
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server

fun Server.addTestPrompt(){
    addPrompt(
        name = "Kotlin Developer",
        description = "Develop small kotlin applications",
        arguments = listOf(
            PromptArgument(
                name = "Project Name",
                description = "Project name for the new project",
                required = true,
            ),
        ),
    ) { request ->
        GetPromptResult(
            messages = listOf(
                PromptMessage(
                    role = Role.user,
                    content = TextContent(
                        "Develop a kotlin project named <name>${request.arguments?.get("Project Name")}</name>",
                    ),
                ),
            ),
            description = "Description for ${request.name}",
        )
    }

}