package compiler.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.*
import java.io.File

internal fun Server.runGradleCommand() {
    addTool(
        name = "Gradle-команда (запускает сборку и проверяет)",
        description = """
            Этот инструмент используется, когда пользователь просит собрать Gradle‑модуль.
            Спроси пользователя 
            Название Gradle-модуля (пример PfmCalendarLibImpl),
            Тип сборки (
            только три варианта:
            :assembleRelease
            :assembleDebug
            :testDebugUnitTest
            )
            Путь к корневой папке проекта получи сам 
            **Процесс:**
            1. Выполняет сборку Gradle
            2. Если успех - возвращает результат
            3. Если ошибка - просит ассистента проанализировать ошибку и предложить исправление
            4. Повторяет сборку с исправлениями до успеха или до 3 попыток
            
            **Ассистент должен:** 
            - Проанализировать лог ошибки
            - Предложить конкретное исправление (изменить файл, команду, параметры)
            - Вызвать соответствующий инструмент для исправления
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "name_module_arg" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Название Gradle-модуля")
                        )
                    ),
                    "path_arg" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Путь к корневой папке проекта")
                        )
                    ),
                    "version_build_arg" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Тип сборки"),
                            "enum" to JsonArray(
                                listOf(
                                    JsonPrimitive(":assembleRelease"),
                                    JsonPrimitive(":assembleDebug"),
                                    JsonPrimitive(":testDebugUnitTest")
                                )
                            )
                        )
                    ),
                    "attempt" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Номер текущей попытки (начинается с 1)")
                        )
                    ),
                    "previous_error" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Ошибка из предыдущей попытки (для анализа)")
                        )
                    ),
                    "applied_fix" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Какое исправление было применено перед этой попыткой")
                        )
                    )
                )
            ),
            required = listOf("name_module_arg", "path_arg", "version_build_arg")
        )
    ) { request ->
        val nameModule = request.arguments["name_module_arg"]!!.jsonPrimitive.content
        val path = request.arguments["path_arg"]!!.jsonPrimitive.content
        val versionBuild = request.arguments["version_build_arg"]!!.jsonPrimitive.content
        val attempt = request.arguments["attempt"]?.jsonPrimitive?.int ?: 1
        val previousError = request.arguments["previous_error"]?.jsonPrimitive?.content
        val appliedFix = request.arguments["applied_fix"]?.jsonPrimitive?.content


        // Проверяем путь и файлы
        val projectDir = File(path)
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = "❌ Путь не существует или не является директорией: $path"
                    )
                )
            )
        }

        val gradlewFile = File(projectDir, "gradlew")
        if (!gradlewFile.exists()) {
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = "❌ Файл gradlew не найден в пути: $path"
                    )
                )
            )
        }

        // Если это повторная попытка, показываем что было сделано
        val buildInfo = if (attempt > 1) {
            """
            🔄 **Попытка сборки #$attempt**
            ${if (appliedFix != null) "Применённое исправление: $appliedFix" else ""}
            ${if (previousError != null) "Предыдущая ошибка: ${previousError.take(200)}..." else ""}
            
            """.trimIndent()
        } else {
            "🚀 **Запуск сборки Gradle...**\n\n"
        }

        // Выполняем команду сборки
        val command = "./gradlew :$nameModule$versionBuild > gradle_output.log 2>&1"
        val process = ProcessBuilder("bash", "-c", command)
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        val outputFile = File(projectDir, "gradle_output.log")
        val output = if (outputFile.exists()) outputFile.readText() else "Не удалось прочитать лог"

        // Проверяем результат
        if (output.contains("BUILD SUCCESSFUL", ignoreCase = true)) {
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = """
                            ${buildInfo}✅ **Сборка успешно завершена!**
                            
                            Команда: `$command`
                            Попытка: #$attempt
                            Путь: $path
                            
                            **Последние строки лога:**
                            ```
                            ${output.lines().takeLast(10).joinToString("\n")}
                            ```
                            
                            ${if (attempt > 1) "🎉 Проблема исправлена после $attempt попыток!" else ""}
                        """.trimIndent()
                    )
                ),
                structuredContent = JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(true),
                        "command" to JsonPrimitive(command),
                        "attempt" to JsonPrimitive(attempt),
                        "output_snippet" to JsonPrimitive(
                            output.lines().takeLast(10).joinToString("\n")
                        )
                    )
                )
            )
        }

        // Если сборка не удалась
        if (attempt >= 3) {
            // Максимальное количество попыток достигнуто
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = """
                            ${buildInfo}❌ **Сборка завершилась с ошибкой после $attempt попыток**
                            
                            Команда: `$command`
                            Путь: $path
                            
                            **Лог ошибки:**
                            ```
                            ${extractErrorLines(output)}
                            ```
                            
                            ⚠️ **Требуется вмешательство ассистента!**
                            
                            Ассистент должен:
                            1. Проанализировать ошибку выше
                            2. Предложить конкретное исправление
                            3. Вызвать нужный инструмент для исправления
                            4. После исправления повторить сборку
                        """.trimIndent()
                    )
                ),
                structuredContent = JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(false),
                        "max_attempts_reached" to JsonPrimitive(true),
                        "command" to JsonPrimitive(command),
                        "attempt" to JsonPrimitive(attempt),
                        "error_output" to JsonPrimitive(output),
                        "requires_assistant_analysis" to JsonPrimitive(true)
                    )
                )
            )
        }

        // Если еще есть попытки, просим ассистента проанализировать ошибку
        val errorLines = extractErrorLines(output)

        return@addTool CallToolResult(
            content = listOf(
                TextContent(
                    text = """
                        ${buildInfo}⚠️ **Сборка завершилась с ошибкой (попытка #$attempt из 3)**
                        
                        Команда: `$command`
                        
                        **Ошибка:**
                        ```
                        $errorLines
                        ```
                        
                        🤖 **Ассистент должен сейчас:**
                        1. Проанализировать ошибку выше
                        2. Определить причину проблемы
                        3. Предложить конкретное исправление (например: 
                           - изменить build.gradle
                           - обновить зависимости
                           - исправить код
                           - изменить параметры сборки)
                        4. Вызвать соответствующий инструмент для исправления
                        5. После исправления вызвать этот инструмент снова с attempt=${attempt + 1}
                        
                        **Важные подсказки для анализа:**
                        - Ищите строки "error:", "failed:", "exception:", "cannot", "unable"
                        - Проверьте зависимости, версии SDK, синтаксис кода
                        - Сравните с предыдущими ошибками если есть
                        
                        **Формат следующего вызова этого инструмента:**
                        ```
                        name_module_arg: "$nameModule"
                        path_arg: "$path"
                        version_build_arg: "$versionBuild"
                        attempt: ${attempt + 1}
                        previous_error: "$errorLines"
                        applied_fix: "[опишите что исправили]"
                        ```
                    """.trimIndent()
                )
            ),
            structuredContent = JsonObject(
                mapOf(
                    "success" to JsonPrimitive(false),
                    "command" to JsonPrimitive(command),
                    "attempt" to JsonPrimitive(attempt),
                    "next_attempt" to JsonPrimitive(attempt + 1),
                    "error_output" to JsonPrimitive(output),
                    "error_snippet" to JsonPrimitive(errorLines),
                    "requires_fix" to JsonPrimitive(true),
                    "suggest_next_call" to JsonObject(
                        mapOf(
                            "tool" to JsonPrimitive("Gradle-команда (запускает сборку и проверяет)"),
                            "parameters" to JsonObject(
                                mapOf(
                                    "name_module_arg" to JsonPrimitive(nameModule),
                                    "path_arg" to JsonPrimitive(path),
                                    "version_build_arg" to JsonPrimitive(versionBuild),
                                    "attempt" to JsonPrimitive(attempt + 1),
                                    "previous_error" to JsonPrimitive(errorLines)
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}

// Вспомогательная функция для извлечения строк с ошибками
// Улучшенная версия: показывает последние N строк + выделяет ошибки
private fun extractErrorLines(output: String, maxLines: Int = 30): String {
    val allLines = output.lines()

    // Берем последние maxLines строк
    val lastLines = if (allLines.size > maxLines) {
        allLines.takeLast(maxLines)
    } else {
        allLines
    }

    // Разделяем на "важные" строки (с ошибками) и остальные
    val importantLines = mutableListOf<String>()
    val otherLines = mutableListOf<String>()

    lastLines.forEach { line ->
        if (line.contains("error", ignoreCase = true) ||
            line.contains("fail", ignoreCase = true) ||
            line.contains("exception", ignoreCase = true) ||
            line.contains("cannot", ignoreCase = true) ||
            line.contains("unable", ignoreCase = true) ||
            line.contains("missing", ignoreCase = true) ||
            line.contains("not found", ignoreCase = true)
        ) {
            importantLines.add("❌ $line")
        } else if (line.contains("warning", ignoreCase = true)) {
            otherLines.add("⚠️ $line")
        } else {
            otherLines.add(line)
        }
    }

    // Формируем результат
    val result = StringBuilder()

    if (importantLines.isNotEmpty()) {
        result.append("**Ключевые ошибки:**\n")
        result.append(importantLines.joinToString("\n"))
        result.append("\n\n")
    }

    if (otherLines.isNotEmpty()) {
        result.append("**Последние строки лога:**\n")
        result.append(otherLines.takeLast(15).joinToString("\n"))
    }

    return result.toString()
}