package prototype.core.result

/**
 * Sealed class для обработки результатов операций
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
    
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }
    
    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        
        fun <T> failure(message: String): Result<T> = Error(AppError.Generic(message))
        
        fun <T> failure(error: AppError): Result<T> = Error(error)
    }
}

/**
 * Типы ошибок в приложении
 */
sealed class AppError(val message: String, val cause: Throwable? = null) {
    class FileNotFound(path: String) : AppError("Файл не найден: $path")
    class FileReadError(path: String, cause: Throwable) : AppError("Ошибка чтения файла: $path", cause)
    class FileWriteError(path: String, cause: Throwable) : AppError("Ошибка записи файла: $path", cause)
    class InvalidArgument(argumentName: String, reason: String) : AppError("Некорректный аргумент '$argumentName': $reason")
    class Generic(message: String) : AppError(message)
    class Unknown(cause: Throwable) : AppError("Неизвестная ошибка: ${cause.message}", cause)
}

