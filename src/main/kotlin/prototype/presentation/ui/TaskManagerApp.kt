package prototype.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import prototype.presentation.ui.components.TaskList
import prototype.presentation.ui.viewmodel.TaskViewModel
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
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

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
                        IconButton(onClick = { viewModel.loadCurrentPlan() }) {
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
        }
    }
}

/**
 * Запустить Task Manager приложение в отдельном окне
 *
 * @param planService Сервис для работы с планами
 */
fun launchTaskManagerApp(planService: PlanService) {
    application {
        val windowState = rememberWindowState(width = 800.dp, height = 600.dp)
        
        Window(
            onCloseRequest = ::exitApplication,
            title = "Task Manager",
            state = windowState
        ) {
            TaskManagerWindow(
                planService = planService,
                onCloseRequest = ::exitApplication
            )
        }
    }
}

