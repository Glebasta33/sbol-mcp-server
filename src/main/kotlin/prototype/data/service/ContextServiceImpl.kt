package prototype.data.service

import prototype.core.config.AppConfig
import prototype.core.result.Result
import prototype.domain.model.ToolContext
import prototype.domain.service.ContextService
import prototype.domain.service.FileService

/**
 * Реализация сервиса для работы с контекстами
 */
class ContextServiceImpl(
    private val fileService: FileService
) : ContextService {
    
    override fun getDataDomainContext(serviceName: String?): Result<ToolContext> {
        return fileService.readRes(MdFiles.DATA)
            .map { content ->
                if (serviceName.isNullOrBlank()) {
                    ToolContext.withoutServiceName(content)
                } else {
                    ToolContext.withServiceName(content, serviceName)
                }
            }
    }
}

