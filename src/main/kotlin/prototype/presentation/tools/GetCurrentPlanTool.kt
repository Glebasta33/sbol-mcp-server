package prototype.presentation.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.buildJsonObject
import prototype.core.result.Result
import prototype.todo.domain.service.PlanService

/**
 * Tool для получения текущего активного плана
 */
fun Server.addGetCurrentPlanTool(planService: PlanService) {
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
            
            Не требует аргументов.
            
            Возвращает:
            - Полную информацию о текущем плане или сообщение об отсутствии активных планов
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject { },
            required = emptyList()
        )
    ) { _ ->
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
                    val tasksInfo = plan.tasks.joinToString("\n") { task ->
                        val statusIcon = when {
                            task.isCompleted() -> "✓"
                            task.isInProgress() -> "→"
                            task.isCancelled() -> "✗"
                            else -> " "
                        }
                        "  $statusIcon [${if (task.isCompleted()) "x" else " "}] ${task.id}: ${task.title} (${task.status.value})"
                    }

                    val inProgressTasks = plan.tasks.filter { it.isInProgress() }
                    val inProgressInfo = if (inProgressTasks.isNotEmpty()) {
                        "\n\nТекущие задачи в работе:\n" + inProgressTasks.joinToString("\n") { "  → ${it.id}: ${it.title}" }
                    } else {
                        ""
                    }

                    CallToolResult(
                        content = listOf(
                            TextContent(
                                text = """
                                    Текущий активный план
                                    
                                    ID: ${plan.id}
                                    Название: ${plan.name}
                                    Описание: ${plan.description}
                                    Создан: ${plan.createdAt}
                                    Статус: ${plan.status}
                                    Файл: ${plan.filePath}
                                    
                                    Прогресс: ${(plan.getProgress() * 100).toInt()}% (${plan.getCompletedTasksCount()}/${plan.getTotalTasksCount()} выполнено)
                                    
                                    Задачи:
                                    $tasksInfo$inProgressInfo
                                    
                                    Для обновления статуса задачи используй:
                                    update_task_status(task_id="<ID задачи>", status="<новый статус>")
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

