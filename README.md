# UI-тесты реестровых страниц

UI-тесты с проверками в БД без Spring.

| Слой | Технологии |
|---|---|
| Page objects, компоненты, core | Java 17 |
| Спеки | Groovy 3.0, Spock 2.3 |
| Браузер | Selenide 7.16.0, Selenoid (CI) |
| Конфигурация | Owner: `env/*.properties`, переменные окружения, `.env` |
| База данных | PostgreSQL, JDBI 3, HikariCP |
| Отчёты | Allure-адаптеры пишут `build/allure-results`, которые уходят в Allure TestOps |

В архив включён демо-стенд (`demo/server.py`) — приложение, реализующее описанный ниже контракт
`data-testid`. Он нужен, чтобы проект можно было запустить локально «как есть», без доступа
к реальному приложению. В настоящем проекте этот каталог удаляется.

## Быстрый старт (локально)

```bash
# 1. поднять демо-стенд (отдельный терминал), нужен только Python 3.9+
python3 demo/server.py              # http://localhost:8080/orders

# 2. (необязательно) настройки БД для DB-спек: скопировать и вписать реальные значения.
#    Если .env нет, DB-спеки пропускаются
cp .env.example .env

# 3. запустить тесты (нужны JDK 17 и установленный Chrome)
./gradlew test -Denv=local
```

Gradle ставить не нужно: в проекте есть wrapper (`./gradlew`, Gradle 9.3).
Браузерный драйвер качать не нужно: Selenide использует Selenium Manager.

## Требования

| Компонент | Версия |
|---|---|
| JDK | 17 |
| Gradle | не нужен, используется `./gradlew` |
| Chrome / Chromium | любой актуальный (локальный запуск) |
| PostgreSQL | только для DB-спек |
| Python | 3.9+ (только для демо-стенда) |

## Контракт с фронтендом (data-testid)

| Что | Локатор |
|---|---|
| Корень страницы | `page-{name}` |
| Панель фильтров | `filter-bar`, внутри — `filter-{key}`, `filters-apply`, `filters-reset` |
| Значение фильтра | `filter-value` (текст/toggle), `filter-toggle` + `filter-options` + `option-{id}` + `selected-option[data-option]` + `filter-clear` (select), `filter-date-from` / `filter-date-to` |
| Таблица | `{name}-table` с атрибутом `data-render-id`, `table-loader`, `table-row[data-row-id]`, `cell-{column}`, `table-empty` |
| Сортировка | `sort-{column}` с `aria-sort = none/ascending/descending` |
| Пагинация | `pagination`, `page-number[data-page][aria-current]`, `page-prev`, `page-next`, `page-size` (`<select>`), `page-summary` («N–M из K») |

`data-render-id` равен `0`, пока первый рендер не завершён, и инкрементируется при каждой
последующей перерисовке тела таблицы. На этом построено детерминированное ожидание
(`TableComponent.awaitRefreshAfter`) — без него тест может прочитать данные предыдущего состояния.

## Как добавить новую страницу за 5 шагов

На примере Invoices. **Core и компоненты не трогаются вообще** — изменения только в новом пакете `page/invoices`:

1. `InvoicesColumn.java` — enum колонок, реализует `ColumnKey` (testId + человеческое имя для ошибок).
2. `InvoiceRow.java` — `record` строки с доменными типами (`BigDecimal`, `LocalDate`, enum).
3. `InvoiceStatus.java` (если есть select-фильтр) — enum, реализует `SelectOption`.
4. `InvoicesFilters.java` — фасад: по одному методу на каждый фильтр страницы поверх `FilterBar`.
5. `InvoicesPage.java` — конструктор собирает `TableComponent<InvoiceRow>`, `PaginationComponent`,
   `InvoicesFilters`; приватный статический `toRow(RowCells)`; публичные методы отдают record'ы и `PageSummary`.

Проверка утверждения: страницы `Orders` и `Users` имеют **разные фильтры** (date range vs toggle)
и **разные колонки**, но `TableComponent`, `PaginationComponent`, `FilterBar`, условия ожидания
и core у них одни и те же — в компонентах нет ни одного `if` про конкретную страницу.

### Где граница абстракции

Дублируются три вещи: сборка компонентов в конструкторе, делегирующие методы
`goToPage/setPageSize/sortBy` и `toRow`. Это дублирование **оставлено намеренно**: вынос его
в `AbstractTablePage<T, F>` даёт один базовый класс, который придётся менять при появлении
страницы с двумя таблицами, без пагинации или с фильтрами в модалке. Цена сейчас — ~20 строк
на страницу; цена иерархии — связность всех страниц. Переосмыслить стоит, когда у 10+ страниц
совпадут одновременно и layout, и жизненный цикл, и тогда это будет **генератор/шаблон страницы**,
а не базовый класс.

## Как добавить новый тип фильтра

1. Создать `final class RangeFilter extends UiComponent implements Filter<NumericRange>` в `component/filter`.
2. Добавить его в `permits` у `sealed interface Filter` (компилятор напомнит сам).
3. Добавить фабричный метод в `FilterBar` (`public RangeFilter range(String key)`), скоупленный
   через `filterRoot(key)` — fail fast останется бесплатно.
