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
    
    override fun writeFile(path: String, content: String): Result<Unit> {
        return try {
            val file = File(path)
            val parentDir = file.parentFile
            
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            
            file.writeText(content, Charsets.UTF_8)
            Result.Success(Unit)
            
        } catch (e: Exception) {
            Result.Error(AppError.FileWriteError(path, e))
        }
    }
    
    override fun fileExists(path: String): Boolean {
        return File(path).exists() && File(path).isFile
    }
    
    override fun directoryExists(path: String): Boolean {
        return File(path).exists() && File(path).isDirectory
    }
    
    override fun createDirectory(path: String): Result<Unit> {
        return try {
            val dir = File(path)
            
            if (dir.exists()) {
                if (dir.isDirectory) {
                    return Result.Success(Unit)
                } else {
                    return Result.failure("Path exists but is not a directory: $path")
                }
            }
            
            val created = dir.mkdirs()
            if (created) {
                Result.Success(Unit)
            } else {
                Result.failure("Failed to create directory: $path")
            }
            
        } catch (e: Exception) {
            Result.failure("Failed to create directory: ${e.message}")
        }
    }
    
    override fun listFiles(path: String): Result<List<String>> {
        return try {
            val dir = File(path)
            
            if (!dir.exists()) {
                return Result.failure("Directory does not exist: $path")
            }
            
            if (!dir.isDirectory) {
                return Result.failure("Path is not a directory: $path")
            }
            
            val files = dir.listFiles()?.map { it.absolutePath } ?: emptyList()
            Result.Success(files)
            
        } catch (e: Exception) {
            Result.failure("Failed to list files: ${e.message}")
        }
    }
    
    override fun deleteFile(path: String): Result<Unit> {
        return try {
            val file = File(path)
            
            if (!file.exists()) {
                return Result.failure("File does not exist: $path")
            }
            
            val deleted = file.delete()
            if (deleted) {
                Result.Success(Unit)
            } else {
                Result.failure("Failed to delete file: $path")
            }
            
        } catch (e: Exception) {
            Result.failure("Failed to delete file: ${e.message}")
        }
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

