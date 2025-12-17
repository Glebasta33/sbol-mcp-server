package compiler.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.*
import java.io.File

// Хранилище для параметров сборки между вызовами инструментов
object GradleBuildSession {
    data class BuildParams(
        var nameModule: String? = null,
        var path: String? = null,
        var buildType: String? = null,
        var attempt: Int = 1,
        var previousError: String? = null,
        var appliedFix: String? = null
    )

    val currentParams = BuildParams()

    fun clear() {
        currentParams.nameModule = null
        currentParams.path = null
        currentParams.buildType = null
        currentParams.attempt = 1
        currentParams.previousError = null
        currentParams.appliedFix = null
    }
}

// 1. Инструмент для запроса параметров сборки у пользователя
internal fun Server.askForGradleBuildParams() {
    addTool(
        name = "Запросить параметры Gradle сборки",
        description = """
            Запрашивает у пользователя параметры для сборки Gradle-модуля.
            
            **Что нужно запросить:**
            1. Название Gradle-модуля (например: PfmCalendarLibImpl, feature-auth, app)
            2. Тип сборки (только один из трех вариантов):
               - :assembleRelease - релизная сборка
               - :assembleDebug - отладочная сборка
               - :testDebugUnitTest - unit-тесты
            3. 
            
            Используй эту функцию когда:
            - Нужно получить у пользователя данные для сборки
            
            Аргументы:
            - module_response: название модуля
            - type_builder_response: тип сборки
            - absolute_core_path: определи сам корневой путь проекта (Абсолютный путь к корневой папке проекта)
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "module_response" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Название Gradle-модуля")
                        )
                    ),
                    "type_builder_response" to JsonObject(
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
                    "absolute_core_path" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Абсолютный путь к корневой папке проекта")
                        )
                    )
                )
            ),
            required = listOf("module_response", "type_builder_response", "absolute_core_path")
        )
    ) { request ->
        val moduleResponse = request.arguments["module_response"]?.jsonPrimitive?.content ?: ""
        val typeBuilderResponse = request.arguments["type_builder_response"]?.jsonPrimitive?.content ?: ""
        val absoluteCorePath = request.arguments["absolute_core_path"]?.jsonPrimitive?.content ?: ""

        // Проверяем, что все параметры предоставлены
        if (moduleResponse.isBlank() || typeBuilderResponse.isBlank() || absoluteCorePath.isBlank()) {
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = """
                            ⚠️ **Не все параметры предоставлены**
                            
                            Пожалуйста, укажите:
                            1. **Название модуля** - например: "PfmCalendarLibImpl"
                            2. **Тип сборки** - выберите один:
                               - :assembleRelease
                               - :assembleDebug
                               - :testDebugUnitTest
                            3. **Путь к проекту** - например: "/home/user/android-project"
                            
                            Вызовите инструмент снова, указав все три параметра.
                        """.trimIndent()
                    )
                ),
                structuredContent = JsonObject(
                    mapOf(
                        "missing_parameters" to JsonPrimitive(true),
                        "module_provided" to JsonPrimitive(moduleResponse.isNotBlank()),
                        "type_provided" to JsonPrimitive(typeBuilderResponse.isNotBlank()),
                        "path_provided" to JsonPrimitive(absoluteCorePath.isNotBlank())
                    )
                )
            )
        }

        // Проверяем валидность типа сборки
        val validBuildTypes = listOf(":assembleRelease", ":assembleDebug", ":testDebugUnitTest")
        if (typeBuilderResponse !in validBuildTypes) {
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = """
                            ❌ **Некорректный тип сборки**
                            
                            Получено: $typeBuilderResponse
                            Допустимые значения:
                            - :assembleRelease
                            - :assembleDebug
                            - :testDebugUnitTest
                            
                            Пожалуйста, выберите один из указанных вариантов.
                        """.trimIndent()
                    )
                ),
                structuredContent = JsonObject(
                    mapOf(
                        "invalid_build_type" to JsonPrimitive(true),
                        "received_type" to JsonPrimitive(typeBuilderResponse),
                        "valid_types" to JsonArray(validBuildTypes.map { JsonPrimitive(it) })
                    )
                )
            )
        }

        // Проверяем существование пути
        val projectDir = File(absoluteCorePath)
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = """
                            ❌ **Путь не существует или не является директорией**
                            
                            Проверьте путь: $absoluteCorePath
                            
                            Убедитесь, что:
                            1. Путь указан абсолютный (начинается с /)
                            2. Директория существует
                            3. У вас есть права на чтение
                        """.trimIndent()
                    )
                ),
                structuredContent = JsonObject(
                    mapOf(
                        "invalid_path" to JsonPrimitive(true),
                        "path" to JsonPrimitive(absoluteCorePath)
                    )
                )
            )
        }

        // Проверяем наличие gradlew
        val gradlewFile = File(projectDir, "gradlew")
        if (!gradlewFile.exists()) {
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = """
                            ⚠️ **Не найден файл gradlew**
                            
                            В указанном пути: $absoluteCorePath
                            Не найден файл gradlew.
                            
                            Убедитесь, что:
                            1. Это корневая папка Android проекта
                            2. В папке есть файл gradlew
                            3. У файла есть права на выполнение
                        """.trimIndent()
                    )
                ),
                structuredContent = JsonObject(
                    mapOf(
                        "missing_gradlew" to JsonPrimitive(true),
                        "path" to JsonPrimitive(absoluteCorePath)
                    )
                )
            )
        }

        // Все проверки пройдены, сохраняем параметры в сессии
        GradleBuildSession.currentParams.apply {
            nameModule = moduleResponse
            path = absoluteCorePath
            buildType = typeBuilderResponse
            attempt = 1
            previousError = null
            appliedFix = null
        }

        return@addTool CallToolResult(
            content = listOf(
                TextContent(
                    text = """
                        ✅ **Параметры успешно получены и проверены!**
                        
                        **Собранные параметры:**
                        - **Модуль:** $moduleResponse
                        - **Тип сборки:** $typeBuilderResponse
                        - **Путь:** $absoluteCorePath
                        
                        **Проверки выполнены:**
                        ✅ Путь существует
                        ✅ Файл gradlew найден
                        ✅ Тип сборки валидный
                        
                        **Следующий шаг:** 
                        Теперь запустите инструмент **"Выполнить Gradle сборку"** 
                        для выполнения сборки с этими параметрами.
                        
                        Или просто скажите "запусти сборку" - система поймет что нужно использовать собранные параметры.
                    """.trimIndent()
                )
            ),
            structuredContent = JsonObject(
                mapOf(
                    "params_collected" to JsonPrimitive(true),
                    "next_tool" to JsonPrimitive("Выполнить Gradle сборку"),
                    "collected_params" to JsonObject(
                        mapOf(
                            "name_module_arg" to JsonPrimitive(moduleResponse),
                            "path_arg" to JsonPrimitive(absoluteCorePath),
                            "version_build_arg" to JsonPrimitive(typeBuilderResponse),
                            "attempt" to JsonPrimitive(1)
                        )
                    ),
                    "checks_passed" to JsonObject(
                        mapOf(
                            "path_exists" to JsonPrimitive(true),
                            "gradlew_found" to JsonPrimitive(true),
                            "build_type_valid" to JsonPrimitive(true)
                        )
                    )
                )
            )
        )
    }
}

// 2. Инструмент для выполнения сборки Gradle
internal fun Server.executeGradleBuild() {
    addTool(
        name = "Выполнить Gradle сборку",
        description = """
            Выполняет сборку Gradle с указанными параметрами.
            
            **Входные параметры:**
            - Название модуля
            - Путь к проекту
            - Тип сборки (:assembleRelease, :assembleDebug, :testDebugUnitTest)
            - Номер попытки (начинается с 1)
            
            **Выполняет команду:** `./gradlew :[модуль][тип_сборки] > gradle_output.log 2>&1`
            
            **Результат:**
            - Если успех: возвращает информацию о сборке
            - Если ошибка: вызывает инструмент анализа ошибки
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
                            "description" to JsonPrimitive("Номер текущей попытки")
                        )
                    ),
                    "previous_error" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Ошибка из предыдущей попытки")
                        )
                    ),
                    "applied_fix" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Исправление примененное перед этой попыткой")
                        )
                    )
                )
            ),
            required = listOf("name_module_arg", "path_arg", "version_build_arg")
        )
    ) { request ->
        // Получаем параметры из запроса или из сессии
        val nameModule = request.arguments["name_module_arg"]?.jsonPrimitive?.content
            ?: GradleBuildSession.currentParams.nameModule
            ?: return@addTool missingParamResult("name_module_arg")

        val path = request.arguments["path_arg"]?.jsonPrimitive?.content
            ?: GradleBuildSession.currentParams.path
            ?: return@addTool missingParamResult("path_arg")

        val versionBuild = request.arguments["version_build_arg"]?.jsonPrimitive?.content
            ?: GradleBuildSession.currentParams.buildType
            ?: return@addTool missingParamResult("version_build_arg")

        val attempt = request.arguments["attempt"]?.jsonPrimitive?.int
            ?: GradleBuildSession.currentParams.attempt

        val previousError = request.arguments["previous_error"]?.jsonPrimitive?.content
            ?: GradleBuildSession.currentParams.previousError

        val appliedFix = request.arguments["applied_fix"]?.jsonPrimitive?.content
            ?: GradleBuildSession.currentParams.appliedFix

        // Обновляем сессию
        GradleBuildSession.currentParams.apply {
            this.nameModule = nameModule
            this.path = path
            this.buildType = versionBuild
            this.attempt = attempt
            this.previousError = previousError
            this.appliedFix = appliedFix
        }

        // Выполняем сборку
        val result = runGradleBuild(nameModule, path, versionBuild)

        if (result.success) {
            // Успешная сборка
            GradleBuildSession.clear() // Очищаем сессию после успеха

            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = """
                            ${if (attempt > 1) "🔄 Попытка #$attempt\n" else "🚀 "}✅ **Сборка успешно завершена!**
                            
                            **Команда:** `./gradlew :$nameModule$versionBuild`
                            **Путь:** $path
                            ${if (appliedFix != null) "**Исправление:** $appliedFix" else ""}
                            
                            ${if (attempt > 1) "🎉 Проблема исправлена после $attempt попыток!" else ""}
                            
                            **Последние строки лога:**
                            ```
                            ${result.lastLines}
                            ```
                        """.trimIndent()
                    )
                ),
                structuredContent = JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(true),
                        "module" to JsonPrimitive(nameModule),
                        "path" to JsonPrimitive(path),
                        "build_type" to JsonPrimitive(versionBuild),
                        "attempt" to JsonPrimitive(attempt),
                        "last_lines" to JsonPrimitive(result.lastLines)
                    )
                )
            )
        } else {
            // Ошибка сборки
            if (attempt >= 3) {
                // Максимальное количество попыток достигнуто
                return@addTool CallToolResult(
                    content = listOf(
                        TextContent(
                            text = """
                                ❌ **Сборка завершилась с ошибкой после 3 попыток**
                                
                                **Команда:** `./gradlew :$nameModule$versionBuild`
                                **Путь:** $path
                                **Попытка:** #$attempt
                                
                                **Лог ошибки:**
                                ```
                                ${result.errorLines}
                                ```
                                
                                ⚠️ **Требуется ручное вмешательство!**
                                
                                Следующий шаг: вызовите инструмент "Анализ ошибки Gradle сборки"
                                для детального анализа проблемы.
                            """.trimIndent()
                        )
                    ),
                    structuredContent = JsonObject(
                        mapOf(
                            "success" to JsonPrimitive(false),
                            "max_attempts_reached" to JsonPrimitive(true),
                            "next_tool" to JsonPrimitive("Анализ ошибки Gradle сборки"),
                            "error_output" to JsonPrimitive(result.fullOutput),
                            "error_summary" to JsonPrimitive(result.errorLines)
                        )
                    )
                )
            }

            // Есть еще попытки - просим анализ ошибки
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = """
                            ⚠️ **Сборка завершилась с ошибкой (попытка #$attempt из 3)**
                            
                            **Команда:** `./gradlew :$nameModule$versionBuild`
                            **Путь:** $path
                            ${if (appliedFix != null) "**Предыдущее исправление:** $appliedFix" else ""}
                            
                            **Ошибка:**
                            ```
                            ${result.errorLines}
                            ```
                            
                            Следующий шаг: вызовите инструмент "Анализ ошибки Gradle сборки"
                            для анализа проблемы и предложения исправления.
                        """.trimIndent()
                    )
                ),
                structuredContent = JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(false),
                        "attempt" to JsonPrimitive(attempt),
                        "next_tool" to JsonPrimitive("Анализ ошибки Gradle сборки"),
                        "error_context" to JsonObject(
                            mapOf(
                                "module" to JsonPrimitive(nameModule),
                                "path" to JsonPrimitive(path),
                                "build_type" to JsonPrimitive(versionBuild),
                                "attempt" to JsonPrimitive(attempt),
                                "error_output" to JsonPrimitive(result.errorLines),
                                "previous_error" to JsonPrimitive(previousError),
                                "applied_fix" to JsonPrimitive(appliedFix)
                            )
                        )
                    )
                )
            )
        }
    }
}

