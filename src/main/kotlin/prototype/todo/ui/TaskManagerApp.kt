package prototype.todo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import prototype.data.service.PlanFileWatcher
import prototype.todo.domain.service.PlanService
import prototype.todo.ui.components.TaskList
import prototype.todo.ui.viewmodel.TaskViewModel
import java.time.format.DateTimeFormatter

/**
 * Главное окно Task Manager приложения
 *
 * Отображает текущий план с задачами и позволяет управлять их статусами
 *
 * @param planService Сервис для работы с планами
 * @param planFileWatcher Watcher для отслеживания изменений файлов планов
 * @param onCloseRequest Callback при закрытии окна
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerWindow(
    planService: PlanService,
    planFileWatcher: PlanFileWatcher?,
    onCloseRequest: () -> Unit
) {
    val viewModel = remember {
        TaskViewModel(
            planService = planService,
            planFileWatcher = planFileWatcher,
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
    var isDarkTheme by remember { mutableStateOf(true) }

    val colorScheme = if (isDarkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme) {
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2
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
                        // Переключатель темы
                        IconButton(onClick = { isDarkTheme = !isDarkTheme }) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "Светлая тема" else "Тёмная тема"
                            )
                        }
                        
                        // Plan selector dropdown
                        Box {
                            IconButton(onClick = { expandedPlanDropdown = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Выбрать план"
                                )
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

// Глобальное состояние для управления видимостью окна
private object UIState {
    var isRunning = false
    var showWindow: ((Boolean) -> Unit)? = null
}

/**
 * Запустить Task Manager приложение в отдельном окне
 *
 * UI запускается в отдельном daemon потоке, чтобы не блокировать MCP сервер.
 * При закрытии окна оно просто скрывается (но остаётся в памяти), MCP сервер продолжает работать.
 *
 * ВАЖНО: Окно НЕ завершает application полностью - оно просто скрывается.
 * Это предотвращает влияние на MCP сервер stdio транспорт.
 *
 * @param planService Сервис для работы с планами
 * @param planFileWatcher Watcher для отслеживания изменений файлов планов
 */
fun launchTaskManagerApp(planService: PlanService, planFileWatcher: PlanFileWatcher? = null) {
    // Если UI уже запущен, показываем окно
    if (UIState.isRunning) {
        System.err.println("Task Manager UI is already running, showing window...")
        UIState.showWindow?.invoke(true)
        return
    }

    UIState.isRunning = true
    System.err.println("Launching Task Manager UI in separate daemon thread...")

    application(exitProcessOnExit = false) {
        // Состояние видимости окна
        var isWindowVisible by mutableStateOf(true)

        // Регистрируем callback для управления видимостью извне
        UIState.showWindow = { show -> isWindowVisible = show }

        val windowState = rememberWindowState(width = 400.dp, height = 600.dp)

        // Окно остается в памяти, даже когда скрыто
        if (isWindowVisible) {
            Window(
                onCloseRequest = {
                    exitApplication()
                    System.err.println("Task Manager UI window hidden (MCP server still running)")
                },
                title = "Task Manager",
                state = windowState,
                visible = isWindowVisible
            ) {
                TaskManagerWindow(
                    planService = planService,
                    planFileWatcher = planFileWatcher,
                    onCloseRequest = {
                        exitApplication()
                        System.err.println("Task Manager UI window hidden (MCP server still running)")
                    }
                )
            }
        }
    }
    // Этот код выполнится только при полном завершении application (никогда в daemon режиме)
    UIState.isRunning = false
    UIState.showWindow = null
    System.err.println("Task Manager UI application terminated")
}

