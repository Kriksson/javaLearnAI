# Практическое изучение Java

Персональный тренажёр с гибкой прогрессией: каждое задание закрепляет освоенные навыки и добавляет новые понятия из выбранной программы.

## Как работаем

1. Тренер создаёт условие, отдельные учебные примеры и JUnit-тесты.
2. Ученик самостоятельно проектирует и собирает проект с нуля, реализует решение и пишет: **«Решил»**.
3. Тренер проверяет код и запускает все тесты.
4. Успешная задача переносится в `tasks/completed/` вместе с решением и тестами.
5. Обновляются общий прогресс и подтверждённые навыки, затем выполняются commit и push.

Новые задания могут смешивать знания из разных разделов программы. После уверенного Java Core программа расширена на следующий этап backend-разработки.

## Формат задач: проектирование и сборка с нуля

Предпочтение Кирилла для текущей и следующих задач — самостоятельно создавать проект и весь код приложения.

- Ученик сам выбирает классы, методы, модели, роли компонентов и связи между ними в рамках требований задачи.
- Ученик вручную создаёт пакеты и исходные файлы, стартовый класс, конфигурацию приложения и SQL-схему, если они нужны.
- Ученик сам подключает зависимости, настраивает Maven, собирает и запускает приложение. Для отдельного нового проекта он также создаёт `pom.xml` и структуру каталогов; существующий учебный Maven-проект можно использовать как окружение проверки.
- Тренер предоставляет условие, внешний контракт, критерии готовности и автоматические тесты. Заготовки классов с `TODO`, готовая структура приложения и конфигурация решения не выдаются.
- Условие описывает необходимые роли и поведение. Имена внутренних классов и методов ученик выбирает сам; обязательные имена фиксируются только там, где без них нельзя определить внешний контракт или запустить проверку.
- Новые понятия объясняются на небольших работающих примерах из другого сценария. Эти примеры остаются в учебном материале; файлы решения ученик создаёт сам.

До создания приложения тесты могут завершаться ошибкой отсутствующей конфигурации или реализации. Это ожидаемое начальное состояние; задача засчитывается только после успешного запуска тестов и проверки ограничений.

## Границы курса

Пройденный фундамент и следующие этапы обучения:

- типы данных, строки, числа, булевы значения, циклы и массивы;
- методы, область видимости, декомпозиция и правила написания кода;
- объекты, классы и коллекции;
- Git, GitHub, командная строка, JDK, отладка и IntelliJ IDEA;
- ООП: инкапсуляция, наследование, `Object`, статика, константы, перечисления, абстракция и полиморфизм;
- дженерики, unit-тесты, анализ проблем и итоговые проекты.
- современный Java: лямбды, Stream API, дата и время;
- SQL, PostgreSQL и проектирование реляционных данных;
- JDBC и работа Java-приложения с базой данных;
- HTTP, REST API и Spring Boot;
- Spring JDBC, Spring Data JPA / Hibernate и транзакции;
- безопасность API, интеграционные тесты, контейнеризация и CI;
- самостоятельные backend-проекты от проектирования до запуска.

### Целевой стек Java backend junior

Это ориентир для следующих задач и итоговых проектов. Освоение каждого пункта подтверждается практическим решением; наличие технологии в плане само по себе не означает, что она уже освоена.

| Область | Технологии и навыки | Практический результат |
|---|---|---|
| Java | Java 21, ООП, коллекции, дженерики, исключения, Stream API, `java.time`; основы JVM, потоков и `ExecutorService` | Самостоятельно проектировать модель, обрабатывать ошибки и понимать базовые проблемы общего изменяемого состояния |
| Сборка и инструменты | Maven, Maven Wrapper, Git/GitHub, IntelliJ IDEA, отладчик, Linux и командная строка | Создавать проект, управлять зависимостями, собирать JAR, отлаживать код и работать с ветками |
| HTTP и API | HTTP/REST, JSON/Jackson, Spring Boot, Spring MVC, Bean Validation | Создавать CRUD API с DTO, фильтрацией, пагинацией, корректными статусами и единым форматом ошибок |
| SQL и база данных | PostgreSQL, SQL, связи и ограничения, индексы, `EXPLAIN`, транзакции | Проектировать схему, писать запросы, обеспечивать целостность данных и разбирать медленные запросы на базовом уровне |
| Доступ к данным | JDBC / `JdbcTemplate`, Spring Data JPA, Hibernate, `@Transactional` | Связывать API с базой, моделировать отношения, управлять транзакциями и замечать проблему N+1 |
| Миграции | Flyway как основной инструмент; знакомство с Liquibase | Версионировать схему и разворачивать базу вместе с приложением |
| Безопасность | Spring Security, аутентификация и авторизация, роли, хеширование паролей, основы сессий и bearer-токенов | Ограничивать доступ к endpoint, различать `401` и `403`, хранить пароли корректно и использовать стандартные механизмы защиты |
| Тестирование | JUnit 5, Mockito, MockMvc, Spring Boot Test, Testcontainers с PostgreSQL | Писать unit- и интеграционные тесты, проверять HTTP-контракт, SQL и откат транзакций |
| Документация API | OpenAPI/Swagger, `curl` или Postman, README проекта | Описывать запросы и ответы и давать воспроизводимые команды запуска и проверки |
| Запуск приложения | Docker, Docker Compose, переменные окружения, Spring profiles | Собрать образ приложения и поднять приложение с PostgreSQL одной командой |
| Диагностика | SLF4J/Logback, уровни логирования, Spring Boot Actuator | Читать логи, находить причины ошибок и проверять состояние приложения через health endpoint |
| CI | GitHub Actions: сборка, тесты и создание артефакта или Docker-образа | Автоматически проверять изменения и получать готовый результат сборки |