// 3. Инструмент для анализа ошибок сборки
internal fun Server.analyzeGradleError() {
    addTool(
        name = "Анализ ошибки Gradle сборки",
        description = """
            Анализирует ошибку Gradle сборки и предлагает исправления.
            
            **Ассистент должен:**
            1. Проанализировать предоставленный лог ошибки
            2. Определить тип и причину проблемы
            3. Предложить конкретное исправление
            4. Вызвать соответствующий инструмент для применения исправления
            
            **Типичные проблемы и решения:**
            - Ошибки зависимостей → изменить build.gradle
            - Ошибки компиляции → исправить код
            - Ошибки конфигурации → изменить AndroidManifest.xml
            - Проблемы с SDK → обновить версии в build.gradle
            - Проблемы с ресурсами → исправить resource файлы
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "error_output" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Лог ошибки сборки")
                        )
                    ),
                    "project_path" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Путь к проекту")
                        )
                    ),
                    "module_name" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Название модуля")
                        )
                    ),
                    "build_type" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Тип сборки")
                        )
                    ),
                    "attempt" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Номер попытки")
                        )
                    ),
                    "previous_fixes" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Предыдущие исправления (если были)")
                        )
                    )
                )
            ),
            required = listOf("error_output", "project_path")
        )
    ) { request ->
        val errorOutput = request.arguments["error_output"]!!.jsonPrimitive.content
        val projectPath = request.arguments["project_path"]!!.jsonPrimitive.content
        val moduleName = request.arguments["module_name"]?.jsonPrimitive?.content
            ?: GradleBuildSession.currentParams.nameModule
            ?: "unknown"
        val buildType = request.arguments["build_type"]?.jsonPrimitive?.content
            ?: GradleBuildSession.currentParams.buildType
            ?: ":assembleDebug"
        val attempt = request.arguments["attempt"]?.jsonPrimitive?.int
            ?: GradleBuildSession.currentParams.attempt
        val previousFixes = request.arguments["previous_fixes"]?.jsonPrimitive?.content

        return@addTool CallToolResult(
            content = listOf(
                TextContent(
                    text = """
                        🔍 **Анализ ошибки Gradle сборки**
                        
                        **Контекст:**
                        - Модуль: $moduleName
                        - Путь: $projectPath
                        - Тип сборки: $buildType
                        - Попытка: #$attempt
                        ${if (previousFixes != null) "- Предыдущие исправления: $previousFixes" else ""}
                        
                        **Лог ошибки:**
                        ```
                        ${getErrorSummary(errorOutput)}
                        ```
                        
                        🤖 **Ассистент должен сейчас:**
                        1. Проанализировать ошибку выше
                        2. Определить точную причину (зависимости, код, конфигурация и т.д.)
                        3. Предложить конкретное исправление
                        4. Вызвать соответствующий инструмент:
                           - "Изменить файл" - для правки build.gradle, кода
                           - "Выполнить команду в терминале" - для clean, других команд
                           - "Обновить зависимости" - специальный инструмент
                        
                        **После применения исправления** вызовите "Выполнить Gradle сборку" 
                        с параметром attempt=${attempt + 1} и applied_fix="[описание исправления]"
                    """.trimIndent()
                )
            ),
            structuredContent = JsonObject(
                mapOf(
                    "analysis_context" to JsonPrimitive(true),
                    "error_classification" to JsonPrimitive(classifyGradleError(errorOutput)),
                    "next_steps" to JsonArray(
                        listOf(
                            JsonPrimitive("1. Проанализировать конкретные строки ошибок"),
                            JsonPrimitive("2. Определить файлы которые нужно изменить"),
                            JsonPrimitive("3. Предложить конкретное исправление"),
                            JsonPrimitive("4. Применить исправление через соответствующий инструмент"),
                            JsonPrimitive("5. Повторить сборку с attempt=${attempt + 1}")
                        )
                    )
                )
            )
        )
    }
}

// 4. Инструмент для применения исправлений к проекту
internal fun Server.applyGradleFix() {
    addTool(
        name = "Применить исправление для Gradle",
        description = """
            Применяет исправление для решения проблемы сборки Gradle.
            
            **Ассистент должен указать:**
            1. Какой файл нужно изменить
            2. Конкретные изменения
            3. Почему это исправит проблему
            
            **После применения исправления** автоматически вызывает повторную сборку.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "file_path" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Путь к файлу который нужно изменить")
                        )
                    ),
                    "changes" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Конкретные изменения для применения")
                        )
                    ),
                    "reason" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Объяснение почему это исправит ошибку")
                        )
                    ),
                    "related_error" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Ошибка которую это исправляет")
                        )
                    )
                )
            ),
            required = listOf("file_path", "changes", "reason")
        )
    ) { request ->
        val filePath = request.arguments["file_path"]!!.jsonPrimitive.content
        val changes = request.arguments["changes"]!!.jsonPrimitive.content
        val reason = request.arguments["reason"]!!.jsonPrimitive.content
        val relatedError = request.arguments["related_error"]?.jsonPrimitive?.content

        // Получаем текущие параметры из сессии
        val session = GradleBuildSession.currentParams

        return@addTool CallToolResult(
            content = listOf(
                TextContent(
                    text = """
                        🔧 **Готово к применению исправления**
                        
                        **Исправление для:** ${session.nameModule ?: "unknown module"}
                        **Путь проекта:** ${session.path ?: "unknown path"}
                        
                        **Файл для изменения:** $filePath
                        **Причина:** $reason
                        ${
                        if (relatedError != null) "**Исправляемая ошибка:** ${
                            relatedError.take(
                                150
                            )
                        }..." else ""
                    }
                        
                        **Изменения:**
                        ```
                        $changes
                        ```
                        
                        [СИСТЕМА: Теперь ассистент должен вызвать инструмент "Изменить файл" 
                        для применения этих изменений.]
                        
                        **После успешного применения исправления:**
                        1. Обновите сессию: applied_fix = "$reason"
                        2. Вызовите "Выполнить Gradle сборку" с attempt = ${session.attempt + 1}
                    """.trimIndent()
                )
            ),
            structuredContent = JsonObject(
                mapOf(
                    "ready_for_application" to JsonPrimitive(true),
                    "next_tool" to JsonPrimitive("Изменить файл"),
                    "fix_details" to JsonObject(
                        mapOf(
                            "file_path" to JsonPrimitive(filePath),
                            "changes" to JsonPrimitive(changes),
                            "reason" to JsonPrimitive(reason)
                        )
                    ),
                    "next_build_call" to JsonObject(
                        mapOf(
                            "tool" to JsonPrimitive("Выполнить Gradle сборку"),
                            "parameters" to JsonObject(
                                mapOf(
                                    "name_module_arg" to JsonPrimitive(session.nameModule ?: ""),
                                    "path_arg" to JsonPrimitive(session.path ?: ""),
                                    "version_build_arg" to JsonPrimitive(session.buildType ?: ""),
                                    "attempt" to JsonPrimitive(session.attempt + 1),
                                    "applied_fix" to JsonPrimitive(reason.take(100))
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}

// 5. Инструмент для очистки проекта (gradle clean)
internal fun Server.cleanGradleProject() {
    addTool(
        name = "Очистить Gradle проект",
        description = """
            Выполняет команду `./gradlew clean` для очистки проекта.
            Полезно при проблемах с кэшем, старыми билдами.
            
            **После очистки** автоматически вызывает повторную сборку.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "project_path" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Путь к проекту")
                        )
                    )
                )
            ),
            required = listOf("project_path")
        )
    ) { request ->
        val projectPath = request.arguments["project_path"]!!.jsonPrimitive.content
        val projectDir = File(projectPath)

        if (!projectDir.exists()) {
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = "❌ Путь не существует: $projectPath"
                    )
                )
            )
        }

        val gradlewFile = File(projectDir, "gradlew")
        if (!gradlewFile.exists()) {
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        text = "❌ Файл gradlew не найден"
                    )
                )
            )
        }

        // Выполняем clean
        val process = ProcessBuilder("bash", "-c", "./gradlew clean > clean_output.log 2>&1")
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        val outputFile = File(projectDir, "clean_output.log")
        val output = if (outputFile.exists()) outputFile.readText() else "Не удалось прочитать лог"

        val success = exitCode == 0

        // Получаем текущие параметры из сессии
        val session = GradleBuildSession.currentParams

        // Функция для получения последних строк
        fun getLastLines(text: String, count: Int): String {
            val lines = text.lines()
            val start = maxOf(0, lines.size - count)
            return lines.subList(start, lines.size).joinToString("\n")
        }

        return@addTool CallToolResult(
            content = listOf(
                TextContent(
                    text = """
                        🧹 **${if (success) "✅ Очистка проекта выполнена" else "⚠️ Очистка завершена с кодом $exitCode"}**
                        
                        **Проект:** $projectPath
                        
                        ${
                        if (output.isNotBlank()) "**Результат:**\n```\n${
                            getLastLines(
                                output,
                                20
                            )
                        }\n```" else ""
                    }
                        
                        ${
                        if (success && session.nameModule != null && session.buildType != null) """
                        **Следующий шаг:** Запустить сборку заново.
                        
                        [СИСТЕМА: Вызовите инструмент "Выполнить Gradle сборку" 
                        с attempt = ${session.attempt + 1} и applied_fix = "Очистка проекта (gradle clean)"]
                        """.trimIndent() else ""
                    }
                    """.trimIndent()
                )
            ),
            structuredContent = JsonObject(
                mapOf(
                    "clean_success" to JsonPrimitive(success),
                    "exit_code" to JsonPrimitive(exitCode),
                    "next_tool" to if (success && session.nameModule != null) {
                        JsonPrimitive("Выполнить Gradle сборку")
                    } else {
                        JsonPrimitive("")
                    },
                    "next_params" to if (success && session.nameModule != null) {
                        JsonObject(
                            mapOf(
                                "name_module_arg" to JsonPrimitive(session.nameModule ?: ""),
                                "path_arg" to JsonPrimitive(session.path ?: ""),
                                "version_build_arg" to JsonPrimitive(session.buildType ?: ""),
                                "attempt" to JsonPrimitive(session.attempt + 1),
                                "applied_fix" to JsonPrimitive("Очистка проекта (gradle clean)")
                            )
                        )
                    } else {
                        JsonObject(emptyMap())
                    }
                )
            )
        )
    }
}

// ============ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ============

private fun missingParamResult(paramName: String): CallToolResult {
    return CallToolResult(
        content = listOf(
            TextContent(
                text = "❌ Отсутствует обязательный параметр: $paramName\n\nВызовите инструмент 'Запросить параметры Gradle сборки' сначала."
            )
        )
    )
}

private data class GradleBuildResult(
    val success: Boolean,
    val lastLines: String,
    val errorLines: String,
    val fullOutput: String
)

private fun runGradleBuild(
    nameModule: String,
    path: String,
    buildType: String
): GradleBuildResult {
    val projectDir = File(path)
    val command = "./gradlew :$nameModule$buildType > gradle_output.log 2>&1"

    val process = ProcessBuilder("bash", "-c", command)
        .directory(projectDir)
        .redirectErrorStream(true)
        .start()

    val exitCode = process.waitFor()
    val outputFile = File(projectDir, "gradle_output.log")
    val output = if (outputFile.exists()) outputFile.readText() else ""

    val success = output.contains("BUILD SUCCESSFUL", ignoreCase = true)
    val lastLines = getLastLines(output, 10)
    val errorLines = extractErrorSummary(output)

    return GradleBuildResult(success, lastLines, errorLines, output)
}

private fun extractErrorSummary(output: String): String {
    val lines = output.lines()
    val lastLines = if (lines.size > 30) getLastLines(output, 30) else output

    val errorLinesList = lastLines.lines().filter { line ->
        line.contains("error", ignoreCase = true) ||
                line.contains("fail", ignoreCase = true) ||
                line.contains("exception", ignoreCase = true)
    }

    return if (errorLinesList.isNotEmpty()) {
        errorLinesList.joinToString("\n")
    } else {
        lastLines
    }
}

private fun getErrorSummary(output: String): String {
    return extractErrorSummary(output)
}

private fun classifyGradleError(output: String): String {
    return when {
        output.contains("Could not resolve", ignoreCase = true) -> "Ошибка зависимостей"
        output.contains("compile", ignoreCase = true) &&
                output.contains("error", ignoreCase = true) -> "Ошибка компиляции"

        output.contains("SDK", ignoreCase = true) -> "Ошибка конфигурации SDK"
        output.contains("No such file", ignoreCase = true) -> "Файл не найден"
        output.contains("permission denied", ignoreCase = true) -> "Проблема с правами доступа"
        else -> "Неизвестная ошибка (требуется анализ)"
    }
}

private fun getLastLines(text: String, count: Int): String {
    val lines = text.lines()
    val start = kotlin.math.max(0, lines.size - count)
    return lines.subList(start, lines.size).joinToString("\n")
}

private fun parseGradleParams(userInput: String): Map<String, String> {
    val params = mutableMapOf<String, String>()

    // Паттерны для парсинга
    val patterns = listOf(
        """модуль\s*[:=]\s*["']?([^"' \n\r]+)["']?""".toRegex(RegexOption.IGNORE_CASE),
        """module\s*[:=]\s*["']?([^"' \n\r]+)["']?""".toRegex(RegexOption.IGNORE_CASE),
        """name\s*[:=]\s*["']?([^"' \n\r]+)["']?""".toRegex(RegexOption.IGNORE_CASE)
    )

    val typePatterns = listOf(
        """тип\s*[:=]\s*["']?([^"' \n\r]+)["']?""".toRegex(RegexOption.IGNORE_CASE),
        """type\s*[:=]\s*["']?([^"' \n\r]+)["']?""".toRegex(RegexOption.IGNORE_CASE),
        """build\s*[:=]\s*["']?([^"' \n\r]+)["']?""".toRegex(RegexOption.IGNORE_CASE)
    )

    val pathPatterns = listOf(
        """путь\s*[:=]\s*["']?([^"' \n\r]+)["']?""".toRegex(RegexOption.IGNORE_CASE),
        """path\s*[:=]\s*["']?([^"' \n\r]+)["']?""".toRegex(RegexOption.IGNORE_CASE)
    )

    // Ищем модуль
    for (pattern in patterns) {
        val match = pattern.find(userInput)
        if (match != null) {
            params["module"] = match.groupValues[1].trim()
            break
        }
    }

    // Ищем тип
    for (pattern in typePatterns) {
        val match = pattern.find(userInput)
        if (match != null) {
            val type = match.groupValues[1].trim()
            // Проверяем что тип валидный
            if (type.startsWith(":")) {
                params["type"] = type
            } else if (type.equals("release", ignoreCase = true)) {
                params["type"] = ":assembleRelease"
            } else if (type.equals("debug", ignoreCase = true)) {
                params["type"] = ":assembleDebug"
            } else if (type.contains("test", ignoreCase = true)) {
                params["type"] = ":testDebugUnitTest"
            }
            break
        }
    }

    // Ищем путь
    for (pattern in pathPatterns) {
        val match = pattern.find(userInput)
        if (match != null) {
            params["path"] = match.groupValues[1].trim()
            break
        }
    }

    // Если не нашли по паттернам, пробуем разбить по запятым
    if (params.size < 3) {
        val parts = userInput.split(',').map { it.trim() }
        if (parts.size >= 3) {
            if (params["module"] == null) params["module"] = parts[0].removeSurrounding("\"", "'")
            if (params["type"] == null) {
                val type = parts[1].removeSurrounding("\"", "'")
                when {
                    type.startsWith(":") -> params["type"] = type
                    type.equals("release", ignoreCase = true) -> params["type"] = ":assembleRelease"
                    type.equals("debug", ignoreCase = true) -> params["type"] = ":assembleDebug"
                    type.contains("test", ignoreCase = true) -> params["type"] =
                        ":testDebugUnitTest"
                }
            }
            if (params["path"] == null) params["path"] = parts[2].removeSurrounding("\"", "'")
        }
    }

    return params
}