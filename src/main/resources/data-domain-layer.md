---
description:
globs:
alwaysApply: false
---
# Промпт для генерации Android-архитектуры по описанию API

## Контекст и цель
Генерируй структуру Android приложения на основе описания API в заданном модуле. Создавай файлы в соответствии с Clean Architecture для data, domain слоев и models, добавляя зависимости в Dagger модуль. По описанию API создавай полную структуру файлов с соблюдением правил именования, организации пакетов и документации  а также создаешь unit-тесты для всех файлов. Твоя задача - по описанию API создавать полную структуру файлов с соблюдением всех правил именования, организации пакетов и документации, включая тесты.

## Входные данные
На входе описание API в формате:
- **Имя запроса** (например: `amounts`) - используется в lowercase для папок
- **HTTP метод** (например: `POST`)
- **Endpoint** (например: `pfpv_alf_mb/v1.00/alf/amounts`)
- **Параметры запроса** (список параметров с типами, обязательностью и описанием)
- **Структура тела запроса** (если есть)
- **Структура ответа** (поля и их типы)
- **Название модуля**

Если не хватает каких то входных данных тебе необходимо их запросить у пользователя, все данные необходимы для составления архитектуры 
## Правила генерации

### 1. Организация структуры проекта
Проект имеет базовую структуру пакетов:
- `data/`
- `domain/`
- `models/`

перед созданием 

новые папки создавай одним запросом (команды mkdir через & )

**Важное правило:** Проверяй существование верхнеуровневых папок (data, domain, models). Если они существуют - не создавай их заново. Проходи вглубь иерархии и создавай только недостающие подпапки.
**Важное правило:** Необходимо производить создание и изменение файлов и папок только в папке модуля который был изначально указан, не нужно создавать файлы в корне проекта

### 2. Структура папок для каждого API запроса
Для каждого API запроса создавай подпапки с именем запроса в lowercase внутри:
- `data/converters/{имя_запроса}/request/`
- `data/converters/{имя_запроса}/response/`
- `data/remote/{имя_запроса}/`
- `data/repository/{имя_запроса}/`
- `domain/repository/{имя_запроса}/`
- `domain/interactor/{имя_запроса}/`
- `models/data/{имя_запроса}/`
- `models/domain/{имя_запроса}/`

  Для тестов: Создавай соответствующие тестовые папки в test/ директории с той же структурой.
### 3. Схема именования файлов и классов
Используй следующую схему именования:
- **Базовое имя**: `{Запрос}` с заглавной буквы (пример: `Amounts`)
- **Storage файлы**:
    - Интерфейс: `I{Запрос}Storage.kt` (пример: `IAmountsStorage.kt`)
    - Реализация: `{Запрос}Storage.kt` (пример: `AmountsStorage.kt`)
    - Тест: {Запрос}StorageTest.kt (пример: AmountsStorageTest.kt)
- **DTO классы**:
    - Request: `{Запрос}RequestBodyDTO.kt`
    - Response: `{Запрос}DTO.kt`
    - Тесты для DTO обычно не требуются (это data-классы)
- **Domain модели**: `{Запрос}DomainModel.kt` и вспомогательные enum/data class
- **Конвертеры**:
    - Request конвертер: `I{Запрос}RequestConverter.kt` и `{Запрос}RequestConverter.kt`
    - Response конвертер: `{Запрос}DTOToDomainModelConverter.kt` (только реализация)
    - Тест: {Запрос}RequestConverterTest.kt и {Запрос}DTOToDomainModelConverterTest.kt
- **Репозитории**:
    - Интерфейс: `I{Запрос}Repository.kt`
    - Реализация: `{Запрос}Repository.kt`
    - Тест: {Запрос}RepositoryTest.kt
- **Интеракторы**:
    - Интерфейс: `IAlf{Запрос}Interactor.kt`
    - Реализация: `Alf{Запрос}Interactor.kt`
    - Тест: Alf{Запрос}InteractorTest.kt

### 4. Правила генерации package
Package для каждого файла формируй по пути файла в иерархии. Используй только верхнеуровневые пакеты:
- Для data слоя: `ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.{подпапки}.{имя_запроса}`
- Для domain слоя: `ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.domain.{подпапки}.{имя_запроса}`
- Для models: `ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.{data/domain}.{имя_запроса}`
- Для тестов: Тот же package, что и у тестируемого класса, только в пакете test

## Шаблоны файлов с документацией

### 1. Storage файлы (в `data/remote/{имя_запроса}/`)

