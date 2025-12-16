package prototype.todo.domain.service

import prototype.core.result.Result
import prototype.todo.domain.model.TaskPlan
import prototype.todo.domain.model.TaskStatus

/**
 * Сервис для управления планами и задачами
 */
interface PlanService {
    
    /**
     * Создать новый план с задачами
     *
     * @param name Название плана
     * @param description Описание плана
     * @param tasks Список названий задач
     * @return Result с созданным планом или ошибкой
     */
    fun createPlan(
        name: String,
        description: String,
        tasks: List<String>
    ): Result<TaskPlan>
    
    /**
     * Получить текущий активный план
     *
     * @return Result с текущим планом или null, если нет активных планов
     */
    fun getCurrentPlan(): Result<TaskPlan?>
    
    /**
     * Обновить статус задачи в плане
     *
     * @param planId Идентификатор плана
     * @param taskId Идентификатор задачи
     * @param status Новый статус задачи
     * @return Result с обновленным планом или ошибкой
     */
    fun updateTaskStatus(
        planId: String,
        taskId: String,
        status: TaskStatus
    ): Result<TaskPlan>
    
    /**
     * Получить все планы
     *
     * @return Result со списком всех планов или ошибкой
     */
    fun getAllPlans(): Result<List<TaskPlan>>
    
    /**
     * Удалить план
     *
     * @param planId Идентификатор плана
     * @return Result с успехом или ошибкой
     */
    fun deletePlan(planId: String): Result<Unit>
}

