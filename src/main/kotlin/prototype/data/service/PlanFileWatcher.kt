package prototype.data.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import prototype.core.config.AppConfig
import prototype.core.result.Result
import prototype.todo.domain.model.TaskPlan
import prototype.todo.domain.service.PlanService
import java.nio.file.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.exists

/**
 * Сервис для отслеживания изменений файлов планов в реальном времени
 *
 * Использует WatchService из Java NIO для мониторинга директории .cursor/plans/
 * и уведомляет подписчиков об изменениях через StateFlow.
 *
 * Особенности:
 * - Автоматически перезагружает план при изменении MD файла
 * - Предотвращает бесконечные циклы обновлений при записи из UI
 * - Работает в отдельной корутине и не блокирует основной поток
 */
class PlanFileWatcher(
    private val planService: PlanService,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    
    private val plansDirectory: Path = Paths.get(AppConfig.PLANS_BASE_PATH)
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    
    // StateFlow для текущего плана
    private val _currentPlan = MutableStateFlow<TaskPlan?>(null)
    val currentPlan: StateFlow<TaskPlan?> = _currentPlan.asStateFlow()
    
    // StateFlow для события перезагрузки (инкрементируется при каждом изменении)
    private val _reloadTrigger = MutableStateFlow(0)
    val reloadTrigger: StateFlow<Int> = _reloadTrigger.asStateFlow()
    
    // Флаг для предотвращения циклов обновлений
    private val isUpdatingFromUI = AtomicBoolean(false)
    
    // Флаг активности watcher'а
    private val isActive = AtomicBoolean(false)
    
    // Job для корутины watcher'а
    private var watcherJob: Job? = null
    
    /**
     * Запустить отслеживание изменений файлов
     */
    fun start() {
        if (isActive.getAndSet(true)) {
            println("PlanFileWatcher: Already running")
            return
        }
        
        // Создать директорию для планов, если её нет
        if (!plansDirectory.exists()) {
            try {
                Files.createDirectories(plansDirectory)
                println("PlanFileWatcher: Created plans directory at $plansDirectory")
            } catch (e: Exception) {
                println("PlanFileWatcher: Failed to create plans directory: ${e.message}")
                isActive.set(false)
                return
            }
        }
        
        // Зарегистрировать директорию для мониторинга
        try {
            plansDirectory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )
            println("PlanFileWatcher: Started watching directory: $plansDirectory")
        } catch (e: Exception) {
            println("PlanFileWatcher: Failed to register watch service: ${e.message}")
            isActive.set(false)
            return
        }
        
        // Загрузить текущий план при старте
        loadCurrentPlan()
        
        // Запустить корутину для обработки событий
        watcherJob = coroutineScope.launch {
            watchLoop()
        }
    }
    
    /**
     * Остановить отслеживание изменений файлов
     */
    fun stop() {
        if (!isActive.getAndSet(false)) {
            println("PlanFileWatcher: Already stopped")
            return
        }
        
        watcherJob?.cancel()
        watcherJob = null
        
        try {
            watchService.close()
            println("PlanFileWatcher: Stopped watching")
        } catch (e: Exception) {
            println("PlanFileWatcher: Error closing watch service: ${e.message}")
        }
    }
    
    /**
     * Установить флаг обновления из UI перед записью в файл
     * Это предотвращает циклическое обновление UI при изменении файла из UI
     */
    fun setUpdatingFromUI(updating: Boolean) {
        isUpdatingFromUI.set(updating)
        println("PlanFileWatcher: UI update flag set to $updating")
    }
    
    /**
     * Вручную перезагрузить текущий план
     */
    fun reloadPlan() {
        loadCurrentPlan()
    }
    
    /**
     * Основной цикл отслеживания событий файловой системы
     */
    private suspend fun watchLoop() {
        while (isActive.get()) {
            try {
                // Ожидать события от WatchService с таймаутом
                val key = withContext(Dispatchers.IO) {
                    watchService.poll(1, java.util.concurrent.TimeUnit.SECONDS)
                }
                
                if (key == null) {
                    // Таймаут - продолжить ожидание
                    continue
                }
                
                // Обработать все события в ключе
                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    
                    // Пропустить OVERFLOW события
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue
                    }
                    
                    @Suppress("UNCHECKED_CAST")
                    val filename = (event as WatchEvent<Path>).context()
                    
                    // Обработать только MD файлы
                    if (filename.toString().endsWith(".md")) {
                        handleFileEvent(kind, filename)
                    }
                }
                
                // Сбросить ключ для получения следующих событий
                val valid = key.reset()
                if (!valid) {
                    println("PlanFileWatcher: Watch key no longer valid, stopping")
                    break
                }
                
            } catch (e: CancellationException) {
                println("PlanFileWatcher: Watch loop cancelled")
                break
            } catch (e: Exception) {
                println("PlanFileWatcher: Error in watch loop: ${e.message}")
                // Продолжить работу после ошибки
            }
        }
    }
    
    /**
     * Обработать событие изменения файла
     */
    private suspend fun handleFileEvent(kind: WatchEvent.Kind<*>, filename: Path) {
        // Проверить флаг обновления из UI
        if (isUpdatingFromUI.get()) {
            println("PlanFileWatcher: Ignoring event for $filename (update from UI)")
            // Сбросить флаг после небольшой задержки
            delay(100)
            isUpdatingFromUI.set(false)
            return
        }
        
        when (kind) {
            StandardWatchEventKinds.ENTRY_CREATE -> {
                println("PlanFileWatcher: File created: $filename")
                loadCurrentPlan()
            }
            StandardWatchEventKinds.ENTRY_MODIFY -> {
                println("PlanFileWatcher: File modified: $filename")
                loadCurrentPlan()
            }
            StandardWatchEventKinds.ENTRY_DELETE -> {
                println("PlanFileWatcher: File deleted: $filename")
                loadCurrentPlan()
            }
        }
    }
    
    /**
     * Загрузить текущий план из файловой системы
     */
    private fun loadCurrentPlan() {
        try {
            val result = planService.getCurrentPlan()
            
            when (result) {
                is Result.Success -> {
                    val plan = result.data
                    _currentPlan.value = plan
                    
                    // Инкрементировать счетчик перезагрузок для уведомления подписчиков
                    _reloadTrigger.value += 1
                    
                    if (plan != null) {
                        println("PlanFileWatcher: Loaded plan '${plan.name}' with ${plan.tasks.size} tasks")
                    } else {
                        println("PlanFileWatcher: No active plan found")
                    }
                }
                is Result.Error -> {
                    println("PlanFileWatcher: Error loading plan: ${result.error}")
                    _currentPlan.value = null
                }
            }
        } catch (e: Exception) {
            println("PlanFileWatcher: Exception loading plan: ${e.message}")
            _currentPlan.value = null
        }
    }
}

