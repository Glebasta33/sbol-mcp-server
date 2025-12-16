package prototype.todo.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import prototype.core.result.Result
import prototype.todo.domain.model.TaskStatus
import prototype.todo.domain.service.PlanService

/**
 * Tool для обновления статуса задачи в плане
 */
fun Server.addUpdateTaskStatusTool(planService: PlanService) {
    addTool(
        name = "update_task_status",
        description = """
            Обновляет статус задачи в текущем активном плане.
            
            Изменения автоматически сохраняются в MD файл плана.
            
            Используй эту функцию когда:
            - Нужно отметить задачу как выполненную
            - Требуется изменить статус задачи (начата, отменена и т.д.)
            - Необходимо обновить прогресс выполнения плана
            
            Аргументы:
            - task_id: идентификатор задачи (обязательно)
            - status: новый статус задачи (обязательно)
              Возможные значения:
              * "pending" - ожидает выполнения
              * "in_progress" - в процессе выполнения
              * "completed" - выполнена
              * "cancelled" - отменена
            
            Примечание:
            - Если активного плана нет, будет возвращена ошибка
            - Task ID можно получить из вывода create_plan или get_current_plan
            
            Возвращает:
            - Информацию об обновленном плане с новым статусом задачи
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put(
                    "task_id",
                    buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Идентификатор задачи"))
                    }
                )
                put(
                    "status",
                    buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put(
                            "enum",
                            buildJsonObject {
                                put("type", JsonPrimitive("array"))
                                put(
                                    "items",
                                    buildJsonObject {
                                        put("type", JsonPrimitive("string"))
                                    }
                                )
                                put(
                                    "values",
                                    JsonArray(
                                        listOf(
                                            JsonPrimitive("pending"),
                                            JsonPrimitive("in_progress"),
                                            JsonPrimitive("completed"),
                                            JsonPrimitive("cancelled")
                                        )
                                    )
                                )
                            }
                        )
                        put("description", JsonPrimitive("Новый статус задачи: pending, in_progress, completed, cancelled"))
                    }
                )
            },
            required = listOf("task_id", "status")
        )
    ) { request ->
        val taskId = request.arguments["task_id"]?.jsonPrimitive?.content
        val statusValue = request.arguments["status"]?.jsonPrimitive?.content

        if (taskId == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Параметр 'task_id' обязателен.")),
                isError = true
            )
        }

        if (statusValue == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Параметр 'status' обязателен.")),
                isError = true
            )
        }

        // Валидация статуса
        val taskStatus = try {
            TaskStatus.fromValue(statusValue)
        } catch (e: IllegalArgumentException) {
            return@addTool CallToolResult(
                content = listOf(
                    TextContent(
                        "Неверное значение статуса '$statusValue'. " +
                                "Допустимые значения: pending, in_progress, completed, cancelled"
                    )
                ),
                isError = true
            )
        }

        // Получаем текущий план
        when (val currentPlanResult = planService.getCurrentPlan()) {
            is Result.Success -> {
                val currentPlan = currentPlanResult.data
                if (currentPlan == null) {
                    return@addTool CallToolResult(
                        content = listOf(TextContent("Нет активного плана. Создайте план с помощью create_plan.")),
                        isError = true
                    )
                }

                // Проверяем существование задачи
                val task = currentPlan.findTaskById(taskId)
                if (task == null) {
                    return@addTool CallToolResult(
                        content = listOf(
                            TextContent(
                                "Задача с ID '$taskId' не найдена в плане '${currentPlan.name}'. " +
                                        "Доступные задачи: ${currentPlan.tasks.joinToString(", ") { it.id }}"
                            )
                        ),
                        isError = true
                    )
                }

                // Обновляем статус
                when (val updateResult = planService.updateTaskStatus(currentPlan.id, taskId, taskStatus)) {
                    is Result.Success -> {
                        val updatedPlan = updateResult.data
                        val updatedTask = updatedPlan.findTaskById(taskId)!!
                        val tasksInfo = updatedPlan.tasks.joinToString("\n") { t ->
                            val marker = if (t.id == taskId) "→" else " "
                            "$marker - [${if (t.isCompleted()) "x" else " "}] ${t.id}: ${t.title} (${t.status.value})"
                        }

                        CallToolResult(
                            content = listOf(
                                TextContent(
                                    text = """
                                        Статус задачи успешно обновлен!
                                        
                                        План: ${updatedPlan.name}
                                        Задача: ${updatedTask.title}
                                        Старый статус: ${task.status.value}
                                        Новый статус: ${updatedTask.status.value}
                                        
                                        Все задачи:
                                        $tasksInfo
                                        
                                        Прогресс: ${(updatedPlan.getProgress() * 100).toInt()}% (${updatedPlan.getCompletedTasksCount()}/${updatedPlan.getTotalTasksCount()})
                                    """.trimIndent()
                                )
                            )
                        )
                    }
                    is Result.Error -> {
                        CallToolResult(
                            content = listOf(
                                TextContent(text = "Ошибка при обновлении статуса: ${updateResult.error.message}")
                            ),
                            isError = true
                        )
                    }
                }
            }
            is Result.Error -> {
                CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка при получении текущего плана: ${currentPlanResult.error.message}")
                    ),
                    isError = true
                )
            }
        }
    }
}

