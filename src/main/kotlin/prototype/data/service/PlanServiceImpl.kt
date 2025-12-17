package prototype.data.service

import prototype.core.config.AppConfig
import prototype.core.result.Result
import prototype.data.parser.MarkdownPlanParser
import prototype.domain.service.FileService
import prototype.todo.domain.model.Task
import prototype.todo.domain.model.TaskPlan
import prototype.todo.domain.model.TaskStatus
import prototype.todo.domain.service.PlanService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Реализация сервиса для управления планами и задачами
 *
 * Отвечает за:
 * - Создание MD файлов планов в .cursor/plans/
 * - Парсинг MD файлов в объекты TaskPlan
 * - Обновление статусов задач в MD файлах
 * - Поиск текущего активного плана
 */
class PlanServiceImpl(
    private val fileService: FileService
) : PlanService {
    
    private val plansDirectory: String = AppConfig.PLANS_BASE_PATH
    
    override fun createPlan(
        name: String,
        description: String,
        tasks: List<String>
    ): Result<TaskPlan> {
        try {
            // Создать директорию для планов, если её нет
            if (!fileService.directoryExists(plansDirectory)) {
                val createDirResult = fileService.createDirectory(plansDirectory)
                if (createDirResult is Result.Error) {
                    return Result.failure("Failed to create plans directory: ${createDirResult.error}")
                }
            }
            
            // Деактивировать все существующие планы перед созданием нового
            val deactivateResult = deactivateAllPlans()
            if (deactivateResult is Result.Error) {
                return Result.failure("Failed to deactivate existing plans: ${deactivateResult.error}")
            }
            
            // Генерировать уникальный ID для плана
            val planId = generatePlanId()
            val timestamp = LocalDateTime.now()
            
            // Создать список задач с уникальными ID
            val planTasks = tasks.mapIndexed { index, taskTitle ->
                Task(
                    id = "task-${index + 1}",
                    title = taskTitle,
                    status = TaskStatus.PENDING,
                    order = index + 1
                )
            }
            
            // Создать объект плана
            val fileName = generateFileName(timestamp, planId)
            val filePath = "$plansDirectory/$fileName"
            
            val plan = TaskPlan(
                id = planId,
                name = name,
                description = description,
                createdAt = timestamp,
                status = "В работе",
                isActive = true,
                tasks = planTasks,
                filePath = filePath
            )
            
            // Сгенерировать MD контент
            val mdContent = MarkdownPlanParser.generate(plan)
            
            // Записать файл
            val writeResult = fileService.writeFile(filePath, mdContent)
            
            return when (writeResult) {
                is Result.Success -> Result.success(plan)
                is Result.Error -> Result.failure("Failed to write plan file: ${writeResult.error}")
            }
            
        } catch (e: Exception) {
            return Result.failure("Failed to create plan: ${e.message}")
        }
    }
    
    override fun getCurrentPlan(): Result<TaskPlan?> {
        try {
            // Получить все планы
            val allPlansResult = getAllPlans()
            
            return when (allPlansResult) {
                is Result.Success -> {
                    val plans = allPlansResult.data
                    
                    if (plans.isEmpty()) {
                        return Result.success(null)
                    }
                    
                    // Найти план с isActive == true
                    val activePlan = plans.find { it.isActive }
                    
                    Result.success(activePlan)
                }
                is Result.Error -> Result.failure("Failed to get current plan: ${allPlansResult.error}")
            }
            
        } catch (e: Exception) {
            return Result.failure("Failed to get current plan: ${e.message}")
        }
    }
    
    override fun updateTaskStatus(
        planId: String,
        taskId: String,
        status: TaskStatus
    ): Result<TaskPlan> {
        try {
            // Найти план по ID
            val planResult = findPlanById(planId)
            
            return when (planResult) {
                is Result.Success -> {
                    val plan = planResult.data
                        ?: return Result.failure("Plan with ID $planId not found")
                    
                    // Найти задачу в плане
                    val task = plan.findTaskById(taskId)
                        ?: return Result.failure("Task with ID $taskId not found in plan $planId")
                    
                    // Создать обновленный список задач
                    val updatedTasks = plan.tasks.map { currentTask ->
                        if (currentTask.id == taskId) {
                            currentTask.copy(status = status)
                        } else {
                            currentTask
                        }
                    }
                    
                    // Обновить статус плана, если все задачи завершены
                    val updatedStatus = if (updatedTasks.all { it.isCompleted() }) {
                        "Завершен"
                    } else {
                        plan.status
                    }
                    
                    // Создать обновленный план
                    val updatedPlan = plan.copy(
                        tasks = updatedTasks,
                        status = updatedStatus
                    )
                    
                    // Сгенерировать MD контент
                    val mdContent = MarkdownPlanParser.generate(updatedPlan)
                    
                    // Записать файл
                    val writeResult = fileService.writeFile(plan.filePath, mdContent)
                    
                    when (writeResult) {
                        is Result.Success -> Result.success(updatedPlan)
                        is Result.Error -> Result.failure("Failed to update plan file: ${writeResult.error}")
                    }
                }
                is Result.Error -> Result.failure("Failed to find plan: ${planResult.error}")
            }
            
        } catch (e: Exception) {
            return Result.failure("Failed to update task status: ${e.message}")
        }
    }
    
    override fun getAllPlans(): Result<List<TaskPlan>> {
        try {
            // Проверить, существует ли директория с планами
            if (!fileService.directoryExists(plansDirectory)) {
                return Result.success(emptyList())
            }
            
            // Получить список всех файлов в директории
            val filesResult = fileService.listFiles(plansDirectory)
            
            return when (filesResult) {
                is Result.Success -> {
                    val files = filesResult.data
                    
                    // Фильтровать только MD файлы
                    val mdFiles = files.filter { it.endsWith(".md") }
                    
                    // Парсить каждый файл в TaskPlan
                    val plans = mdFiles.mapNotNull { filePath ->
                        val readResult = fileService.readFile(filePath)
                        
                        when (readResult) {
                            is Result.Success -> {
                                val parseResult = MarkdownPlanParser.parse(readResult.data, filePath)
                                
                                when (parseResult) {
                                    is Result.Success -> parseResult.data
                                    is Result.Error -> {
                                        // Логировать ошибку, но продолжить обработку других файлов
                                        System.err.println("Warning: Failed to parse plan file $filePath: ${parseResult.error}")
                                        null
                                    }
                                }
                            }
                            is Result.Error -> {
                                System.err.println("Warning: Failed to read plan file $filePath: ${readResult.error}")
                                null
                            }
                        }
                    }
                    
                    Result.success(plans)
                }
                is Result.Error -> Result.failure("Failed to list plan files: ${filesResult.error}")
            }
            
        } catch (e: Exception) {
            return Result.failure("Failed to get all plans: ${e.message}")
        }
    }
    
    override fun deletePlan(planId: String): Result<Unit> {
        try {
            // Найти план по ID
            val planResult = findPlanById(planId)
            
            return when (planResult) {
                is Result.Success -> {
                    val plan = planResult.data
                        ?: return Result.failure("Plan with ID $planId not found")
                    
                    // Удалить файл
                    fileService.deleteFile(plan.filePath)
                }
                is Result.Error -> Result.failure("Failed to find plan: ${planResult.error}")
            }
            
        } catch (e: Exception) {
            return Result.failure("Failed to delete plan: ${e.message}")
        }
    }
    
    override fun setActivePlan(planId: String): Result<TaskPlan> {
        try {
            // Получить все планы
            val allPlansResult = getAllPlans()
            
            return when (allPlansResult) {
                is Result.Success -> {
                    val allPlans = allPlansResult.data
                    
                    // Найти целевой план по ID
                    val targetPlan = allPlans.find { it.id == planId }
                        ?: return Result.failure("Plan with ID $planId not found")
                    
                    // Обновить статус isActive для всех планов
                    val updatedPlans = allPlans.map { plan ->
                        plan.copy(isActive = plan.id == planId)
                    }
                    
                    // Записать обновленные планы в MD файлы
                    updatedPlans.forEach { plan ->
                        val mdContent = MarkdownPlanParser.generate(plan)
                        val writeResult = fileService.writeFile(plan.filePath, mdContent)
                        
                        if (writeResult is Result.Error) {
                            return Result.failure("Failed to update plan file ${plan.filePath}: ${writeResult.error}")
                        }
                    }
                    
                    // Вернуть активированный план
                    val activatedPlan = updatedPlans.find { it.id == planId }!!
                    Result.success(activatedPlan)
                }
                is Result.Error -> Result.failure("Failed to get plans: ${allPlansResult.error}")
            }
            
        } catch (e: Exception) {
            return Result.failure("Failed to set active plan: ${e.message}")
        }
    }
    
    /**
     * Найти план по ID
     */
    private fun findPlanById(planId: String): Result<TaskPlan?> {
        val allPlansResult = getAllPlans()
        
        return when (allPlansResult) {
            is Result.Success -> {
                val plan = allPlansResult.data.find { it.id == planId }
                Result.success(plan)
            }
            is Result.Error -> Result.failure("Failed to find plan: ${allPlansResult.error}")
        }
    }
    
    /**
     * Деактивировать все существующие планы (установить isActive = false)
     */
    private fun deactivateAllPlans(): Result<Unit> {
        val allPlansResult = getAllPlans()
        
        return when (allPlansResult) {
            is Result.Success -> {
                val allPlans = allPlansResult.data
                
                // Обновить только те планы, которые активны
                val activePlans = allPlans.filter { it.isActive }
                
                activePlans.forEach { plan ->
                    val deactivatedPlan = plan.copy(isActive = false)
                    val mdContent = MarkdownPlanParser.generate(deactivatedPlan)
                    val writeResult = fileService.writeFile(plan.filePath, mdContent)
                    
                    if (writeResult is Result.Error) {
                        return Result.failure("Failed to deactivate plan ${plan.id}: ${writeResult.error}")
                    }
                }
                
                Result.success(Unit)
            }
            is Result.Error -> Result.failure("Failed to get plans: ${allPlansResult.error}")
        }
    }
    
    /**
     * Генерировать уникальный ID для плана
     */
    private fun generatePlanId(): String {
        return "plan-${UUID.randomUUID().toString().substring(0, 8)}"
    }
    
    /**
     * Генерировать имя файла для плана
     * Формат: plan-YYYYMMDD-HHMMSS-{id}.md
     */
    private fun generateFileName(timestamp: LocalDateTime, planId: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        val dateTimeString = timestamp.format(formatter)
        return "plan-$dateTimeString-$planId.md"
    }
}