4. Открыть его в фасаде нужной страницы: `public RangeFilter amount() { return bar.range("amount"); }`.

Существующие страницы и спеки при этом не меняются.

## Если фронт не отдаёт data-testid

По убыванию предпочтительности:

1. Завести задачу на фронт — это дешевле любой компенсации в тестах; `data-render-id` и `aria-sort`
   просить вместе с testid.
2. Временно использовать существующие **семантические** атрибуты: `role`, `aria-*`, `name`, `id` —
   и обернуть их в аналог `ByTestId` (`BySemantics`), чтобы замена была одной правкой.
3. Если ничего нет — локатор по стабильному структурному признаку внутри root компонента
   (`root.$("tbody tr")`), но **никогда** по тексту и индексам: текст меняется при локализации,
   индексы — при добавлении колонки.
4. Для `data-render-id`: заменить `awaitRefreshAfter` на составное условие «лоадер появился →
   лоадер скрыт → контрольная сумма строк изменилась». Это слабее и должно быть временно —
   зафиксируйте причину в коде ссылкой на задачу.

## Запуск

```bash
# локально, видимый браузер (env=local включает headless=false)
./gradlew test -Denv=local

# headless поверх local-профиля
./gradlew test -Denv=local -Dheadless=true

# конкретная спека
./gradlew test --tests "io.qa.ui.orders.OrdersFilteringSpec" -Denv=local

# последовательно (отладка)
./gradlew test -Denv=local -Dparallel=false

# другой адрес стенда без правки кода
./gradlew test -Denv=local -Dbase.url=http://127.0.0.1:9000 -Dapi.base.url=http://127.0.0.1:9000/api

# только спеки, работающие с БД
./gradlew test --tests "io.qa.ui.db.*" -Denv=local

# другая БД без правки .env
./gradlew test -Denv=local -DDB_URL=jdbc:postgresql://127.0.0.1:5433/registry

# CI / Selenoid
./gradlew test -Denv=stage -Dremote.url=http://selenoid:4444/wd/hub -Dparallel.threads=8 -Dselenoid.video=true
```

Степень параллелизма ограничена снизу количеством браузеров в Selenoid (`-limit`):
`parallel.threads` не должен его превышать.

### Пример Jenkins pipeline

Креды БД берутся из Jenkins Credentials и передаются тестам как переменные окружения.
Результаты из `build/allure-results` отправляются в Allure TestOps (через плагин Jenkins или `allurectl`).

```groovy
pipeline {
    agent any
    environment {
        DB_URL = 'jdbc:postgresql://db.stage.example.com:5432/registry'
    }
    stages {
        stage('UI tests') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'registry-db',
                                                  usernameVariable: 'DB_USER',
                                                  passwordVariable: 'DB_PASSWORD')]) {
                    sh './gradlew test -Denv=stage -Dremote.url=http://selenoid:4444/wd/hub -Dparallel.threads=8'
                }
            }
        }
    }
}
```

## Работа с базой данных (PostgreSQL)

Библиотеки — без Spring и ORM, только SQL:

