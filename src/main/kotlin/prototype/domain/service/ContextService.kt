package prototype.domain.service

import prototype.core.result.Result
import prototype.domain.model.ToolContext

/**
 * Сервис для работы с контекстами (промптами)
 */
interface ContextService {
    /**
     * Получить контекст для Data & Domain слоя
     * @param serviceName опциональное имя сервиса (заменит переменную 'x' в правилах)
     * @return Result с контекстом или ошибкой
     */
    fun getDataDomainContext(serviceName: String? = null): Result<ToolContext>
}