**Интерфейс (`I{Запрос}Storage.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.remote.{имя_запроса}

import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}DTO
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}RequestBodyDTO

/**
 * Remote storage для [краткое описание функциональности на основе описания API]
 */
internal interface I{Запрос}Storage {

    /**
     * [Описание метода на основе описания API]
     *
     * @param requestBody тело запроса
     */
    fun {имя_метода}(
        requestBody: {Запрос}RequestBodyDTO
    ): {Запрос}DTO
}
```

Тест ({Запрос}StorageTest.kt):
```kotlin

package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.remote.{имя_запроса}

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import ru.sberbank.mobile.core.efs.models.data.EfsError
import ru.sberbank.mobile.core.efs.net.api.EfsApi
import ru.sberbank.mobile.core.efs.net.api.EfsRequest
import ru.sberbank.mobile.core.network.Method
import ru.sberbank.mobile.core.pfm.data.validators.IValidator
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}DTO
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}RequestBodyDTO

/**
* Юнит тесты на [{Запрос}Storage]
  */
  internal class {Запрос}StorageTest {

  private val efsApi = mockk<EfsApi>()
  private val validator = mockk<IValidator<EfsError>>()
  private val storage = {Запрос}Storage(efsApi, validator)

  @Test
  fun `{имя_метода} should return correct DTO`() {
  // Given
  val requestBody = mockk<{Запрос}RequestBodyDTO>()
  val efsRequest = mockk<EfsRequest>()
  val expectedDto = mockk<{Запрос}DTO>()
  val efsError = mockk<EfsError>()

       every { efsApi.newRequest(Method.{МЕТОД}) } returns efsRequest
       every { efsRequest.path(any()) } returns efsRequest
       every { efsRequest.jsonBody(any()) } returns efsRequest
       every { efsRequest.executeWithException({Запрос}DTO::class.java) } returns expectedDto
       every { expectedDto.error } returns efsError
       every { validator.validate(efsError) } returns Unit

       // When
       val result = storage.{имя_метода}(requestBody)

       // Then
       assertThat(result).isEqualTo(expectedDto)
       verify {
           efsApi.newRequest(Method.{МЕТОД})
           efsRequest.path(ENDPOINT_{ЗАПРОС_В_ВЕРХНЕМ_РЕГИСТРЕ})
           efsRequest.jsonBody(requestBody)
           efsRequest.executeWithException({Запрос}DTO::class.java)
           validator.validate(efsError)
       }
  }

  companion object {
  private const val ENDPOINT_{ЗАПРОС_В_ВЕРХНЕМ_РЕГИСТРЕ} = "{endpoint}"
  }
  }
```

**Реализация (`{Запрос}Storage.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.remote.{имя_запроса}

import ru.sberbank.mobile.core.efs.models.data.EfsError
import ru.sberbank.mobile.core.efs.net.api.EfsApi
import ru.sberbank.mobile.core.network.Method
import ru.sberbank.mobile.core.pfm.data.validators.IValidator
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}DTO
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}RequestBodyDTO

/**
 * Реализация [I{Запрос}Storage]
 *
 * @property efsApi ЕФС api
 * @property validator Валидатор для проверки ошибок от бека
 */
internal class {Запрос}Storage(
    private val efsApi: EfsApi,
    private val validator: IValidator<EfsError>,
) : I{Запрос}Storage {

    override fun {имя_метода}(requestBody: {Запрос}RequestBodyDTO): {Запрос}DTO {

        val request = efsApi.newRequest(Method.{МЕТОД})
            .apply {
                path(ENDPOINT_{ЗАПРОС_В_ВЕРХНЕМ_РЕГИСТРЕ})
                jsonBody(requestBody)
            }

        return request.executeWithException({Запрос}DTO::class.java)
            .also { validator.validate(it.error) }
    }

    companion object {
        private const val ENDPOINT_{ЗАПРОС_В_ВЕРХНЕМ_РЕГИСТРЕ} = "{endpoint}"
    }
}
```

### 2. DTO классы (в `models/data/{имя_запроса}/`)

**Request DTO (`{Запрос}RequestBodyDTO.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}

import kotlinx.serialization.Serializable

/**
 * [Описание тела запроса на основе описания API]
 *
 * @property mode [Описание режима работы]
 * @property filter [Описание фильтров]
 * @property display [Описание отображения]
 */
@Serializable
internal data class {Запрос}RequestBodyDTO(
    val mode: Mode? = null,
    val filter: Filter,
    val display: Display? = null
) {
    /**
     * [Описание Mode]
     *
     * @property property1 [Описание property1]
     * @property property2 [Описание property2]
     */
    @Serializable
    data class Mode(
        val property1: String?,
        val property2: Boolean?
    )

    /**
     * [Описание Filter]
     *
     * @property from Дата начала периода в формате dd.mm.yyyy
     * @property to Дата окончания периода в формате dd.mm.yyyy
     * @property incomeType Признак учета типа операций
     */
    @Serializable
    data class Filter(
        val from: String?,
        val to: String?,
        val incomeType: String,
        // остальные поля на основе описания API
    )

    /**
     * [Описание Display]
     *
     * @property showPlan Признак передачи в ответе плана расходов за период
     * @property showCategoryAmounts Признак передачи в ответе сумм, сгруппированных по категориям
     */
    @Serializable
    data class Display(
        val showPlan: Boolean?,
        val showCategoryAmounts: Boolean?,
        // остальные поля на основе описания API
    )
}
```

**Response DTO (`{Запрос}DTO.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}

import kotlinx.serialization.Serializable
import ru.sberbank.mobile.core.efs.models.data.EfsError
import ru.sberbank.mobile.common.pfmandbudget.models.data.efs.PfmEfsServerEntityDTO

/**
 * Модель ответа на запрос {endpoint}
 */
@Serializable
internal class {Запрос}DTO : PfmEfsServerEntityDTO<{Запрос}BodyDTO>()

/**
 * Тело ответа [{Запрос}DTO]
 *
 * @property error Ошибка от сервера
 * @property data Основные данные ответа
 */
@Serializable
internal data class {Запрос}BodyDTO(
    val error: EfsError? = null,
    val data: List<DataItem>? = null
) {
    /**
     * Элемент данных ответа
     *
     * @property id Идентификатор элемента
     * @property name Наименование элемента
     * @property amount Сумма элемента
     */
    @Serializable
    data class DataItem(
        val id: String,
        val name: String,
        val amount: Double
    )
}
```

### 3. Domain модели (в `models/domain/{имя_запроса}/`)

**Основная Domain Model (`{Запрос}DomainModel.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.domain.{имя_запроса}

import ru.sberbank.mobile.core.pfm.models.domain.IncomeType
import ru.sberbank.mobile.core.models.data.base.money.IMoney
import java.util.Date

/**
 * Доменная модель [краткое описание на основе описания API]
 *
 * @property startOfRequestedDate Дата начала запрошенного периода
 * @property endOfRequestedDate Дата конца запрошенного периода
 * @property incomeType Тип сумм расходные/доходные
 * @property visibleAmount Общая сумма трат по запрошенному периоду
 * @property nationalAmount Сумма операций за период. В национальной валюте
 * @property plan План расхода за период
 * @property categories Список категорий
 */
internal data class {Запрос}DomainModel(
    val startOfRequestedDate: Date,
    val endOfRequestedDate: Date,
    val incomeType: IncomeType,
    val visibleAmount: IMoney,
    val nationalAmount: IMoney,
    val plan: PlanDomainModel?,
    val categories: List<CategoryDomainModel>?
)
```

**Enum классы:**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.domain.{имя_запроса}

/**
 * Тип [чего-то] на основе описания API
 */
internal enum class {ИмяEnum}Type(val value: String) {
    /** [Описание VALUE1] */
    VALUE1("value1"),
    
    /** [Описание VALUE2] */
    VALUE2("value2");
}
```

### 4. Конвертеры

**Request конвертер - интерфейс (`I{Запрос}RequestConverter.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.converters.{имя_запроса}.request

import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}RequestBodyDTO

/**
 * Контракт конвертера параметров запроса {Запрос} в [{Запрос}RequestBodyDTO]
 */
internal interface I{Запрос}RequestConverter {

    /**
     * Сконвертировать параметры [список параметров с описанием]
     * в модель для тела запроса [{Запрос}RequestBodyDTO]
     *
     * @param from Дата начала периода
     * @param incomeTypes Типы запрашиваемых сумм
     * @param to Дата окончания периода
     * @return модель для тела запроса
     */
    fun convert(
        from: Date,
        incomeTypes: Set<IncomeType>,
        to: Date? = null
        // остальные параметры на основе описания API
    ): {Запрос}RequestBodyDTO
}
```

**Request конвертер - реализация (`{Запрос}RequestConverter.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.converters.{имя_запроса}.request

import ru.sberbank.mobile.common.pfmandbudget.data.converters.SetOfIncomeTypeToStringConverter
import ru.sberbank.mobile.core.pfm.models.domain.IncomeType
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}RequestBodyDTO
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.utils.IDateFormatter
import java.util.Date

/**
 * Реализация [I{Запрос}RequestConverter]
 *
 * @property setOfIncomeTypesToRequestStringConverter конвертер множества [IncomeType] в [String]
 * @property dateFormatter сущность для форматирования дат
 */
internal class {Запрос}RequestConverter(
    private val setOfIncomeTypesToRequestStringConverter: SetOfIncomeTypeToStringConverter,
    private val dateFormatter: IDateFormatter,
) : I{Запрос}RequestConverter {

    override fun convert(
        from: Date,
        incomeTypes: Set<IncomeType>,
        to: Date? = null
        // остальные параметры
    ): {Запрос}RequestBodyDTO {
        val isAddDisplay = /* логика определения необходимости Display */

        val mode = {Запрос}RequestBodyDTO.Mode(
            /* заполнение полей */
        ).takeIf { /* условие */ }

        val filter = {Запрос}RequestBodyDTO.Filter(
            from = from.formatDate(),
            to = to?.formatDate(),
            incomeType = setOfIncomeTypesToRequestStringConverter.convert(incomeTypes),
            // остальные поля
        )

        val display = {Запрос}RequestBodyDTO.Display(
            // заполнение полей
        ).takeIf { isAddDisplay }

        return {Запрос}RequestBodyDTO(
            mode,
            filter,
            display
        )
    }

    private fun Date.formatDate() = dateFormatter.format(this, DATE_FORMAT)

    private companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX"
    }
}
```
Тест для Request конвертера ({Запрос}RequestConverterTest.kt):
```kotlin

package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.converters.{имя_запроса}.request

import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import org.junit.jupiter.api.Test
import ru.sberbank.mobile.common.pfmandbudget.data.converters.SetOfIncomeTypeToStringConverter
import ru.sberbank.mobile.core.pfm.models.domain.IncomeType
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}RequestBodyDTO
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.domain.{имя_запроса}.{ИмяEnum}Type
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.utils.IDateFormatter
import java.util.Date

/**
* Тест на [{Запрос}RequestConverter]
  */
  internal class {Запрос}RequestConverterTest {

  private val setOfIncomeTypesToRequestStringConverter: SetOfIncomeTypeToStringConverter = mockk()
  private val dateFormatter: IDateFormatter = mockk()
  private val converter = {Запрос}RequestConverter(setOfIncomeTypesToRequestStringConverter, dateFormatter)

  @Test
  fun `test convert with all parameters`() {
  // Given
  val date: Date = mockk()
  val incomeTypes = setOf(IncomeType.INCOME)
  val {ИмяEnum}Type = {ИмяEnum}Type.VALUE1

       val expected = {Запрос}RequestBodyDTO(
           filter = {Запрос}RequestBodyDTO.Filter(
               from = MOCK_STRING,
               to = MOCK_STRING,
               incomeType = MOCK_STRING,
               // остальные поля
           ),
           display = {Запрос}RequestBodyDTO.Display(
               // заполнение полей на основе параметров
           ),
           mode = {Запрос}RequestBodyDTO.Mode(
               // заполнение полей
           )
       )

       every { dateFormatter.format(date, DATE_FORMAT) } returns MOCK_STRING
       every { setOfIncomeTypesToRequestStringConverter.convert(incomeTypes) } returns MOCK_STRING

       // When
       val actual = converter.convert(
           from = date,
           incomeTypes = incomeTypes,
           to = date,
           // остальные параметры
       )

       // Then
       Truth.assertThat(actual).isEqualTo(expected)

       verifyAll {
           setOfIncomeTypesToRequestStringConverter.convert(incomeTypes)
           dateFormatter.format(date, DATE_FORMAT)
       }
  }

  @Test
  fun `test convert without optional parameters`() {
  // Given
  val date: Date = mockk()
  val incomeTypes = setOf(IncomeType.INCOME)

       val expected = {Запрос}RequestBodyDTO(
           filter = {Запрос}RequestBodyDTO.Filter(
               from = MOCK_STRING,
               to = null,
               incomeType = MOCK_STRING,
               // остальные поля
           ),
           display = null,
           mode = null
       )

       every { dateFormatter.format(date, DATE_FORMAT) } returns MOCK_STRING
       every { setOfIncomeTypesToRequestStringConverter.convert(incomeTypes) } returns MOCK_STRING

       // When
       val actual = converter.convert(
           from = date,
           incomeTypes = incomeTypes,
           to = null,
           // остальные параметры = null
       )

       // Then
       Truth.assertThat(actual).isEqualTo(expected)

       verifyAll {
           setOfIncomeTypesToRequestStringConverter.convert(incomeTypes)
           dateFormatter.format(date, DATE_FORMAT)
       }
  }

  private companion object {
  private const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX"
  private const val MOCK_STRING = ""
  }
  }
```


**Response конвертер (`{Запрос}DTOToDomainModelConverter.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.converters.{имя_запроса}.response

import ru.sberbank.mobile.common.pfmandbudget.data.converters.IncomeTypeDTOConverter
import ru.sberbank.mobile.core.models.data.base.money.CurrencyParser
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.converters.MoneyDTOConverter
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}DTO
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.domain.{имя_запроса}.{Запрос}DomainModel

