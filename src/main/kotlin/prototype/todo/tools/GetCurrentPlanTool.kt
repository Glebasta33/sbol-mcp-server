package prototype.todo.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import prototype.core.result.Result
import prototype.todo.domain.service.PlanService
import prototype.todo.ui.launchTaskManagerApp
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Tool для получения текущего активного плана
 */
fun Server.addGetCurrentPlanTool(planService: PlanService, coroutineScope: CoroutineScope) {
    addTool(
        name = "get_current_plan",
        description = """
            Получает информацию о текущем активном плане.
            
            Возвращает подробную информацию о плане, включая:
            - Основные данные (ID, название, описание, статус)
            - Список всех задач с их статусами
            - Прогресс выполнения
            - Путь к MD файлу
            
            Используй эту функцию когда:
            - Нужно узнать текущее состояние плана
            - Требуется получить список задач и их статусы
            - Необходимо проверить прогресс выполнения
            - Нужно получить ID задач для обновления статусов
            
            Примечание:
            - Если нет активного плана, вернется сообщение об этом
            - Активный план определяется по статусу "В работе" или самый новый по дате
            
            Аргументы:
            - include_details: (необязательный) boolean, включать ли расширенную информацию о задачах (по умолчанию true)
            
            Возвращает:
            - Полную информацию о текущем плане или сообщение об отсутствии активных планов
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put(
                    "include_details",
                    buildJsonObject {
                        put("type", JsonPrimitive("boolean"))
                        put("description", JsonPrimitive("Включать ли расширенную информацию о задачах"))
                        put("default", JsonPrimitive(true))
                    }
                )
            },
            required = listOf("include_details")
        )
    ) { request ->
        // Извлекаем необязательный аргумент (по умолчанию true)
        val includeDetails = request.arguments["include_details"]?.let {
            try {
                it.toString().toBooleanStrictOrNull() ?: true
            } catch (e: Exception) {
                true
            }
        } ?: true
        
        when (val result = planService.getCurrentPlan()) {
            is Result.Success -> {
                val plan = result.data
                if (plan == null) {
                    CallToolResult(
                        content = listOf(
                            TextContent(
                                text = """
                                    Нет активного плана.
                                    
                                    Создайте новый план с помощью create_plan:
                                    - plan_name: название плана
                                    - description: описание плана
                                    - tasks: массив названий задач
                                """.trimIndent()
                            )
                        )
                    )
                } else {
                    val tasksInfo = if (includeDetails) {
                        plan.tasks.joinToString("\n") { task ->
                            val statusIcon = when {
                                task.isCompleted() -> "✓"
                                task.isInProgress() -> "→"
                                task.isCancelled() -> "✗"
                                else -> " "
                            }
                            "  $statusIcon [${if (task.isCompleted()) "x" else " "}] ${task.id}: ${task.title} (${task.status.value})"
                        }
                    } else {
                        plan.tasks.joinToString("\n") { task ->
                            "  [${if (task.isCompleted()) "x" else " "}] ${task.id}: ${task.title}"
                        }
                    }

                    val inProgressTasks = plan.tasks.filter { it.isInProgress() }
                    val inProgressInfo = if (includeDetails && inProgressTasks.isNotEmpty()) {
                        "\n\nТекущие задачи в работе:\n" + inProgressTasks.joinToString("\n") { "  → ${it.id}: ${it.title}" }
                    } else {
                        ""
                    }

                    val detailsInfo = if (includeDetails) {
                        """
                        Создан: ${plan.createdAt}
                        Статус: ${plan.status}
                        Файл: ${plan.filePath}
                        
                        """.trimIndent()
                    } else {
                        ""
                    }

                    val helpInfo = if (includeDetails) {
                        """
                        
                        Для обновления статуса задачи используй:
                        update_task_status(task_id="<ID задачи>", status="<новый статус>")
                        """.trimIndent()
                    } else {
                        ""
                    }

                    // Запустить UI если это первый запрос текущего плана
                    if (isUILaunched.compareAndSet(false, true)) {
                        coroutineScope.launch {
                            println("Launching Task Manager UI after getting current plan...")
                            launchTaskManagerApp(planService)
                        }
                    }

                    CallToolResult(
                        content = listOf(
                            TextContent(
                                text = """
                                    Текущий активный план
                                    
                                    ID: ${plan.id}
                                    Название: ${plan.name}
                                    Описание: ${plan.description}
                                    $detailsInfo
                                    Прогресс: ${(plan.getProgress() * 100).toInt()}% (${plan.getCompletedTasksCount()}/${plan.getTotalTasksCount()} выполнено)
                                    
                                    Задачи:
                                    $tasksInfo$inProgressInfo$helpInfo
                                """.trimIndent()
                            )
                        )
                    )
                }
            }
            is Result.Error -> {
                CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка при получении текущего плана: ${result.error.message}")
                    ),
                    isError = true
                )
            }
        }
    }
}

