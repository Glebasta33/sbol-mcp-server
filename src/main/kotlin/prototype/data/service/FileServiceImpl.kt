package prototype.data.service

import prototype.core.config.AppConfig
import prototype.core.result.AppError
import prototype.core.result.Result
import prototype.domain.service.FileService
import java.io.File

/**
 * Реализация сервиса для работы с файлами
 */
class FileServiceImpl : FileService {
    
    override fun readFile(path: String): Result<String> {
        return try {
            val file = File(path)
            
            if (!file.exists()) {
                return Result.Error(AppError.FileNotFound(path))
            }
            
            val content = file.readText(Charsets.UTF_8)
            Result.Success(content)
            
        } catch (e: Exception) {
            Result.Error(AppError.FileReadError(path, e))
        }
    }
    
    override fun fileExists(path: String): Boolean {
        return File(path).exists()
    }
    
    override fun readPromptFile(fileName: String): Result<String> {
        val fullPath = "${AppConfig.PROMPTS_BASE_PATH}/$fileName"
        return readFile(fullPath)
    }

    override fun readPromptFile(mdFile: MdFiles): Result<String> {
        return readFile(mdFile.path)
    }

    override fun MdFiles.readPromptFileEx(): Result<String> {
        return readFile(path)
    }

}

enum class MdFiles(val path:String){
    TEST(path = "sbol-mcp-server/src/main/kotlin/prompts/test.md"),
    DATA(path = "sbol-mcp-server/src/main/kotlin/prompts/data-domain-layer.md"),
    VIEW("sbol-mcp-server/src/main/kotlin/prompts/view.md")
}