/**
 * Конвертер [{Запрос}DTO] в [{Запрос}DomainModel]
 *
 * @property incomeTypeDTOConverter конвертер [IncomeTypeDTO] в [IncomeType]
 * @property moneyDTOConverter конвертер [MoneyDTO] в [IMoney]
 * @property currencyParser парсер валют
 */
internal class {Запрос}DTOToDomainModelConverter(
    private val incomeTypeDTOConverter: IncomeTypeDTOConverter,
    private val moneyDTOConverter: MoneyDTOConverter,
    private val currencyParser: CurrencyParser
) {

    /**
     * Конвертировать [{Запрос}DTO] в список [{Запрос}DomainModel]
     *
     * @param dto DTO модель
     * @return список доменных моделей
     */
    fun convert(dto: {Запрос}DTO): List<{Запрос}DomainModel> {
        return dto.body?.let { body ->
            body.data?.map { item ->
                {Запрос}DomainModel(
                    startOfRequestedDate = item.from,
                    endOfRequestedDate = item.to,
                    incomeType = incomeTypeDTOConverter.convert(item.incomeType),
                    visibleAmount = moneyDTOConverter.convert(item.visibleAmount),
                    nationalAmount = moneyDTOConverter.convert(item.nationalAmount),
                    // конвертация остальных полей
                )
            } ?: emptyList()
        } ?: emptyList()
    }
}
```

### 5. Репозитории

**Интерфейс (`I{Запрос}Repository.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.domain.repository.{имя_запроса}

