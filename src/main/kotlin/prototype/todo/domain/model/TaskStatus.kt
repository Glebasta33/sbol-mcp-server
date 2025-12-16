package prototype.todo.domain.model

/**
 * Статус задачи в плане
 */
enum class TaskStatus(val value: String) {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        fun fromValue(value: String): TaskStatus {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown TaskStatus value: $value")
        }
    }
}