| Библиотека | Зачем |
|---|---|
| [JDBI 3](https://jdbi.org) (`jdbi3-core` + `jdbi3-sqlobject`) | SQL пишется как есть в аннотации метода, результат сам маппится в `record` |
| HikariCP | пул соединений: спеки идут параллельно |
| `org.postgresql:postgresql` | JDBC-драйвер |

### Настройки подключения

| Переменная | Пример | |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/registry` | обязательна |
| `DB_USER` | `registry_test` | обязательна |
| `DB_PASSWORD` | — | обязательна |
| `DB_POOL_SIZE` | `5` | необязательна |

Ключи описаны в общем `FrameworkConfig`, отдельного конфига для БД нет. Owner читает их из источников
по приоритету (выше — главнее): `-DDB_URL=...` → переменные окружения → файл `.env` в корне проекта
(`file:${user.dir}/.env` в `@Sources`). `-DDB_*` из команды `./gradlew` прокидываются в тесты. Значит, переменная окружения в Jenkins всегда перекрывает `.env`,
а в Jenkins `.env` просто нет, и Owner этот источник пропускает.

`.env` читается как обычный `.properties`: значения без кавычек, `\` экранируется как `\\`.

**Локально:**

```bash
cp .env.example .env   # и вписать свои значения; .env в .gitignore
```

**Jenkins:** `DB_URL` задаётся в `environment`, а `DB_USER` и `DB_PASSWORD` берутся из Credentials
через `withCredentials` (пример в разделе «Пример Jenkins pipeline»).

Подключение ленивое: пул создаётся при первом обращении к DAO. UI-спеки, которые не трогают БД,
работают без настроек и без доступной базы. Если параметр не задан — тест падает с понятным
сообщением, какой именно ключ и куда его добавить.

### Как использовать в спеке

```groovy
@Shared
OrdersDao ordersDb = Db.dao(OrdersDao)

def "заказ сохранён в БД"() {
    when: ...
    then:
    ordersDb.findByNumber(number).orElseThrow().status() == OrderStatus.PAID
}
```

Каждый запрос попадает в отчёт (и в TestOps) отдельным шагом «БД: SELECT ...» с параметрами во вложении.

Примеры, которые можно копировать как шаблон (обе спеки пропускаются через `@Requires`, если `DB_URL` не задан):

| Спека | Что показывает |
|---|---|
| `db/UsersDbSpec` | только БД, без браузера: вставка, чтение по id, `UPDATE`, выборка по условию, нарушение уникальности через `thrown(...)` |
| `db/OrdersDbSpec` | UI + БД: данные вставлены в БД и видны в реестре; заказ, созданный через API, сохранён в БД |

Таблица `users` для `UsersDbSpec` выдумана; чтобы поднять её на локальном Postgres:

```bash
psql "postgresql://USER:PASSWORD@localhost:5432/DBNAME" -f src/test/resources/db/users.sql
```

Таблица `orders` в `OrdersDao` тоже пример: сверьте имена колонок с реальной схемой.

### Как добавить запросы к новой таблице за 2 шага

`Db` отвечает только за подключение и выдаёт любой DAO через `Db.dao(XxxDao)` — при добавлении
таблицы его не трогают. Сущности не связаны друг с другом: каждый DAO — отдельный интерфейс
в своём пакете `db/<домен>/`. Базового DAO с общим CRUD нет намеренно: у таблиц разные ключи
и условия, а общий CRUD прятал бы SQL, ради видимости которого и выбран JDBI.

1. `db/<домен>/XxxRecord.java` — `record` строки (колонка `created_at` → поле `createdAt` автоматически).
2. `db/<домен>/XxxDao.java` — интерфейс, по методу на запрос: `@SqlQuery` для SELECT, `@SqlUpdate`
   для INSERT/UPDATE/DELETE, `@RegisterConstructorMapper(XxxRecord.class)` на интерфейсе.
   Параметры `:name` — это имена аргументов метода.

В спеке: `@Shared XxxDao xxxDb = Db.dao(XxxDao)`.

Правила: в спеке нет SQL и JDBC — только методы DAO; данные, вставленные в БД, помечаются
`runTag` и удаляются в `cleanupSpec` так же, как данные из backdoor-API.

## Правила код-ревью тестов (чек-лист)

- [ ] В спеке нет селекторов, `$`, `Condition` — только вызовы PO и доменные типы.
- [ ] В page object нет ни одного `assert`, `should*` на бизнес-значение и ни одного `return SelenideElement`.
- [ ] Нет `Thread.sleep`, `Selenide.sleep`, `while`-ожиданий.
- [ ] Нет `if/else`, `try/catch` и `@IgnoreIf` «чтобы было зелено».
- [ ] Ожидаемые значения заданы тестом/сидом, а не вычислены из того же UI, который проверяется.
- [ ] Предусловия созданы через backdoor, а не кликами; данные помечены тегом прогона и убираются в `cleanupSpec`.
- [ ] Тест не зависит от порядка выполнения и от данных, созданных другой спекой.
- [ ] Нет статических мутабельных полей; `@Shared` содержит только immutable-данные и thread-safe клиенты.
- [ ] Каждое действие, меняющее данные таблицы, обёрнуто в `awaitRefreshAfter` (через метод страницы).
- [ ] Элементы, участвующие в проверках, имеют `.as("...")` — иначе в отчёте будет голый CSS.
- [ ] Новые локаторы — только `data-testid` через `ByTestId`.
- [ ] Блоки `given/when/then` имеют человеческие лейблы; `@Unroll` содержит значащие параметры.

## Структура

```
src/main/java/io/qa/ui/
  core/config      — внешняя конфигурация (Owner), профили окружений
  core/browser     — жизненный цикл драйвера, Selenoid caps, навигация
  core/ui          — ByTestId, UiComponent (скоупинг), UiConditions, UiFormats
  core/diagnostics — артефакты падения: скриншот, page source, console log
  component/table  — TableComponent<T>, RowMapper, RowCells, ColumnKey, SortDirection
  component/pagination — PaginationComponent, PageSize, PageSummary
  component/filter — sealed Filter<V> + TextFilter/SelectFilter/DateRangeFilter/ToggleFilter + FilterBar
  page/orders, page/users — две страницы с разным набором фильтров и колонок
  testdata         — билдеры сидов и backdoor-клиенты (предусловия НЕ через UI)
  db               — подключение к PostgreSQL (Db) и DAO по доменам (db/orders, db/users)
src/test/java/io/qa/ui/spock  — глобальный Spock-extension для артефактов падения
src/test/groovy               — BaseSpec и спецификации (orders/ — UI, db/ — работа с БД)
src/test/resources            — SpockConfig.groovy (параллелизм), регистрация extension
src/test/resources/db         — DDL примерных таблиц для локального Postgres
demo/                         — демо-стенд (SUT) для локального запуска
.env.example                  — шаблон локальных настроек БД (сам .env в git не попадает)
```
