---
description:
globs:
alwaysApply: false
---
# Data & Domain layers Rule for Clean Architecture

## References (Project‑Specific)
Перед генерацией учитывай, что в проекте уже есть следующие классы:

- **DTO**:
    - `ApiResponse` (`core\models\src\main\kotlin\com\core\models\data\ApiResponse.kt`)
    - `AdInfoDTO` (`feature\ads\ads-impl\src\main\kotlin\com\feature\ads\impl\data\info\dto\AdInfoDTO.kt`)

- **Domain Models**:
    - `AdInfoModel` (`feature\ads\ads-impl\src\main\kotlin\com\feature\ads\impl\domain\info\model\AdInfoModel.kt`)

- **Existing Mappers**:
    - `AdInfoMappers` (`feature\ads\ads-impl\src\main\kotlin\com\feature\ads\impl\data\info\dto\AdInfoMappers.kt`)

    - **Repository**:
    - `AdInfoRepositoryImpl` (`feature\ads\ads-impl\src\main\kotlin\com\feature\ads\impl\data\info\AdInfoRepositoryImpl.kt`)
    - `AdInfoRepository` (`feature\ads\ads-impl\src\main\kotlin\com\feature\ads\impl\domain\info\AdInfoRepository.kt`)
    - `AdsRepositoryImpl` (`feature\ads\ads-impl\src\main\kotlin\com\feature\ads\impl\data\main\AdsRepositoryImpl.kt`)

    - **UseCase**:
    - `SetProfilePhotoUseCase` (`feature\profile\profile-impl\src\main\kotlin\com\feature\profile\impl\domain\setphoto\SetProfilePhotoUseCase.kt`)

    - **DI**:
    - `AdsModule` (`feature\ads\ads-impl\src\main\kotlin\com\feature\ads\impl\di\AdsModule.kt`)

    - **Core**:
    - `Validator` (`core\data\src\main\kotlin\com\core\data\validator\Validator.kt`)

---

Когда я передаю документацию сервиса и его API‑контракт, генерируй код data и domain‑слоя по следующим принципам:
(x - название фичи, буду указывать в каждом запросе так: "x=servicename". Используй его в нейминге классов и .названии пакета).

1. Пакетная структура
    - `data.x.repository` – реализация интерфейсов репозиториев (пример: AdInfoRepositoryImpl реализует AdInfoRepository из domain-слоя (`domain.x.repository`)). Обязательно создавай отдельный класс для каждого репозитория. Если есть query-параметр используй dsl ktor - parameter как в AdsRepositoryImpl.
    - `data.x.dto` – все внешние (network) модели данных (Kotlin data class) оборачиваются в ApiResponse (смотри как AdInfoDTO оборачивается в ApiResponse в AdInfoRepositoryImpl).
    - `data.x.model` - модель domain-слоя (пример: AdInfoModel)
    - `data.x.usecase` - также добавляй класс UseCase, который проксирует вызов репозиторию (пример: SetProfilePhotoUseCase)
    - `data.x.mapper` – преобразователи DTO ↔ domain‑моделей (чаще всего это именно DTO -> domain, тк это GET-запросы).
    - Создавай DTO, Mapper и Repository в отдельных файлах и пакетах.

2. Repository
    - В domain‑слое объявлен интерфейс `XRepository` (пример: AdInfoRepository).
    - В data‑слое – `XRepositoryImpl` (пример: AdInfoRepositoryImpl), В репозиторий инжектятся io.ktor.client.HttpClient через koin (чаще всего как auth_client - смотри инжект AdInfoRepository в AdsModule) и Validator.
    - Методы `suspend fun` возвращают `DomainModel` (не нужно использовать обёртки типа Result).
    - Добавляй комментарий на основе аналитики к классу репозитория!
    - Выноси строки (endpoints, params) в константы в companion object!
3. DTO
    - Описывай схемы запросов/ответов API.
    - Используй `@Serializable` аннотации.
    - Рассматривай DTO как body из JSON. DTO всегда используется как типовой аргумент ApiResponse.
    - Если это не Resonse, а Request Body - добавляй Request постфикс перед DTO.
    - Ответ от сервера стандантизирован. Ниже приведена общая схема ответа на ВСЕ endpoints в проекте:
      {
      "success": true,
      "body": {},
      "error": {
      "id": "id",
      "message": "Ошибка"
      }
      }

4. Mappers
    - В `data.mapper` – функции-расширения.
    - Обрабатывай nullable/optional поля и дефолтные значения.

5. Error Handling
    - Все ошибки заворачивать в единый sealed‑класс `DataError` (Network, Api, Db).
    - Репозиторий возвращает `Result` или `Either<DataError, Domain>`.

6. Асинхронность
    - Используй Kotlin Coroutines и Flow.
    - Не блокировать поток UI, всё в Dispatchers.IO.

7. UseCase
    - Чаще всего UseCase просто проксирует вызов из Repository (пример: SetProfilePhotoUseCase).
    - Названия метода UseCase должно совпадать с именем метода репозитория.
    - Не забывай про иньекцию Repository в UseCase.

8. Dependency Injection
    - Инъекция (DI) описывается в файле XModule.kt через Koin в пакете di (смотри AdsModule) для `RepositoryImpl`.


—  
Используй это правило при генерации всех классов и интерфейсов data- domain-слоя.
