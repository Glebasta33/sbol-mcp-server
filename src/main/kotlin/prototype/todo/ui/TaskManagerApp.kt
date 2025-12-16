package prototype.todo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import prototype.todo.ui.components.TaskList
import prototype.todo.ui.viewmodel.TaskViewModel
import prototype.todo.domain.service.PlanService
import java.time.format.DateTimeFormatter

/**
 * Главное окно Task Manager приложения
 *
 * Отображает текущий план с задачами и позволяет управлять их статусами
 *
 * @param planService Сервис для работы с планами
 * @param onCloseRequest Callback при закрытии окна
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerWindow(
    planService: PlanService,
    onCloseRequest: () -> Unit
) {
    val viewModel = remember {
        TaskViewModel(
            planService = planService,
            coroutineScope = CoroutineScope(Dispatchers.Default)
        )
    }

    val currentPlan by viewModel.currentPlan.collectAsState()
    val allPlans by viewModel.allPlans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var expandedPlanDropdown by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentPlan?.name ?: "Task Manager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            currentPlan?.let { plan ->
                                Text(
                                    text = "${plan.getCompletedTasksCount()} / ${plan.getTotalTasksCount()} задач завершено",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Plan selector dropdown
                        Box {
                            IconButton(onClick = { expandedPlanDropdown = true }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Планы",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Выбрать план"
                                    )
                                }
                            }
                            
                            DropdownMenu(
                                expanded = expandedPlanDropdown,
                                onDismissRequest = { expandedPlanDropdown = false }
                            ) {
                                if (allPlans.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Нет доступных планов") },
                                        onClick = { },
                                        enabled = false
                                    )
                                } else {
                                    allPlans.forEach { plan ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = plan.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (plan.isActive) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                        Text(
                                                            text = "${plan.getCompletedTasksCount()}/${plan.getTotalTasksCount()} • ${plan.status}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                planToDelete = plan.id
                                                                showDeleteConfirmation = true
                                                                expandedPlanDropdown = false
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Удалить план",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        if (plan.isActive) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Активный план",
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                if (!plan.isActive) {
                                                    viewModel.setActivePlan(plan.id)
                                                }
                                                expandedPlanDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        IconButton(onClick = { 
                            viewModel.loadCurrentPlan()
                            viewModel.loadAllPlans()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обновить"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // План информация
                currentPlan?.let { plan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = plan.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Создан: ${plan.createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Статус: ${plan.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Progress bar
                            LinearProgressIndicator(
                                progress = { plan.getProgress().toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                            )
                        }
                    }
                }

                // Обработка состояний загрузки и ошибок
                when {
                    isLoading && currentPlan == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null && currentPlan == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = error ?: "Неизвестная ошибка",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = { viewModel.loadCurrentPlan() }) {
                                    Text("Повторить")
                                }
                            }
                        }
                    }
                    currentPlan != null -> {
                        // Список задач
                        TaskList(
                            tasks = currentPlan!!.tasks.sortedBy { it.order },
                            onTaskClick = { taskId ->
                                viewModel.toggleTaskCompletion(taskId)
                            },
                            onTaskStatusChange = { taskId, newStatus ->
                                viewModel.updateTaskStatus(taskId, newStatus)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Snackbar для ошибок
                error?.let { errorMessage ->
                    if (currentPlan != null) {
                        Snackbar(
                            modifier = Modifier.padding(16.dp),
                            action = {
                                TextButton(onClick = { viewModel.clearError() }) {
                                    Text("OK")
                                }
                            }
                        ) {
                            Text(errorMessage)
                        }
                    }
                }
            }
            
            // Диалог подтверждения удаления
            if (showDeleteConfirmation && planToDelete != null) {
                val planName = allPlans.find { it.id == planToDelete }?.name ?: "план"
                
                AlertDialog(
                    onDismissRequest = {
                        showDeleteConfirmation = false
                        planToDelete = null
                    },
                    title = {
                        Text("Удалить план?")
                    },
                    text = {
                        Text("Вы уверены, что хотите удалить план \"$planName\"? Это действие нельзя отменить.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                planToDelete?.let { viewModel.deletePlan(it) }
                                showDeleteConfirmation = false
                                planToDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Удалить")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmation = false
                                planToDelete = null
                            }
                        ) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }
    }
}

// Флаг для отслеживания запущенного UI
private var isUIRunning = false

/**
 * Запустить Task Manager приложение в отдельном окне
 *
 * UI запускается в отдельном daemon потоке, чтобы не блокировать MCP сервер.
 * При закрытии окна завершается только UI поток, MCP сервер продолжает работать.
 *
 * @param planService Сервис для работы с планами
 */
fun launchTaskManagerApp(planService: PlanService) {
    // Проверяем, не запущен ли уже UI
    if (isUIRunning) {
        println("Task Manager UI is already running")
        return
    }
    
    isUIRunning = true
    
    // Запускаем UI в отдельном daemon потоке, чтобы не блокировать MCP сервер
    Thread {
        try {
            application {
                val windowState = rememberWindowState(width = 800.dp, height = 600.dp)
                
                Window(
                    onCloseRequest = {
                        // Завершаем только UI application, MCP сервер продолжает работать
                        exitApplication()
                    },
                    title = "Task Manager",
                    state = windowState
                ) {
                    TaskManagerWindow(
                        planService = planService,
                        onCloseRequest = {
                            // Завершаем только UI application, MCP сервер продолжает работать
                            exitApplication()
                        }
                    )
                }
            }
        } finally {
            // Сбрасываем флаг при завершении UI
            isUIRunning = false
            println("Task Manager UI closed")
        }
    }.apply {
        name = "TaskManagerUI"
        isDaemon = true
    }.start()
    
    println("Launching Task Manager UI in separate daemon thread...")
}