import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.domain.{имя_запроса}.{Запрос}DomainModel
import ru.sberbank.mobile.core.pfm.models.domain.IncomeType
import java.util.Date
import kotlin.reflect.KProperty1

/**
 * Репозиторий для [описание функциональности на основе описания API]
 */
internal interface I{Запрос}Repository {

    /**
     * Получить [что-то] по запрошенным данным
     *
     * @param force `true` - идем в сеть, `false` - пытаемся достать из кеша
     * @param parameters параметры
     * @param cacheName название кэша (если не передавать будет записываться в общий кэш)
     */
    suspend fun get{Запрос}(
        force: Boolean,
        parameters: Parameters,
        cacheName: String? = null
    ): List<{Запрос}DomainModel>

    /**
     * Есть ли кеш
     *
     * @param parameters параметры
     * @param cacheName название кэша (если не передавать будет проверяться общий кэш)
     */
    suspend fun isCacheExists(
        parameters: Parameters,
        cacheName: String? = null
    ): Boolean

    /**
     * Очистить кэш
     *
     * @param parameters параметры
     * @param cacheName название кэша (если не передавать будет очищаться общий кэш)
     */
    suspend fun clearCache(
        parameters: Parameters,
        cacheName: String? = null
    ): Boolean

    /**
     * Очистить кэш
     *
     * @param cacheName название кэша
     */
    suspend fun clearCache(
        cacheName: String
    ): Boolean

