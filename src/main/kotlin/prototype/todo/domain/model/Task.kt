package prototype.todo.domain.model

/**
 * Модель задачи в плане
 *
 * @property id Уникальный идентификатор задачи
 * @property title Название задачи
 * @property status Текущий статус задачи
 * @property order Порядковый номер задачи в списке
 */
data class Task(
    val id: String,
    val title: String,
    val status: TaskStatus,
    val order: Int
) {
    fun isCompleted(): Boolean = status == TaskStatus.COMPLETED
    
    fun isPending(): Boolean = status == TaskStatus.PENDING
    
    fun isInProgress(): Boolean = status == TaskStatus.IN_PROGRESS
    
    fun isCancelled(): Boolean = status == TaskStatus.CANCELLED
}