Порядок backend-практики: Spring MVC и валидация → Spring JDBC и PostgreSQL → JPA/Hibernate и транзакции → миграции → безопасность → Testcontainers, Docker и CI → итоговый проект. Знакомые инструменты тестирования, Git и Maven используются на каждом этапе.

Готовность к итоговому проекту: ученик с нуля создаёт REST API с PostgreSQL, миграциями, ролями доступа, тестами, документацией и запуском через Docker Compose; умеет объяснить архитектуру и исправить найденную ошибку.

Дополнительные темы после основного стека: Redis, Kafka/RabbitMQ, Gradle, углублённая многопоточность и микросервисы. Их вводим по потребности проекта. Kubernetes и сложная распределённая инфраструктура остаются за границами базового курса.

Материал для практики: [Spring Boot и SQL/JPA](https://docs.spring.io/spring-boot/3.5/reference/data/sql.html), [миграции и инициализация базы](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html), [Spring Security](https://docs.spring.io/spring-security/reference/servlet/index.html), [Testcontainers в Spring Boot](https://docs.spring.io/spring-boot/3.5/reference/testing/testcontainers.html), [модель приложения Docker Compose](https://docs.docker.com/compose/intro/compose-application-model/).

## Команды

- `Решил` / `Проверь задание` — проверить, архивировать и отправить успешное решение.
- `Ещё задание` / `Следующая задача` — получить следующий шаг прогрессии.
- `Сложнее` — получить задачу с большей самостоятельностью и новыми понятиями.
- `Подсказка`, `Упрости`, `Объясни ...` — получить помощь без готового решения.
- `Покажи прогресс` — увидеть общий счётчик, навыки и текущую задачу.

## Проверка

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```

В текущем Linux-окружении установленный полный JDK 21 и локальный Maven-кеш задаются явно; исходный код компилируется для Java 21:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Для запуска из IntelliJ IDEA выбери этот JDK 21 в Project SDK и настройках запуска. Spring Boot 3.5.16 поддерживает Java 17–25; прежняя рекомендация использовать JDK 27 исправлена. [Требования Spring Boot](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

## Профиль ученика

- Имя: Кирилл
- Исходный опыт: небольшой опыт в Java Core
- Интересы: игры
- Текущий практический уровень: уверенно решает задачи на Java Core, ООП, коллекции и Stream API; освоил SQL, JDBC, HTTP и JSON; самостоятельно создаёт Spring Boot REST API с валидацией и внешней конфигурацией, читает и изменяет данные через JdbcTemplate, отображает результат SQL-изменения на HTTP-статусы и проверяет контракт через MockMvc; получает генерируемый БД BIGINT ID через Spring JDBC; применяет `@Transactional` для атомарных изменений нескольких строк и проверяет откат через интеграционный тест; самостоятельно отображает JPA-сущность на таблицу через `@Entity`, `@Table`, `@Id`, `@GeneratedValue` и `@Column`, использует `JpaRepository.findById` и `save`, получает ID от базы и возвращает его в `201 Created` с `Location`, настраивает Hibernate для создания схемы H2; связывает сущности через `@ManyToOne` и `@JoinColumn`, возвращает данные связанной гильдии через REST и проверяет целостность внешнего ключа; выбирает хранителей по ID связанной гильдии через производный метод Spring Data с сортировкой и преобразует сущности в DTO; объединяет три условия равенства через `And`, проверяет параметры URL и различает отсутствие гильдии и отсутствие совпадений; использует `GreaterThanEqual` и сортировку по двум полям `OrderByLevelDescIdAsc`; изменяет связь управляемой JPA-сущности внутри транзакционного сервиса без явного `save()`, применяя dirty checking; выполняет постраничную выборку через `Pageable` / `PageRequest`, преобразует `Page<сущность>` в DTO и возвращает корректные общие количества; загружает LAZY-связь через `@EntityGraph`, возвращает вложенный DTO и устраняет N+1 при постраничном чтении с отключённым Open Session in View; подключает приложение к PostgreSQL через JDBC-драйвер и переменные окружения, сохраняет данные после повторного открытия контекста; создаёт схему первой SQL-миграцией Flyway, задаёт ограничения базы и проверяет соответствие сущностям через Hibernate `validate`; обновляет существующую схему миграцией V2 с DEFAULT и CHECK, сохраняя строки, историю V1 и генерацию ID. Настраивает HTTP Basic, пользователей в памяти с BCrypt-хешами из конфигурации, публичные маршруты и доступ по ролям через SecurityFilterChain; различает 401 и 403.

- Ближайшая практика: банковское API с пользователями и ролями в PostgreSQL, собственный UserDetailsService и получение текущего пользователя через Principal. Базовая сложность, ручная сборка с нуля; Docker и Testcontainers вводятся отдельно.

### Приобретённый стек

[![Spring Security](https://img.shields.io/badge/Spring_Security-HTTP_Basic_%26_roles-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)](https://docs.spring.io/spring-security/reference/6.5/servlet/index.html)
[![BCrypt](https://img.shields.io/badge/Passwords-BCrypt-6DB33F?style=flat-square)](https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html)

[![Flyway](https://img.shields.io/badge/Flyway-SQL_migrations-CC0200?style=flat-square&logo=flyway&logoColor=white)](https://documentation.red-gate.com/flyway/)
[![PostgreSQL JDBC](https://img.shields.io/badge/PostgreSQL-JDBC_driver-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://jdbc.postgresql.org/)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![BigDecimal](https://img.shields.io/badge/BigDecimal-Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)
[![Optional](https://img.shields.io/badge/Optional-Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html)
[![Stream API](https://img.shields.io/badge/Stream_API-Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html)
[![java.time](https://img.shields.io/badge/java.time-Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html)
[![JUnit 5](https://img.shields.io/badge/JUnit-5-25A162?style=flat-square&logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Git](https://img.shields.io/badge/Git-2-F05032?style=flat-square&logo=git&logoColor=white)](https://git-scm.com/)
[![GitHub](https://img.shields.io/badge/GitHub-Actions-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/Kriksson/javaLearnAI/actions)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-SQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![JDBC](https://img.shields.io/badge/JDBC-Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/package-summary.html)
[![HTTP Client](https://img.shields.io/badge/HTTP-Client-005571?style=flat-square&logo=openjdk&logoColor=white)](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html)
[![HTTP Server](https://img.shields.io/badge/HTTP-Server-005571?style=flat-square&logo=openjdk&logoColor=white)](https://docs.oracle.com/en/java/javase/21/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpHandler.html)
[![Jackson](https://img.shields.io/badge/Jackson-JSON-2E8B57?style=flat-square)](https://github.com/FasterXML/jackson)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Bean Validation](https://img.shields.io/badge/Bean_Validation-Jakarta-6DB33F?style=flat-square)](https://beanvalidation.org/)
[![Spring JDBC](https://img.shields.io/badge/Spring_JDBC-JdbcTemplate-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://docs.spring.io/spring-framework/reference/6.2/data-access/jdbc/core.html)
[![H2](https://img.shields.io/badge/H2-2.3.232-09476B?style=flat-square)](https://h2database.com/)
[![Generated IDs](https://img.shields.io/badge/Spring_JDBC-GeneratedKeyHolder-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html#jdbc-auto-generated-keys)
[![Spring Transactions](https://img.shields.io/badge/Spring-Transactional-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html)
[![Spring Data JPA](https://img.shields.io/badge/Spring_Data-JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://docs.spring.io/spring-data/jpa/reference/)
[![Derived queries](https://img.shields.io/badge/Spring_Data-Derived_queries-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://docs.spring.io/spring-data/jpa/reference/3.5/repositories/query-methods-details.html)
[![Combined filters](https://img.shields.io/badge/Spring_Data-And_filters-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://docs.spring.io/spring-data/jpa/reference/3.5/repositories/query-methods-details.html)
[![Range queries](https://img.shields.io/badge/Spring_Data-GreaterThanEqual-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://docs.spring.io/spring-data/jpa/reference/3.5/repositories/query-keywords-reference.html)
[![Dirty checking](https://img.shields.io/badge/JPA-Dirty_checking-59666C?style=flat-square)](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/transactions.html)
[![JPA Pagination](https://img.shields.io/badge/Spring_Data-Pageable_%26_Page-6DB33F?style=flat-square)](https://docs.spring.io/spring-data/commons/reference/3.5/repositories/query-methods-details.html)
[![EntityGraph](https://img.shields.io/badge/JPA-EntityGraph_%26_N%2B1-59666C?style=flat-square)](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/query-methods.html#jpa.entity-graph)
[![JPA relations](https://img.shields.io/badge/JPA-ManyToOne-59666C?style=flat-square)](https://jakarta.ee/specifications/persistence/)
[![Hibernate](https://img.shields.io/badge/Hibernate-ORM-59666C?style=flat-square&logo=hibernate&logoColor=white)](https://hibernate.org/orm/documentation/)
[![Conditional update](https://img.shields.io/badge/Spring_JDBC-UPDATE_row_count-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html)

Java 21 · BigDecimal · Optional · Stream API · java.time · JUnit 5 · Maven · Git · GitHub Actions · SQL · PostgreSQL · JDBC · HTTP Client · HTTP Server · Jackson · Spring Boot · Bean Validation · Spring JDBC · H2 · GeneratedKeyHolder · SQL IDENTITY · JdbcTemplate UPDATE row count · Spring Data JPA · Hibernate · ManyToOne · Spring Data derived queries · Flyway · Hibernate schema validation · Spring Security · HTTP Basic · BCrypt

- Подтверждённые навыки:
  - консольный ввод через `Scanner` и форматированный вывод;
  - методы, параметры и возвращаемые значения;
  - обработка строк: `trim`, регистр, `replace`, `substring`, `indexOf`, `lastIndexOf`;
  - условные конструкции и проверка данных;
  - разбор строки с заданной структурой без `split` и циклов;
  - отдельные классы, приватные поля, конструкторы и геттеры;
  - изменение состояния объекта и выбрасывание `IllegalArgumentException`;
  - композиция объектов и хранение сущностей в `List`/`ArrayList`;
  - цикл `for-each`, поиск, проверка уникальности и агрегирование данных коллекции;
  - перечисления `enum`;
  - хранение пар ключ–значение в `Map`/`HashMap`, обход ключей и изменение количества;
  - файловый ввод-вывод через `Path` и `Files` в UTF-8;
  - обработка `IOException`, сериализация объектов в строки и проверка формата файла;
  - наследование через `extends`, вызов `super` и переопределение методов;
  - полиморфные вызовы и расширение поведения базового класса.
  - абстрактные классы и абстрактные методы;
  - полиморфная работа с разными потомками в `List<CombatUnit>`.
  - переопределение `equals`, `hashCode` и `toString` для идентичности объектов;
  - хранение уникальных объектов в `Set`/`HashSet`.
  - перечисления `enum` с состоянием и конструктором;
  - статические поля, методы и константы класса.
  - параметр типа `T` и обобщённые классы;
  - типобезопасное хранение элементов в `List<T>`.
  - естественный порядок объектов через `Comparable<T>`;
  - сортировка копии коллекции через `Collections.sort`.
  - интерфейсы как общий контракт для разных классов;
  - полиморфная обработка объектов через `List<Upgradeable>`.
  - unit-тесты на JUnit 5 с `@Test`, `assertEquals` и `assertThrows`;
  - проверка обычных, граничных и ошибочных сценариев.
  - моделирование состояний через `enum` и допустимые переходы;
  - объединение классов, `Map` и тестов в мини-проект.
  - параметризованные unit-тесты с `@ParameterizedTest`, `@ValueSource` и `@NullSource`;
  - проверка набора входных данных без дублирования тестового кода.
  - анализ сообщений о падении unit-тестов;
  - поиск и точечное исправление ошибок в существующем коде.
  - Maven-координаты `groupId:artifactId:version`;
  - фазы Maven `clean`, `test` и `package`, создание JAR.
  - настройка Maven-плагинов через `build/plugins`;
  - анализ итоговой конфигурации через effective POM.
  - Maven-профили и свойства окружения;
  - включение профиля через `-P` и проверка свойства через `help:evaluate`.
  - создание Git-ветки, тематический коммит и публикация ветки в GitHub.
  - слияние готовой ветки в `main` через merge-коммит;
  - публикация обновлённой ветки `main`.
  - чтение состояния Git при конфликте слияния;
  - ручное разрешение конфликта и создание merge-коммита.
  - просмотр незакоммиченных изменений через `git diff`;
  - восстановление одного файла из `HEAD` через `git restore`.
  - неизменяемые модели данных через `record`;
  - составной `Comparator` и сортировка копии коллекции по нескольким полям.
  - агрегация данных в `Map` через `merge`;
  - неизменяемые снимки `Map` и сортировка записей `Map.Entry`.
  - точные денежные расчёты через `BigDecimal`;
  - сравнение и суммирование денежных значений без `double`.
  - представление отсутствующего результата через `Optional`;
  - API поиска без возврата `null`.
  - собственные checked-исключения и объявление `throws`;
  - сохранение целостного состояния при неудачной операции.
  - FIFO-очередь через `Deque` и `ArrayDeque`;
  - координация `Deque` и `Set` для уникальности элементов в очереди.
  - очередь с приоритетом через `PriorityQueue`;
  - создание упорядоченного снимка коллекции по `Comparator`.
  - лямбда-выражения и Stream-конвейеры;
  - агрегация данных через `groupingBy` и `summingInt`.
  - работа с календарными датами через `LocalDate`;
  - сравнение периодов и подсчёт дней через `ChronoUnit`.
  - проектирование таблиц SQL через `CREATE TABLE`;
  - первичные и внешние ключи, `NOT NULL`, `UNIQUE` и `CHECK`;
  - точные денежные значения через `DECIMAL` и целостность реляционных данных.
  - выборка и фильтрация данных через `SELECT` и `WHERE`;
  - сортировка результатов через `ORDER BY`;
  - группировка и агрегирование через `GROUP BY` и `SUM`.
  - объединение связанных таблиц через `LEFT JOIN`;
  - подсчёт связанных записей через `COUNT` с сохранением сущностей без связей.
  - фильтрация сгруппированных результатов через `HAVING`.
  - изменение существующих строк через `UPDATE` и `SET`;
  - условное удаление строк через `DELETE` и `WHERE`.
  - параметризованный поиск через JDBC `PreparedStatement` и чтение строк из `ResultSet`;
  - управление JDBC-ресурсами через `try-with-resources` без закрытия чужого соединения.
  - параметризованный `INSERT` через JDBC и `executeUpdate()`;
  - получение созданного базой ID через `RETURN_GENERATED_KEYS` и `getGeneratedKeys()`.
  - транзакции JDBC: `commit()`, `rollback()` и восстановление `autoCommit`;
  - проверка числа изменённых строк и откат при неудачном переводе.
  - HTTP `GET` через Java `HttpClient`, формирование URI и заголовка `Accept`;
  - обработка статусов HTTP и чтение тела ответа в UTF-8.
  - разбор JSON через Jackson `ObjectMapper` и `JsonNode`;
  - проверка структуры и типов JSON-полей с преобразованием в `record`.
  - отправка HTTP `POST` с JSON-телом и заголовком `Content-Type`;
  - сериализация JSON через Jackson `ObjectNode` и обработка ответа `201 Created`.
  - объединение HTTP `GET`, проверки статуса и разбора JSON в типизированный `Optional<PlayerProfile>`.
  - отправка результата матча через HTTP `POST` и обработка конфликта `409` без разбора тела ответа.
  - чтение JSON-ответа HTTP в UTF-8 и проверка соответствия ID ответа запрошенному ID.
  - проверка JSON boolean и обязательных полей ответа настроек игрока.
  - отправка JSON со специальными символами через Jackson и проверка согласованности ответа с запросом.
  - отправка JSON boolean и проверка согласованности нескольких полей HTTP-ответа.
  - обработка входящих HTTP-запросов через `HttpHandler` и `HttpExchange`;
  - формирование JSON-ответа сервера в UTF-8 со статусами `200`, `400`, `404` и `405`.
  - чтение JSON-тела входящего `POST`-запроса и обновление состояния по проверенным данным.
  - Spring MVC: `@RestController`, `@GetMapping`, `@PathVariable` и типизированный JSON-ответ через `ResponseEntity`.
  - Spring MVC: `@PostMapping`, `@RequestBody`, проверка полей входящего JSON и возврат `400` без тела.
  - Spring MVC: `@PutMapping`, обновление существующего ресурса и идемпотентность повторного запроса.
  - Spring MVC: `@RequestParam`, необязательные параметры URL, совместная фильтрация и сортировка JSON-массива.
  - Spring MVC: `@DeleteMapping`, ответ `204 No Content` и запрет удаления с `409 Conflict`.
  - Spring Boot: `@Service`, внедрение зависимости через конструктор и делегирование логики из контроллера.
  - Jakarta Bean Validation: `@Valid`, `@NotBlank`, `@NotNull`, `@Size`, `@Min` и `@Max` для входящего JSON.
  - каскадная валидация вложенных объектов через `@Valid` вместе с отдельной проверкой обязательности через `@NotNull`.
  - Spring Boot: привязка внешних настроек к `record` через `@ConfigurationProperties`, регистрация через `@ConfigurationPropertiesScan` и использование настройки в проверке вместимости.
  - Spring MVC: `@RestControllerAdvice`, `@ExceptionHandler` и единый JSON-формат ошибок валидации и разбора запроса.
  - базовый Spring Boot endpoint: `@GetMapping`, `@PathVariable`, внедрение сервиса через конструктор и перевод `Optional.empty()` в `404`.
  - проектирование Spring-компонентов по ролям: `@Repository`, `@Service`, `@RestController` и конструкторное внедрение;
  - хранение свойства награды в enum и формирование API-ответов из данных репозитория;
  - `201 Created`, заголовок `Location` и сериализация созданного ресурса в JSON;
  - реализация состояния ресурса через `PUT`, ответы `404` для отсутствующих ID и `409` для повторного действия;
  - интеграционные тесты REST-контракта через `MockMvc`, включая границы, невалидный JSON и неизвестные ID.
  - Spring JDBC: параметризованный `JdbcTemplate.query`, преобразование строки `ResultSet` в модель и поиск через `Optional`;
  - самостоятельная настройка H2 и создание таблицы с начальными данными через `schema.sql` при запуске Spring Boot;
  - связка REST API и SQL: чтение актуальной записи после изменения базы через другое соединение, без изменения данных при `GET`.
  - Spring JDBC: параметризованный `INSERT` через `JdbcTemplate.update` и проверка количества добавленных строк;
  - преобразование `DuplicateKeyException` в HTTP `409` с сохранением существующих записей;
  - регистрация рекрута из валидированного JSON с `201 Created`, `Location` и последующим чтением из SQL-таблицы.
  - получение выданного базой `BIGINT IDENTITY` через Spring JDBC `GeneratedKeyHolder`;
  - условный `UPDATE` через `JdbcTemplate.update`, проверка числа изменённых строк и ответы `204`/`404` для существующего и отсутствующего ID.

  - JPA-сущности с явным отображением колонок, генерация схемы Hibernate и поиск через Spring Data `JpaRepository.findById`.
  - создание JPA-сущности через `JpaRepository.save`, получение identity ID и REST-ответ `201 Created` с `Location`.

  - связь JPA-сущностей через `@ManyToOne` и `@JoinColumn`, вложенный JSON-ответ и целостность внешнего ключа.

  - производный метод Spring Data с поиском по ID связанной сущности и сортировкой, проверка родительской записи через `existsById`, преобразование списка сущностей в DTO.

  - производный запрос Spring Data с тремя условиями через `And`: ID гильдии, точное имя и уровень; проверка обязательных параметров URL, пустых строк, длины и отрицательных значений.

  - производный запрос с `GreaterThanEqual`, включение граничного уровня и порядок по уровню по убыванию, затем по ID по возрастанию; изоляция гильдий и проверка актуальных данных.

  - изменение связи managed JPA-сущности через dirty checking в транзакционном сервисе без `save()`, идемпотентный `PUT`, валидация nullable ID и `204`/`404`/`400`.

  - постраничная выборка через `Pageable` / `PageRequest` и `Page`, преобразование через `Page.map`, DTO страницы с `long totalElements`, пустые страницы и проверка параметров URL.

  - LAZY-связь и `@EntityGraph` для постраничного чтения без N+1, вложенный DTO гильдии и не более двух SQL-запросов с отключённым Open Session in View.

  - Spring Boot JPA с настоящим PostgreSQL, JDBC-драйвер, подключение из переменных окружения и сохранение данных между запусками контекста.

  - первая версионированная SQL-миграция Flyway, история применения и неизменность истории при повторных запусках; ограничения PostgreSQL и проверка схемы Hibernate через `ddl-auto=validate`.

  - обновление базы с данными с V1 до V2, добавление колонки с DEFAULT/NOT NULL/CHECK, сохранение строк, истории V1 и последовательности ID, обновление сущности и DTO.

  - HTTP Basic, BCrypt-хеши паролей из конфигурации, пользователи в памяти, SecurityFilterChain, публичный маршрут, hasAnyRole/hasRole и ответы 401/403; подтверждены 8 интеграционных тестов.

## Прогресс

- Решено задач: **85**
- Последняя решённая задача: [№85 — Первое подключение Spring Security](tasks/completed/task_085_security_recruit_read/README.md).

| № | Задача | Подтверждённые навыки | Статус |
|---:|---|---|---|
| 1 | Профиль игрового персонажа | `Scanner`, консольный ввод и вывод | решена |
| 2 | Тег игрового персонажа | методы, очистка и преобразование строк | решена |
| 3 | Игровой идентификатор | `substring`, проверки длины, композиция методов | решена |
| 4 | Парсер карточки персонажа | `indexOf`, `lastIndexOf`, разбор формата, валидация | решена |
| 5 | Класс игрового персонажа | отдельный класс, инкапсуляция, состояние объекта, конструктор, исключения | решена |
| 6 | Отряд персонажей | композиция классов, `List`, `ArrayList`, циклы, поиск и агрегирование | решена |
| 7 | Инвентарь игрока | `enum`, `Map`, `HashMap`, ключи, значения и подсчёты | решена |
| 8 | Файловый рейтинг игроков | `Path`, `Files`, UTF-8, `IOException`, сериализация | решена |
| 9 | Боевые классы персонажей | наследование, `super`, `@Override`, полиморфные вызовы | решена |
| 10 | Абстрактный боевой отряд | абстрактные классы и методы, полиморфизм в `List` | решена |
| 11 | Уникальные игроки | `equals`, `hashCode`, `toString`, `Set`/`HashSet` | решена |
| 12 | Игровые сессии | `enum`, статические поля, методы и константы | решена |
| 13 | Универсальный инвентарь | дженерики, параметр типа `T`, `List<T>` | решена |
| 14 | Отсортированный рейтинг | `Comparable`, естественный порядок, `Collections.sort` | решена |
| 15 | Мастерская улучшений | интерфейсы, полиморфизм через общий контракт | решена |
| 16 | Прогресс квеста и unit-тесты | JUnit 5, проверки обычных, граничных и ошибочных сценариев | решена |
| 17 | Журнал квестов | модель состояний, `Map`, unit-тесты, мини-проект | решена |
| 18 | Валидатор никнеймов | параметризованные тесты, проверка входных данных | решена |
| 19 | Отладка расчёта награды | анализ тестов, поиск и исправление ошибок | решена |
| 20 | Maven-координаты и сборка JAR | Maven, жизненный цикл сборки, JAR | решена |
| 21 | Maven Compiler Plugin | Maven-плагины, Compiler Plugin, effective POM | решена |
| 22 | Maven-профиль development | Maven-профили, свойства окружения | решена |
| 23 | Git-ветка для изменения | Git-ветка, тематический коммит и push | решена |
| 24 | Слияние готовой ветки | `git switch`, `git merge --no-ff`, публикация `main` | решена |
| 25 | Разрешение конфликта Git | конфликт слияния, выбор итогового содержимого, merge-коммит | решена |
| 26 | Отмена локального изменения | `git diff`, `git restore`, проверка состояния файла | решена |
| 27 | История боёв | `record`, `Comparator`, неизменяемые списки, сортировка по нескольким полям | решена |
| 28 | Статистика добычи | `Map.merge`, агрегация, `Map.Entry`, неизменяемые снимки | решена |
| 29 | Корзина покупок | `BigDecimal`, `Map<Product, Integer>`, точный итог заказа | решена |
| 30 | Каталог игроков | `Optional`, поиск в `Map`, замена профиля по ключу | решена |
| 31 | Склад ресурсов | checked-исключение, `throws`, целостность состояния | решена |
| 32 | Очередь матчмейкинга | `Deque`, `ArrayDeque`, FIFO, уникальность через `Set` | решена |
| 33 | Очередь обращений | `PriorityQueue`, приоритеты, `Optional`, снимок по компаратору | решена |
| 34 | Аналитика урона | Stream API, лямбды, `groupingBy`, `summingInt` | решена |
| 35 | Сезонные пропуска | `LocalDate`, границы периода, `ChronoUnit` | решена |
| 36 | Схема заказов игроков | SQL: таблицы, ключи, ограничения, внешние ключи | решена |
| 37 | Отчёты по заказам игроков | SQL: `SELECT`, `WHERE`, `ORDER BY`, `SUM`, `GROUP BY` | решена |
| 38 | Подсчёт заказов игроков | SQL: `LEFT JOIN`, `COUNT`, агрегирование связанных таблиц | решена |
| 39 | Игроки с заданными расходами | SQL: `HAVING`, фильтрация агрегированных групп | решена |
| 40 | Обновление статусов заказов | SQL: `UPDATE`, `DELETE`, условие `WHERE` для изменения строк | решена |
| 41 | Поиск игрока через JDBC | JDBC: параметризованный запрос, чтение `ResultSet` | решена |
| 42 | Регистрация игрока через JDBC | JDBC: параметризованный `INSERT`, сгенерированный ID | решена |
| 43 | Перевод монет через JDBC | JDBC: транзакции, `commit`, `rollback`, целостность данных | решена |
| 44 | Загрузка профиля игрока по HTTP | HTTP: GET, статусы ответа, Java `HttpClient` | решена |
| 45 | Разбор JSON-профиля игрока | JSON: `ObjectMapper`, `JsonNode`, проверка структуры | решена |
| 46 | Регистрация игрока через HTTP API | HTTP `POST`, JSON-запрос, разбор ответа | решена |
| 47 | Профиль игрока: HTTP + JSON | `GET`, JSON, `Optional`, проверка ответов | решена |
| 48 | Отправка результата матча | `POST`, JSON, `Optional`, проверка ответов | решена |
| 49 | Получение результата матча | `GET`, JSON, `Optional`, проверка ID ответа | решена |
| 50 | Настройки игрока через HTTP | `GET`, JSON boolean, `Optional`, проверка полей | решена |
| 51 | Приглашение игрока через HTTP | `POST`, JSON, `Optional`, проверка ответа | решена |
| 52 | Ответ на приглашение через HTTP | `POST`, JSON boolean, `Optional`, согласованность полей | решена |
| 53 | Первый серверный API игроков | `HttpHandler`, `HttpExchange`, JSON-ответ | решена |
| 54 | Приём результата игрока через HTTP | `POST`, входящий JSON, обновление рекорда | решена |
| 55 | Первый контроллер Spring Boot | Spring MVC, маршруты, автоматический JSON | решена |
| 56 | Расчёт очков через Spring Boot | `@PostMapping`, `@RequestBody`, проверка DTO | решена |
| 57 | Обновление профиля игрока через Spring Boot | `@PutMapping`, путь, JSON-запрос и состояние | решена |
| 58 | Поиск игроков через параметры запроса | `@RequestParam`, фильтрация, список JSON | решена |
| 59 | Удаление игрока через Spring Boot | `@DeleteMapping`, `204 No Content`, конфликт `409` | решена |
| 60 | Начисление очков через сервис Spring Boot | `@Service`, внедрение зависимости, разделение логики | решена |
| 61 | Проверка заявки через Bean Validation | `@Valid`, декларативные ограничения полей | решена |
| 62 | Единый ответ об ошибках Spring Boot | `@RestControllerAdvice`, `@ExceptionHandler` | решена |
| 63 | Награда за квест через Spring Boot | `@RestController`, `@GetMapping`, `@Service`, внедрение зависимости | решена |
| 64 | Самостоятельный мини-проект: API каталога квестов | проектирование слоёв, `@Repository`, `@Service`, enum с наградой и REST-контроллер | решена |
| 65 | Самостоятельный мини-проект: доска заявок гильдии | проектирование Spring-компонентов, состояние заявки, `201 Created`, `Location`, `MockMvc` | решена |
| 66 | Заявка на рейд с вложенной валидацией | каскадная проверка вложенного DTO через `@Valid` и `@NotNull`, проверка JSON границ через `MockMvc` | решена |
| 67 | Лимит мест в рейде из конфигурации | `@ConfigurationProperties`, сканирование настроек и лимит отдельно для каждого рейда | решена |
| 68 | Рекрут по ID через Spring JDBC | самостоятельная сборка приложения, `JdbcTemplate.query`, H2, `schema.sql`, актуальные данные из SQL | решена |
| 69 | Регистрация рекрута через Spring JDBC | `JdbcTemplate.update`, параметризованный `INSERT`, `DuplicateKeyException`, валидация, `201` и `409` | решена |
| 70 | ID рекрута создаёт база | SQL `IDENTITY`, `GeneratedKeyHolder`, фактический `BIGINT ID` в `Location` и JSON | решена |
| 71 | Изменение уровня рекрута через Spring JDBC | условный `JdbcTemplate.update`, число изменённых строк, `204`, `404` и сохранение остальных полей | решена |
| 72 | Атомарный перевод монеты между сундуками | Spring `@Transactional`, откат нескольких SQL-изменений, `409`/`404`, проверка транзакции интеграционным тестом | решена |
| 73 | Найти хранителя через Spring Data JPA | JPA-сущность и явное отображение колонок, Spring Data `findById`, генерация схемы Hibernate, чтение через REST и MockMvc | решена |
| 74 | Создать хранителя через Spring Data JPA | `JpaRepository.save`, identity ID, `201 Created`, `Location`, интеграционная проверка через MockMvc | решена |

| 75 | Хранитель в гильдии через JPA-связь | `@ManyToOne`, `@JoinColumn`, внешний ключ, вложенный JSON и `404` | решена |

| 76 | Хранители одной гильдии | производный запрос по связи, `OrderBy`, `existsById`, DTO, актуальные данные и `200`/`404` | решена |

| 77 | Поиск хранителей в гильдии | три условия через `And`, путь по связи, точное сравнение, валидация параметров URL и DTO | решена |

| 78 | Хранители для сложного рейда | `GreaterThanEqual`, два поля в `OrderBy`, границы уровня, изоляция гильдий и DTO | решена |

| 79 | Перевод хранителя в другую гильдию | managed entity, dirty checking, транзакционный сервис, изменение связи без `save()`, идемпотентный `PUT` | решена |

| 80 | Хранители гильдии по страницам | `Pageable`, `PageRequest`, `Page`, `Page.map`, общие количества, стабильные страницы и DTO | решена |

| 81 | Страница хранителей без N+1 | LAZY, `@EntityGraph`, вложенный DTO, отключённый Open Session in View, проверка SQL-запросов | решена |
| 82 | Рекруты в PostgreSQL | Внешний PostgreSQL, JDBC-драйвер, переменные окружения, сохранение схемы и данных между запусками | решена |
| 83 | Первая миграция Flyway | V1, история миграций, SQL-ограничения, `ddl-auto=validate`, сохранение данных и проверка несовместимой схемы | решена |
| 84 | Миграция V2: монеты существующих рекрутов | ALTER TABLE, DEFAULT/NOT NULL/CHECK, сохранение данных и истории V1, обновление сущности и DTO | решена |
| 85 | Первое подключение Spring Security | HTTP Basic, BCrypt, пользователи в памяти, SecurityFilterChain, доступ по ролям и 401/403 | решена |

## Последняя решённая задача

[Открыть архив задачи №85](tasks/completed/task_085_security_recruit_read/README.md).

## Текущая задача

№85 завершена. Следующая задача №86 готовится: банк, пользователи в PostgreSQL и вход через Spring Security.

## Структура

```text
tasks/
  active/       текущее задание
  completed/    выполненные задания с решениями и тестами
src/main/java/  рабочие файлы текущего задания
src/test/java/  тесты текущего задания
```
