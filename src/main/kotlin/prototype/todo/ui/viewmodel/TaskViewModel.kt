package prototype.todo.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import prototype.core.result.Result
import prototype.todo.domain.model.TaskPlan
import prototype.todo.domain.model.TaskStatus
import prototype.todo.domain.service.PlanService

/**
 * ViewModel для управления состоянием Task Manager UI
 *
 * Управляет загрузкой плана, обновлением статусов задач и подпиской на изменения файлов
 */
class TaskViewModel(
    private val planService: PlanService,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _currentPlan = MutableStateFlow<TaskPlan?>(null)
    val currentPlan: StateFlow<TaskPlan?> = _currentPlan.asStateFlow()

    private val _allPlans = MutableStateFlow<List<TaskPlan>>(emptyList())
    val allPlans: StateFlow<List<TaskPlan>> = _allPlans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCurrentPlan()
        loadAllPlans()
    }

    /**
     * Загрузить текущий активный план
     */
    fun loadCurrentPlan() {
        coroutineScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = planService.getCurrentPlan()) {
                is Result.Success -> {
                    _currentPlan.value = result.data
                    if (result.data == null) {
                        _error.value = "Нет активных планов"
                    }
                }
                is Result.Error -> {
                    _error.value = result.error.message
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Загрузить все планы
     */
    fun loadAllPlans() {
        coroutineScope.launch {
            when (val result = planService.getAllPlans()) {
                is Result.Success -> {
                    _allPlans.value = result.data
                }
                is Result.Error -> {
                    _error.value = result.error.message
                }
            }
        }
    }

    /**
     * Установить активный план
     *
     * @param planId Идентификатор плана
     */
    fun setActivePlan(planId: String) {
        coroutineScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = planService.setActivePlan(planId)) {
                is Result.Success -> {
                    _currentPlan.value = result.data
                    loadAllPlans()
                }
                is Result.Error -> {
                    _error.value = result.error.message
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Обновить статус задачи
     *
     * @param taskId Идентификатор задачи
     * @param newStatus Новый статус задачи
     */
    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        val plan = _currentPlan.value ?: return

        coroutineScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = planService.updateTaskStatus(plan.id, taskId, newStatus)) {
                is Result.Success -> {
                    _currentPlan.value = result.data
                }
                is Result.Error -> {
                    _error.value = result.error.message
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Переключить статус задачи между PENDING и COMPLETED
     *
     * @param taskId Идентификатор задачи
     */
    fun toggleTaskCompletion(taskId: String) {
        val plan = _currentPlan.value ?: return
        val task = plan.findTaskById(taskId) ?: return

        val newStatus = if (task.isCompleted()) {
            TaskStatus.PENDING
        } else {
            TaskStatus.COMPLETED
        }

        updateTaskStatus(taskId, newStatus)
    }

    /**
     * Установить задачу в статус IN_PROGRESS
     *
     * @param taskId Идентификатор задачи
     */
    fun setTaskInProgress(taskId: String) {
        updateTaskStatus(taskId, TaskStatus.IN_PROGRESS)
    }

    /**
     * Отменить задачу
     *
     * @param taskId Идентификатор задачи
     */
    fun cancelTask(taskId: String) {
        updateTaskStatus(taskId, TaskStatus.CANCELLED)
    }

    /**
     * Удалить план
     *
     * @param planId Идентификатор плана для удаления
     */
    fun deletePlan(planId: String) {
        coroutineScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = planService.deletePlan(planId)) {
                is Result.Success -> {
                    // Перезагрузить все планы после удаления
                    loadAllPlans()
                    
                    // Если удален текущий план, загрузить новый активный план
                    if (_currentPlan.value?.id == planId) {
                        loadCurrentPlan()
                    }
                }
                is Result.Error -> {
                    _error.value = result.error.message
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Очистить ошибку
     */
    fun clearError() {
        _error.value = null
    }
}

