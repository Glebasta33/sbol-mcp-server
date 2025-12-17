package prototype.data.parser

import prototype.core.result.Result
import prototype.todo.domain.model.Task
import prototype.todo.domain.model.TaskPlan
import prototype.todo.domain.model.TaskStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Утилита для парсинга и генерации MD файлов с планами задач
 *
 * Формат MD файла:
 * ```
 * # План: Feature Implementation
 *
 * **Создан:** 2025-12-16 15:30:00
 * **ID:** plan-abc123
 * **Статус:** В работе
 * **Active:** true
 *
 * ## Описание
 * Краткое описание плана и его целей.
 *
 * ## Задачи
 * - [ ] task-1: Создать доменные модели
 * - [x] task-2: Реализовать сервисы
 * ```
 */
object MarkdownPlanParser {
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    private val planTitleRegex = Regex("""^#\s+План:\s+(.+)$""")
    private val createdAtRegex = Regex("""\*\*Создан:\*\*\s+(.+)$""")
    private val idRegex = Regex("""\*\*ID:\*\*\s+(.+)$""")
    private val statusRegex = Regex("""\*\*Статус:\*\*\s+(.+)$""")
    private val activeRegex = Regex("""\*\*Active:\*\*\s+(.+)$""")
    private val descriptionHeaderRegex = Regex("""^##\s+Описание$""")
    private val tasksHeaderRegex = Regex("""^##\s+Задачи$""")
    private val taskRegex = Regex("""^-\s+\[([^\]]+)\]\s+([^:]+):\s+(.+?)(?:\s+\(([^)]+)\))?$""")
    
    /**
     * Парсит MD файл в объект TaskPlan
     *
     * @param content Содержимое MD файла
     * @param filePath Путь к файлу
     * @return Result с TaskPlan или ошибкой парсинга
     */
    fun parse(content: String, filePath: String): Result<TaskPlan> {
        return try {
            val lines = content.lines().map { it.trim() }
            
            val name = extractPlanName(lines)
            val createdAt = extractCreatedAt(lines)
            val id = extractId(lines)
            val status = extractStatus(lines)
            val isActive = extractActive(lines)
            val description = extractDescription(lines)
            val tasks = extractTasks(lines)
            
            val plan = TaskPlan(
                id = id,
                name = name,
                description = description,
                createdAt = createdAt,
                status = status,
                isActive = isActive,
                tasks = tasks,
                filePath = filePath
            )
            
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure("Failed to parse plan from markdown: ${e.message}")
        }
    }
    
    /**
     * Генерирует MD файл из объекта TaskPlan
     *
     * @param plan План задач
     * @return Содержимое MD файла
     */
    fun generate(plan: TaskPlan): String {
        return buildString {
            // Заголовок плана
            appendLine("# План: ${plan.name}")
            appendLine()
            
            // Метаданные
            appendLine("**Создан:** ${plan.createdAt.format(dateTimeFormatter)}")
            appendLine("**ID:** ${plan.id}")
            appendLine("**Статус:** ${plan.status}")
            appendLine("**Active:** ${plan.isActive}")
            appendLine()
            
            // Описание
            appendLine("## Описание")
            appendLine(plan.description)
            appendLine()
            
            // Задачи
            appendLine("## Задачи")
            plan.tasks.forEach { task ->
                val checkbox = when {
                    task.isCompleted() -> "[x]"
                    task.isPending() -> "[ ]"
                    task.isInProgress() -> "[→]"
                    task.isCancelled() -> "[c]"
                    else -> "[ ]"
                }
                // ✅ ДОБАВИТЬ: Сохранять статус в скобках
                appendLine("- $checkbox ${task.id}: ${task.title} (${task.status.value})")
            }
        }
    }
    
    private fun extractPlanName(lines: List<String>): String {
        val line = lines.find { planTitleRegex.matches(it) }
            ?: throw IllegalArgumentException("Plan title not found")
        
        val match = planTitleRegex.find(line)
            ?: throw IllegalArgumentException("Failed to extract plan name")
        
        return match.groupValues[1].trim()
    }
    
