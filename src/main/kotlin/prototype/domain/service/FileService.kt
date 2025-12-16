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
     * Записывает содержимое в файл
     * @param path путь к файлу
     * @param content содержимое для записи
     * @return Result с успехом или ошибкой
     */
    fun writeFile(path: String, content: String): Result<Unit>
    
    /**
     * Проверяет существование файла
     * @param path путь к файлу
     * @return true если файл существует
     */
    fun fileExists(path: String): Boolean
    
    /**
     * Проверяет существование директории
     * @param path путь к директории
     * @return true если директория существует
     */
    fun directoryExists(path: String): Boolean
    
    /**
     * Создает директорию если она не существует
     * @param path путь к директории
     * @return Result с успехом или ошибкой
     */
    fun createDirectory(path: String): Result<Unit>
    
    /**
     * Список файлов в директории
     * @param path путь к директории
     * @return Result со списком файлов или ошибкой
     */
    fun listFiles(path: String): Result<List<String>>
    
    /**
     * Удаляет файл
     * @param path путь к файлу
     * @return Result с успехом или ошибкой
     */
    fun deleteFile(path: String): Result<Unit>
    
    /**
     * Читает markdown файл из директории prompts
     * @param fileName имя файла (например, "data-domain-layer.md")
     * @return Result с содержимым файла или ошибкой
     */
    fun readPromptFile(fileName: String): Result<String>

    fun readPromptFile(mdFile: MdFiles): Result<String>

    fun MdFiles.readPromptFileEx(): Result<String>
}

