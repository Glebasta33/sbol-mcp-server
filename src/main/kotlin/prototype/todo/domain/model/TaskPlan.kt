package prototype.todo.domain.model

import java.time.LocalDateTime

/**
 * Модель плана с задачами
 *
 * @property id Уникальный идентификатор плана
 * @property name Название плана
 * @property description Описание плана и его целей
 * @property createdAt Дата и время создания плана
 * @property status Статус плана (например, "В работе", "Завершен")
 * @property isActive Флаг активного плана (только один план может быть активным одновременно)
 * @property tasks Список задач в плане
 * @property filePath Путь к MD файлу плана
 */
data class TaskPlan(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime,
    val status: String,
    val isActive: Boolean,
    val tasks: List<Task>,
    val filePath: String
) {
    fun getCompletedTasksCount(): Int = tasks.count { it.isCompleted() }
    
    fun getTotalTasksCount(): Int = tasks.size
    
    fun getProgress(): Double {
        if (tasks.isEmpty()) return 0.0
        return getCompletedTasksCount().toDouble() / getTotalTasksCount()
    }
    
    fun hasInProgressTasks(): Boolean = tasks.any { it.isInProgress() }
    
    fun isCompleted(): Boolean = tasks.isNotEmpty() && tasks.all { it.isCompleted() }
    
    fun findTaskById(taskId: String): Task? = tasks.find { it.id == taskId }
}