    /**
     * Очистить весь кеш
     */
    suspend fun clearAllCaches(): Boolean

    /**
     * Параметры для запроса {имя_запроса}
     *
     * @property from Дата начала периода
     * @property incomeTypes Типы запрашиваемых сумм
     * @property to Дата окончания периода
     */
    data class Parameters(
        val from: Date,
        val incomeTypes: Set<IncomeType>,
        val to: Date? = null,
        // остальные параметры на основе описания API
        private val cacheFields: Set<KProperty1<Parameters, *>> = allFields
    ) {

        /**
         * Получить ключ для кэша
         */
        fun getCacheKey(): Any = allFields.map {
            if (cacheFields.contains(it)) it.get(this) else null
        }

        companion object {
            val allFields = setOf(
                Parameters::from,
                Parameters::incomeTypes,
                Parameters::to,
                // остальные поля
            )
        }
    }
}
```

**Реализация (`{Запрос}Repository.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.repository.{имя_запроса}

import kotlinx.coroutines.withContext
import ru.sberbank.mobile.core.cache.coroutine.ConcurrentCache
import ru.sberbank.mobile.core.cache.coroutine.concurrentChild
import ru.sberbank.mobile.core.coroutines.dispatcher.CoroutineDispatchers
import ru.sberbank.mobile.core.pfm.domain.analytics.ScreensTimeAnalyticsPluginFacade
import ru.sberbank.mobile.core.pfm.domain.analytics.wrapRequest
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.converters.{имя_запроса}.request.I{Запрос}RequestConverter
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.converters.{имя_запроса}.response.{Запрос}DTOToDomainModelConverter
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.data.remote.{имя_запроса}.I{Запрос}Storage
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.domain.repository.{имя_запроса}.I{Запрос}Repository
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}DTO
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.data.{имя_запроса}.{Запрос}RequestBodyDTO
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.domain.{имя_запроса}.{Запрос}DomainModel
import ru.sberbank.mobile.core.pfm.models.domain.IncomeType
import java.util.Date