    private fun extractCreatedAt(lines: List<String>): LocalDateTime {
        val line = lines.find { createdAtRegex.matches(it) }
            ?: throw IllegalArgumentException("Created date not found")
        
        val match = createdAtRegex.find(line)
            ?: throw IllegalArgumentException("Failed to extract created date")
        
        val dateString = match.groupValues[1].trim()
        
        return try {
            LocalDateTime.parse(dateString, dateTimeFormatter)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Invalid date format: $dateString", e)
        }
    }
    
    private fun extractId(lines: List<String>): String {
        val line = lines.find { idRegex.matches(it) }
            ?: throw IllegalArgumentException("Plan ID not found")
        
        val match = idRegex.find(line)
            ?: throw IllegalArgumentException("Failed to extract plan ID")
        
        return match.groupValues[1].trim()
    }
    
    private fun extractStatus(lines: List<String>): String {
        val line = lines.find { statusRegex.matches(it) }
            ?: throw IllegalArgumentException("Plan status not found")
        
        val match = statusRegex.find(line)
            ?: throw IllegalArgumentException("Failed to extract plan status")
        
        return match.groupValues[1].trim()
    }
    
    private fun extractActive(lines: List<String>): Boolean {
        val line = lines.find { activeRegex.matches(it) }
            ?: return false // Обратная совместимость: если поле отсутствует, возвращаем false
        
        val match = activeRegex.find(line)
            ?: return false
        
        val activeString = match.groupValues[1].trim()
        return activeString.equals("true", ignoreCase = true)
    }
    
    private fun extractDescription(lines: List<String>): String {
        val descriptionStartIndex = lines.indexOfFirst { descriptionHeaderRegex.matches(it) }
        if (descriptionStartIndex == -1) {
            throw IllegalArgumentException("Description section not found")
        }
        
        val tasksStartIndex = lines.indexOfFirst { tasksHeaderRegex.matches(it) }
        if (tasksStartIndex == -1) {
            throw IllegalArgumentException("Tasks section not found")
        }
        
        val descriptionLines = lines.subList(descriptionStartIndex + 1, tasksStartIndex)
            .filter { it.isNotBlank() }
        
        return descriptionLines.joinToString("\n").trim()
    }
    
    private fun extractTasks(lines: List<String>): List<Task> {
        val tasksStartIndex = lines.indexOfFirst { tasksHeaderRegex.matches(it) }
        if (tasksStartIndex == -1) {
            throw IllegalArgumentException("Tasks section not found")
        }
        
        val tasks = mutableListOf<Task>()
        var order = 1
        
        for (i in tasksStartIndex + 1 until lines.size) {
            val line = lines[i]
            val match = taskRegex.find(line)
            
            if (match != null) {
                val checkboxSymbol = match.groupValues[1].trim()
                val taskId = match.groupValues[2].trim()
                val taskTitle = match.groupValues[3].trim()
                val statusString = match.groupValues.getOrNull(4)?.trim()
                
                // Определить статус по приоритету: статус в скобках > статус по checkbox
                val status = when {
                    // Если статус указан явно в скобках - использовать его
                    !statusString.isNullOrBlank() -> {
                        try {
                            TaskStatus.fromValue(statusString)
                        } catch (e: IllegalArgumentException) {
                            // Если статус неизвестен - fallback на checkbox
                            parseStatusFromCheckbox(checkboxSymbol)
                        }
                    }
                    // Иначе определить по checkbox
                    else -> parseStatusFromCheckbox(checkboxSymbol)
                }
                
                tasks.add(
                    Task(
                        id = taskId,
                        title = taskTitle,
                        status = status,
                        order = order++
                    )
                )
            }
        }
        
        if (tasks.isEmpty()) {
            throw IllegalArgumentException("No tasks found in plan")
        }
        
        return tasks
    }
    
    /**
     * Парсить статус по символу в checkbox
     */
    private fun parseStatusFromCheckbox(checkboxSymbol: String): TaskStatus {
        return when (checkboxSymbol) {
            "x" -> TaskStatus.COMPLETED
            "→" -> TaskStatus.IN_PROGRESS
            "c" -> TaskStatus.CANCELLED
            " ", "" -> TaskStatus.PENDING
            else -> TaskStatus.PENDING // fallback для неизвестных символов
        }
    }
}

