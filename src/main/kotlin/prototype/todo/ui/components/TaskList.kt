package prototype.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import prototype.todo.domain.model.Task
import prototype.todo.domain.model.TaskStatus

/**
 * Компонент списка задач
 *
 * Отображает список задач с чекбоксами и индикаторами статуса
 *
 * @param tasks Список задач для отображения
 * @param onTaskClick Callback при клике на чекбокс задачи
 * @param onTaskStatusChange Callback для изменения статуса задачи
 * @param modifier Modifier для кастомизации компонента
 */
@Composable
fun TaskList(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, TaskStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет задач",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onCheckboxClick = { onTaskClick(task.id) },
                    onStatusChange = { newStatus -> onTaskStatusChange(task.id, newStatus) }
                )
            }
        }
    }
}

/**
 * Компонент отдельной задачи
 *
 * @param task Задача для отображения
 * @param onCheckboxClick Callback при клике на чекбокс
 * @param onStatusChange Callback для изменения статуса
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskItem(
    task: Task,
    onCheckboxClick: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val statusColor = getStatusColor(task.status)
    val statusIcon = getStatusIcon(task.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Чекбокс
            Checkbox(
                checked = task.isCompleted(),
                onCheckedChange = { onCheckboxClick() },
                enabled = !task.isCancelled()
            )

            // Иконка статуса
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor)
            )

            // Название задачи
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.isCompleted()) TextDecoration.LineThrough else null,
                color = if (task.isCancelled()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )

            // Иконка статуса
            Icon(
                imageVector = statusIcon,
                contentDescription = task.status.value,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )

            // Меню для изменения статуса
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Опции",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ожидает") },
                        onClick = {
                            onStatusChange(TaskStatus.PENDING)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = getStatusColor(TaskStatus.PENDING)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("В работе") },
                        onClick = {
                            onStatusChange(TaskStatus.IN_PROGRESS)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = getStatusColor(TaskStatus.IN_PROGRESS)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Завершена") },
                        onClick = {
                            onStatusChange(TaskStatus.COMPLETED)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = getStatusColor(TaskStatus.COMPLETED)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Отменена") },
                        onClick = {
                            onStatusChange(TaskStatus.CANCELLED)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = getStatusColor(TaskStatus.CANCELLED)
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Получить цвет для статуса задачи
 */
private fun getStatusColor(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.PENDING -> Color(0xFFFFB74D) // Orange 300
        TaskStatus.IN_PROGRESS -> Color(0xFF64B5F6) // Blue 300
        TaskStatus.COMPLETED -> Color(0xFF81C784) // Green 300
        TaskStatus.CANCELLED -> Color(0xFFE57373) // Red 300
    }
}

/**
 * Получить иконку для статуса задачи
 */
private fun getStatusIcon(status: TaskStatus) = when (status) {
    TaskStatus.PENDING -> Icons.Default.DateRange
    TaskStatus.IN_PROGRESS -> Icons.Default.PlayArrow
    TaskStatus.COMPLETED -> Icons.Default.Check
    TaskStatus.CANCELLED -> Icons.Default.Close
}