/**
 * Реализация [I{Запрос}Repository]
 *
 * @param remoteStorage удаленное хранилище данных
 * @param localStorage локальное хранилище данных
 * @param dtoConverter конвертер сущностей [{Запрос}DTO] в список [{Запрос}DomainModel]
 * @param requestConverter конвертер domain параметров в [{Запрос}RequestBodyDTO] для [remoteStorage]
 * @param coroutineDispatchers диспатчеры корутин
 * @param screensTimeAnalyticsPluginFacade Facade для end-to-end метрик загрузки экрана.
 */
internal class {Запрос}Repository(
    private val remoteStorage: I{Запрос}Storage,
    private val localStorage: ConcurrentCache<Any>,
    private val dtoConverter: {Запрос}DTOToDomainModelConverter,
    private val requestConverter: I{Запрос}RequestConverter,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val screensTimeAnalyticsPluginFacade: ScreensTimeAnalyticsPluginFacade
) : I{Запрос}Repository {

    private val localStorages = mutableMapOf<String, ConcurrentCache<Any>>()

    private fun String?.getStorage() = if (this == null) localStorage else localStorages[this]

    private fun String?.getOrPutStorage() =
        if (this == null) {
            localStorage
        } else {
            localStorages.getOrPut(this) { localStorage.concurrentChild(this).build() }
        }

    override suspend fun get{Запрос}(
        force: Boolean,
        parameters: I{Запрос}Repository.Parameters,
        cacheName: String?
    ): List<{Запрос}DomainModel> = withContext(coroutineDispatchers.io) {
        cacheName.getOrPutStorage().getSync(
            key = parameters.getCacheKey(),
            action = {
                fromRemote(
                    from = parameters.from,
                    incomeTypes = parameters.incomeTypes,
                    to = parameters.to
                    // остальные параметры
                )
            },
            force = force
        )
            .toDomainModel()
    }

    private suspend fun fromRemote(
        from: Date,
        incomeTypes: Set<IncomeType>,
        to: Date?
        // остальные параметры
    ): {Запрос}DTO {
        val requestDTO = requestConverter.convert(
            from = from,
            incomeTypes = incomeTypes,
            to = to
            // остальные параметры
        )
        return screensTimeAnalyticsPluginFacade.wrapRequest {
            remoteStorage.{имя_метода}(requestDTO)
        }
    }

    private fun {Запрос}DTO.toDomainModel() = dtoConverter.convert(this)

    override suspend fun isCacheExists(
        parameters: I{Запрос}Repository.Parameters,
        cacheName: String?
    ): Boolean =
        cacheName.getStorage()?.isExist(parameters.getCacheKey()) == true

    override suspend fun clearCache(
        parameters: I{Запрос}Repository.Parameters,
        cacheName: String?
    ): Boolean =
        cacheName.getStorage()?.invalidate(parameters.getCacheKey()) == true

    override suspend fun clearCache(cacheName: String): Boolean =
        localStorages[cacheName]?.invalidateAll() == true

    override suspend fun clearAllCaches(): Boolean = localStorage.cleanUp()
}
```

### 6. Интеракторы

**Интерфейс (`IAlf{Запрос}Interactor.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.domain.interactor.{имя_запроса}

import ru.sberbank.mobile.feature.pfmfinanceanalysis.api.di.PfmFinanceAnalysisBaseOutApi
import ru.sberbank.mobile.feature.pfmfinanceanalysis.api.models.domain.GetAlf{Запрос}
import ru.sberbank.mobile.feature.pfmfinanceanalysis.api.models.domain.amounts.Alf{Запрос}DomainModel
import sber.core.di.meta.annotation.ProvidedBy

/**
 * Интерактор для получения данных от {endpoint}
 */
@ProvidedBy(PfmFinanceAnalysisBaseOutApi::class)
interface IAlf{Запрос}Interactor {

    /**
     * Получить [Alf{Запрос}DomainModel]
     *
     * @param parameters Модель с параметрами
     */
    suspend fun getAlf{Запрос}(
        parameters: GetAlf{Запрос}
    ): List<Alf{Запрос}DomainModel>
}
```

**Реализация (`Alf{Запрос}Interactor.kt`):**
```kotlin
package ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.domain.interactor.{имя_запроса}

