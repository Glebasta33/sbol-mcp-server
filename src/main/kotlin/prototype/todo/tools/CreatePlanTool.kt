package prototype.todo.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import prototype.core.result.Result
import prototype.todo.domain.service.PlanService
import prototype.todo.ui.launchTaskManagerApp
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Глобальный флаг для отслеживания запуска UI
 */
private val isUILaunched = AtomicBoolean(false)

/**
 * Tool для создания нового плана с задачами
 */
fun Server.addCreatePlanTool(planService: PlanService, coroutineScope: CoroutineScope) {
    addTool(
        name = "create_plan",
        description = """
            Создает новый план с задачами и сохраняет его в MD файл.
            
            План будет создан в директории .cursor/plans/ с именем файла вида:
            plan-{название}-{timestamp}-{id}.md
            
            Используй эту функцию когда:
            - Нужно создать новый план работы
            - Требуется структурировать задачи для проекта
            - Необходимо задокументировать этапы реализации
            
            Аргументы:
            - plan_name: название плана (обязательно)
            - description: описание плана и его целей (обязательно)
            - tasks: массив названий задач (обязательно, минимум 1 задача)
            
            Возвращает:
            - Информацию о созданном плане включая ID, путь к файлу и список задач
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put(
                    "plan_name",
                    buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Название плана"))
                    }
                )
                put(
                    "description",
                    buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Описание плана и его целей"))
                    }
                )
                put(
                    "tasks",
                    buildJsonObject {
                        put("type", JsonPrimitive("array"))
                        put(
                            "items",
                            buildJsonObject {
                                put("type", JsonPrimitive("string"))
                            }
                        )
                        put("description", JsonPrimitive("Массив названий задач"))
                    }
                )
            },
            required = listOf("plan_name", "description", "tasks")
        )
    ) { request ->
        val planName = request.arguments["plan_name"]?.jsonPrimitive?.content
        val description = request.arguments["description"]?.jsonPrimitive?.content
        val tasksArray = request.arguments["tasks"]?.jsonArray

        if (planName == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Параметр 'plan_name' обязателен.")),
                isError = true
            )
        }

        if (description == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Параметр 'description' обязателен.")),
                isError = true
            )
        }

        if (tasksArray == null || tasksArray.isEmpty()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Параметр 'tasks' обязателен и должен содержать хотя бы одну задачу.")),
                isError = true
            )
        }

        val tasks = tasksArray.map { it.jsonPrimitive.content }

        when (val result = planService.createPlan(planName, description, tasks)) {
            is Result.Success -> {
                val plan = result.data
                val tasksInfo = plan.tasks.joinToString("\n") { task ->
                    "  - [${if (task.isCompleted()) "x" else " "}] ${task.id}: ${task.title} (${task.status.value})"
                }

                // Запустить UI если это первый план
                if (isUILaunched.compareAndSet(false, true)) {
                    coroutineScope.launch {
                        println("Launching Task Manager UI after first plan creation...")
                        launchTaskManagerApp(planService)
                    }
                }

                CallToolResult(
                    content = listOf(
                        TextContent(
                            text = """
                                План успешно создан!
                                
                                ID: ${plan.id}
                                Название: ${plan.name}
                                Описание: ${plan.description}
                                Создан: ${plan.createdAt}
                                Статус: ${plan.status}
                                Файл: ${plan.filePath}
                                
                                Задачи (${plan.getTotalTasksCount()}):
                                $tasksInfo
                                
                                Прогресс: ${(plan.getProgress() * 100).toInt()}% (${plan.getCompletedTasksCount()}/${plan.getTotalTasksCount()})
                            """.trimIndent()
                        )
                    )
                )
            }
            is Result.Error -> {
                CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка при создании плана: ${result.error.message}")
                    ),
                    isError = true
                )
            }
        }
    }
}