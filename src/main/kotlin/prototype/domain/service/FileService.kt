package prototype.domain.service

import prototype.core.result.Result
import prototype.data.service.MdFiles

/**
 * Интерфейс сервиса для работы с файлами
 */
interface FileService {
    /**
     * Читает содержимое файла по указанному пути
     * @param path путь к файлу относительно корня проекта
     * @return Result с содержимым файла или ошибкой
     */
    fun readFile(path: String): Result<String>
    
    /**
     * Проверяет существование файла
     * @param path путь к файлу
     * @return true если файл существует
     */
    fun fileExists(path: String): Boolean
    
    /**
     * Читает markdown файл из директории prompts
     * @param fileName имя файла (например, "data-domain-layer.md")
     * @return Result с содержимым файла или ошибкой
     */
    fun readPromptFile(fileName: String): Result<String>

    fun readPromptFile(mdFile: MdFiles): Result<String>

    fun MdFiles.readPromptFileEx(): Result<String>
}