import ru.sberbank.mobile.core.pfm.models.domain.IncomeType
import ru.sberbank.mobile.feature.pfmfinanceanalysis.api.models.domain.GetAlf{Запрос}
import ru.sberbank.mobile.feature.pfmfinanceanalysis.api.models.domain.amounts.Alf{Запрос}DomainModel
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.domain.repository.{имя_запроса}.I{Запрос}Repository
import ru.sberbank.mobile.feature.pfmfinanceanalysis.impl.models.domain.{имя_запроса}.{Запрос}DomainModel

/**
 * Реализация интерфейса [IAlf{Запрос}Interactor]
 *
 * @param {имя_запроса}Repository Репозиторий для полученях сумм по запрошенным данным
 */
internal class Alf{Запрос}Interactor(
    private val {имя_запроса}Repository: I{Запрос}Repository
) : IAlf{Запрос}Interactor {

    override suspend fun getAlf{Запрос}(
        parameters: GetAlf{Запрос}
    ): List<Alf{Запрос}DomainModel> = {имя_запроса}Repository.get{Запрос}(
        parameters.force,
        I{Запрос}Repository.Parameters(
            from = parameters.from,
            incomeTypes = parameters.incomeTypes.map { it.toIncomeType() }.toSet(),
            to = parameters.to
            // остальные параметры
        )
    ).map { it.toAlf() }

    private fun {Запрос}DomainModel.toAlf() = Alf{Запрос}DomainModel(
        startOfRequestedDate = startOfRequestedDate,
        endOfRequestedDate = endOfRequestedDate,
        incomeType = incomeType.toAlf(),
        visibleAmount = visibleAmount,
        nationalAmount = nationalAmount
        // остальные поля
    )

    private fun IncomeType.toAlf() = AlfIncomeType.valueOf(name)

    private fun AlfIncomeType.toIncomeType() = IncomeType.valueOf(name)
}
```

## Генерация Dagger зависимостей

### 1. Добавление константы кэша
В начало Dagger модуля `PfmFinanceAnalysisBaseModule` (рядом с другими константами кэша) добавь:
```java
String {ЗАПРОС_В_ВЕРХНЕМ_РЕГИСТРЕ}_CACHE = "{имя_запроса}Cache";
```

### 2. Метод provide репозитория
```java
/**
 * Предоставляет I{Запрос}Repository в граф зависимостей
 *
 * @param efsApi апи ефс
 * @param validator валидатор ошибок
 * @param cacheManager менеджер кеша
 * @param incomeTypeDTOConverter конвертер из {@link IncomeTypeDTO} в {@link IncomeType}
 * @param moneyDTOConverter конвертер из {@link MoneyDTO} в {@link IMoney}
 * @param incomeTypesConverter конвертер множества {@link IncomeType} в {@link String}
 * @param dateFormatter утилита для работы с SimpleDateFormat
 * @param coroutineDispatchers интерфейс для получения диспетчеров
 * @param screensTimeAnalyticsPluginFacade фасад для аналитики экранов
 * @param currencyParser интерфейс парсера валют
 * @return {@link I{Запрос}Repository}
 */
@PerFeature
@Provides
static I{Запрос}Repository provide{Запрос}Repository(
        EfsApi efsApi,
        @PfmValidator IValidator<EfsError> validator,
        RuntimeCacheManager cacheManager,
        IncomeTypeDTOConverter incomeTypeDTOConverter,
        MoneyDTOConverter moneyDTOConverter,
        SetOfIncomeTypeToStringConverter incomeTypesConverter,
        IDateFormatter dateFormatter,
        CoroutineDispatchers coroutineDispatchers,
        ScreensTimeAnalyticsPluginFacade screensTimeAnalyticsPluginFacade,
        CurrencyParser currencyParser
) {

    return new {Запрос}Repository(
            new {Запрос}Storage(efsApi, validator),
            concurrentChild(cacheManager.getSessionCache(), {ЗАПРОС_В_ВЕРХНЕМ_РЕГИСТРЕ}_CACHE).build(),
            new {Запрос}DTOToDomainModelConverter(incomeTypeDTOConverter, moneyDTOConverter, currencyParser),
            new {Запрос}RequestConverter(incomeTypesConverter, dateFormatter),
            coroutineDispatchers,
            screensTimeAnalyticsPluginFacade
    );
}
```

### 3. Метод provide интерактора
```java
/**
 * Предоставляет IAlf{Запрос}Interactor в граф зависимостей
 *
 * @param {имя_запроса}Repository репозиторий для {имя_запроса}
 * @return {@link IAlf{Запрос}Interactor}
 */
@PerFeature
@Provides
static IAlf{Запрос}Interactor provideAlf{Запрос}Interactor(
        I{Запрос}Repository {имя_запроса}Repository
) {
    return new Alf{Запрос}Interactor({имя_запроса}Repository);
}
```

## Алгоритм генерации

1. **Получить описание API**: имя запроса, метод, endpoint, параметры, структура запроса и ответа
2. **Нормализовать имя запроса**:
    - Для папок: lowercase (например, `amounts`)
    - Для классов: CamelCase с заглавной буквы (например, `Amounts`)
    - Для констант: верхний регистр с подчеркиваниями (например, `AMOUNTS`)
3. **Проверить существование папок**:
    - Если существуют `data/`, `domain/`, `models/` - не создавать
    - Пройти по иерархии и создать только недостающие подпапки
    - Создать соответствующие тестовые папки в test/ директории
4. **Определить структуры**:
    - На основе параметров запроса создать `Parameters` класс в репозитории
    - На основе структуры запроса создать DTO request с вложенными классами
    - На основе структуры ответа создать DTO response и Domain модели
    - Определить необходимые enum классы на основе ограниченных значений
5. **Сгенерировать файлы**
    - в соответствии с шаблонами
    - Тестовые файлы для всех классов с логикой
6. **Заменить плейсхолдеры** на реальные значения:
    - `{Запрос}` → Имя запроса с заглавной буквы
    - `{имя_запроса}` → Имя запроса в lowercase
    - `{МЕТОД}` → HTTP метод (POST, GET, etc.)
    - `{endpoint}` → Endpoint API
    - `{ЗАПРОС_В_ВЕРХНЕМ_РЕГИСТРЕ}` → Имя запроса в верхнем регистре для констант
    - `{имя_метода}` → Имя метода (обычно совпадает с именем запроса в lowercase)
7. **Сгенерировать документацию**:
    - Для каждого класса: краткое описание функциональности
    - Для каждого свойства: описание назначения и возможных значений
    - Для каждого метода: описание действия, параметров и возвращаемого значения
8. **Сгенерировать Dagger зависимости**:
    - Добавить константу кэша
    - Добавить метод provide репозитория
    - Добавить метод provide интерактора

## Особые указания

1. **Документация**: Всегда добавляй KDoc/JavaDoc комментарии для всех классов, интерфейсов, свойств и методов по образцу из примеров
2. **Именование**: Строго соблюдай схему именования как в примере `amounts`
3. **Package**: Формируй package строго по пути файла в иерархии
4. **Внутренняя видимость**: Все файлы в `impl` модуле должны быть `internal` (в Kotlin) или с package-private видимостью (в Java)
5. **Аннотации**: Используй `@ProvidedBy(PfmFinanceAnalysisBaseOutApi::class)` для интерфейсов интеракторов
6. **Кэширование**: Включай методы кэширования в репозитории по аналогии с `AmountsRepository`
7. **Конвертация**: Создавай полные цепочки конвертации DTO → Domain → Public Domain
8. **Расширяющие функции**: Создавай функции расширения для конвертации enum и других типов
9. **Сопутствующие объекты**: Создавай `companion object` для констант в Kotlin-классах
10. **Тестирование**: Создавай unit-тесты для всех классов с логикой:
    - Используй Mockk для мокинга зависимостей
    - Используй Truth для утверждений
    - Используй kotlinx-coroutines-test для тестирования корутин
    - Тестируй основные сценарии и edge-кейсы
    - Мокируй все внешние зависимости

## Пример для запроса "amounts"

Если на вход приходит:
- Имя запроса: `amounts`
- Метод: `POST`
- Endpoint: `pfpv_alf_mb/v1.00/alf/amounts`
- Параметры: `from: Date`, `incomeTypes: Set<IncomeType>`, `to: Date?`, и т.д.

Создаются файлы:
- `data/remote/amounts/IAmountsStorage.kt` и `AmountsStorage.kt`
- `models/data/amounts/AmountsRequestBodyDTO.kt` и `AmountsDTO.kt`
- `models/domain/amounts/AmountsDomainModel.kt` с enum (`OnOffType`, `PeriodAmountsType`, и т.д.)
- `data/converters/amounts/request/IAmountsRequestConverter.kt` и `AmountsRequestConverter.kt`
- `data/converters/amounts/response/AmountsDTOToDomainModelConverter.kt`
- `domain/repository/amounts/IAmountsRepository.kt`
- `data/repository/amounts/AmountsRepository.kt`
- `domain/interactor/amounts/IAlfAmountsInteractor.kt` и `AlfAmountsInteractor.kt`

И Dagger зависимости:
- Константа: `String AMOUNT_CACHE = "amountsCoroutineCache";`
- Метод: `provideAmountsRepository(...)`
- Метод: `provideAlfAmountsInteractor(...)`

---
**Этот промпт содержит все инструкции для генерации Android-архитектуры по описанию API с полной документацией и Dagger зависимостями.**
—  
Используй это правило при генерации всех классов и интерфейсов data- domain-слоя.

