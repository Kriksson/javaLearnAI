# Задача №85 — Первое подключение Spring Security

**Сложность: базовая. HTTP Basic и чтение по ролям.**

Самостоятельно собери приложение с нуля в пакете `learning.task085`. Публичное описание гильдии может прочитать любой человек. Данные рекрутов доступны читателю и администратору; общая численность гильдии — только администратору.

Закрепляем: PostgreSQL, Flyway, Hibernate `validate`, JPA, DTO, актуальные данные, большие ID, внешний конфиг и MockMvc.

Новые понятия: **проверка учётных данных через HTTP Basic** и **разрешение доступа по ролям в Spring Security**. Пользователи безопасности пока хранятся в памяти; пароли проверяются через BCrypt. Рекруты продолжают храниться в PostgreSQL. Регистрация, JWT и запись через API в эту задачу не входят.

## Что происходит перед контроллером

Spring Security добавляет фильтры перед обработкой HTTP-запроса контроллером. Клиент передаёт заголовок `Authorization: Basic ...`, содержащий Base64 от строки `логин:пароль`. Base64 кодирует строку; для шифрования соединения реального сервера нужен HTTPS.

Spring сначала проверяет логин и пароль — это аутентификация. Затем проверяет, разрешено ли этому пользователю обращаться к выбранному URL, — это авторизация.

| Ситуация | Результат |
|---|---|
| Нет учётных данных или пароль неверен для защищённого URL | `401 Unauthorized`, заголовок `WWW-Authenticate: Basic ...` |
| Логин и пароль верны, но подходящей роли нет | `403 Forbidden` |
| Учётные данные и роль подходят | Запрос доходит до контроллера |

Если пользователь без пароля спрашивает отсутствующего рекрута, защищённый endpoint сначала вернёт `401`. Авторизованный читатель получит от контроллера `404`: проверка доступа уже пройдена. [HTTP Basic в Spring Security](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/basic.html).

### Как назначаются роли: отдельный пример с документами

Разделим три понятия. **Логин** отвечает на вопрос «кто вошёл?», **роль** — «какие действия этому пользователю разрешены?», **ресурс** — «к каким данным он обращается?». Например, пользователь `alex` читает документы с ролью `VIEWER`, а пользователь `nina` с ролью `MANAGER` также может читать отчёты. Логин, роль и название документа независимы друг от друга.

| Логин пользователя | Назначенная роль | Что разрешаем в этом примере |
|---|---|---|
| `alex` | `VIEWER` | Чтение документов |
| `nina` | `MANAGER` | Чтение документов и отчётов |

Название роли выбираешь ты. Spring не знает, что означает слово `VIEWER`, пока ты не напишешь правила доступа. В этой практике роль задаётся строкой при создании пользователя: отдельный Java-класс роли, таблица ролей или SQL-команда не нужны.

У назначения доступа две части:

1. При создании пользователя присвоить ему роль через `.roles("VIEWER")`.
2. При настройке URL потребовать эту роль через `.hasRole("VIEWER")` или допустить несколько ролей через `.hasAnyRole(...)`.

Первая часть сохраняет сведения о пользователе; вторая использует эти сведения для проверки запроса. Одного назначения роли недостаточно, чтобы ограничить конкретный URL.

В отдельном Spring Boot приложении уже есть GET `/status`, GET `/documents/{id}` и GET `/reports/{id}`. Подключена зависимость `spring-boot-starter-security`. Ниже полная конфигурация безопасности для этого отдельного примера:

```java
package example.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecuritySettings {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder,
                            @Value("${demo.alex-password}") String firstPassword,
                            @Value("${demo.nina-password}") String secondPassword) {
        var firstAccount = User.withUsername("alex")
                .password(encoder.encode(firstPassword))
                .roles("VIEWER")
                .build();

        var secondAccount = User.withUsername("nina")
                .password(encoder.encode(secondPassword))
                .roles("MANAGER")
                .build();

        return new InMemoryUserDetailsManager(firstAccount, secondAccount);
    }

    @Bean
    SecurityFilterChain access(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(rules -> rules
                        .requestMatchers("/status").permitAll()
                        .requestMatchers("/documents/**").hasAnyRole("VIEWER", "MANAGER")
                        .requestMatchers("/reports/**").hasRole("MANAGER")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
```

В конфигурации отдельного примера заданы свойства:

```properties
demo.alex-password=${DEMO_ALEX_PASSWORD}
demo.nina-password=${DEMO_NINA_PASSWORD}
```

Эти переменные окружения содержат пароли двух пользователей API. Пример остаётся в учебном материале; среди файлов решения его не размещай.

**Что создаётся при запуске приложения:**

- `@Configuration` отмечает класс настройки. `@Bean` сообщает Spring, что возвращённый объект нужно зарегистрировать для использования другими компонентами.
- Метод `passwordEncoder` создаёт объект, который умеет получать BCrypt-хеш и проверять пароль по хешу. Spring передаёт этот объект в параметр `encoder` метода `users`.
- `@Value` получает пароли из свойств приложения; `firstPassword` и `secondPassword` — обычные параметры Java-метода.
- `User.withUsername("alex")` начинает сборку объекта с логином `alex`. `User` здесь — готовый класс Spring Security, а не JPA-сущность приложения.
- `.password(encoder.encode(firstPassword))` записывает в объект хеш пароля. При входе Spring проверяет присланный пароль через `PasswordEncoder.matches`.
- `.roles("VIEWER")` назначает пользователю роль. Внутри объекта она хранится как право с именем `ROLE_VIEWER`; префикс добавляется автоматически.
- `.build()` завершает сборку и возвращает `UserDetails` — сведения для проверки входа: логин, хеш пароля, права и состояние учётной записи. `firstAccount` — имя Java-переменной, оно не участвует во входе.
- `InMemoryUserDetailsManager` получает оба объекта и хранит пользователей в памяти приложения. Он реализует `UserDetailsService`: при входе этот сервис позволяет найти сведения по логину.

**Как используются роли при запросе:**

`requestMatchers` выбирает URL, к которым относится следующее правило. `hasRole("MANAGER")` требует право `ROLE_MANAGER`. `hasAnyRole("VIEWER", "MANAGER")` пропускает пользователя, если у него есть хотя бы одна из этих ролей. Правила рассматриваются по порядку; применяется первое совпавшее. `permitAll` открывает маршрут всем, `authenticated` требует успешного входа. `httpBasic` включает проверку заголовка Basic; `build()` создаёт цепочку фильтров.

| Запись в коде | Значение при стандартном префиксе |
|---|---|
| `.roles("VIEWER")` | Назначить пользователю право `ROLE_VIEWER` |
| `.hasRole("VIEWER")` | Проверить наличие права `ROLE_VIEWER` |
| `.hasAnyRole("VIEWER", "MANAGER")` | Проверить наличие хотя бы одного из двух прав |

В `roles` передавай имя без `ROLE_`; в `hasRole` здесь тоже используется имя без префикса. Написание должно совпадать: `VIEWER` и `Viewer` — разные строки. Назначение роли `MANAGER` само по себе не добавляет роль `VIEWER`; поэтому доступ к документам явно разрешён обеим ролям. Несколько пользователей могут иметь одну роль, а одному пользователю можно назначить несколько: `.roles("VIEWER", "MANAGER")`.

Полный путь запроса:

```shell
curl -i -u alex http://localhost:8080/documents/12
```

1. `curl` спрашивает пароль и добавляет заголовок Basic с логином и паролем. Роль клиент не передаёт.
2. Фильтр Spring Security извлекает логин `alex`; через `UserDetailsService` получает его сведения из памяти.
3. Spring проверяет присланный пароль по сохранённому хешу. При неверном пароле защищённый запрос получает `401`.
4. После успешной проверки пользователь считается вошедшим; у него есть право `ROLE_VIEWER`.
5. Для `/documents/12` действует `hasAnyRole("VIEWER", "MANAGER")`. Проверка проходит, запрос доходит до контроллера. Если документ существует, контроллер возвращает его DTO с `200`.
6. Для `/reports/12` нужна роль `MANAGER`. У `alex` её нет, поэтому ответ — `403`, контроллер не вызывается. Пользователь `nina` с правильным паролем проходит эту проверку.

Ты пишешь создание пользователей с ролями и правила доступа к URL. Проверку заголовка, поиск пользователя при входе, сравнение паролей и проверку назначенных прав выполняет Spring Security. Названия логинов и ролей никак не связываются автоматически.

[Пользователи в памяти](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/in-memory.html), [назначение ролей через UserBuilder](https://docs.spring.io/spring-security/site/docs/6.5.6/api/org/springframework/security/core/userdetails/User.UserBuilder.html), [правила доступа](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/authorize-http-requests.html), [PasswordEncoder и BCrypt](https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html).

## Пользователи и настройки

Вручную подключи `org.springframework.boot:spring-boot-starter-security` версии `${spring.boot.version}` в текущий Maven-проект. Остальные необходимые зависимости проверяешь сам; Java остаётся 21.

Создай конфигурацию Spring Security, `SecurityFilterChain`, bean `PasswordEncoder` и `UserDetailsService` с `InMemoryUserDetailsManager`. Форма входа не требуется; используй HTTP Basic. Остальные настройки безопасности оставляй стандартными. Имена внутренних классов и методов выбираешь сам.

| Логин | Единственная роль | Пароль |
|---|---|---|
| `reader` | `READER` | Из свойства `guild.security.reader-password` |
| `admin` | `ADMIN` | Из свойства `guild.security.admin-password` |

Оба пользователя читают рекрутов. Роль ADMIN сама по себе не означает наличие READER: правило чтения должно допускать обе роли. В `UserDetails` сохраняй BCrypt-хеши, полученные через `PasswordEncoder` из настроенных паролей. Открытые пароли, `{noop}` и вручную зафиксированные хеши вместо чтения конфигурации не подходят. Пользователей безопасности в PostgreSQL пока не создавай.

Вручную создай `src/main/resources/application.properties`:

| Свойство | Значение |
|---|---|
| `spring.datasource.url` | `${COURSE_PG_URL}` без значения по умолчанию |
| `spring.datasource.username` | `${COURSE_PG_USER}` |
| `spring.datasource.password` | `${COURSE_PG_PASSWORD}` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |
| `spring.jpa.open-in-view` | `false` |
| `guild.security.reader-password` | `${COURSE_READER_PASSWORD}` без значения по умолчанию |
| `guild.security.admin-password` | `${COURSE_ADMIN_PASSWORD}` без значения по умолчанию |

Для обычного запуска приложения нужны все пять переменных. Тесты используют твои `COURSE_PG_*`, а два пароля API задают собственными значениями через свойства тестового контекста: реальные пароли пользователей приложения им не нужны.

## PostgreSQL и миграции

Сам перенеси обе миграции №84 в `src/main/resources/db/migration/` **без изменений**:

- `V1__create_recruits.sql`;
- `V2__add_recruit_coins.sql`.

Исходники находятся в `tasks/completed/task_084_flyway_recruit_coins/src/main/resources/db/migration/`. Новые версии миграций не нужны. Таблица `recruits` по-прежнему содержит `id BIGINT IDENTITY PRIMARY KEY`, `name VARCHAR(40) NOT NULL`, `level INTEGER NOT NULL CHECK (level >= 0)`, `coins INTEGER NOT NULL DEFAULT 0 CHECK (coins >= 0)`.

Используй явное отображение JPA-сущности на таблицу/колонки и ID типа `Long`. Чтение по ID и количества — через Spring Data JPA; у репозитория есть стандартный метод `count()`, возвращающий `long`. Рекруты появляются через клиент базы или тесты: начальные записи при запуске не добавляй. Стандартная автоконфигурация Flyway и Hibernate `validate` сохраняются. Не задавай фиксированную схему в SQL или `@Table`, отдельное подключение Flyway, очистку базы или автоматическое создание таблиц Hibernate.

## HTTP-контракт

Нужны три GET-endpoint. Формат ошибок свободный. Все запросы только читают данные.

### GET /guild/info

Публичный маршрут. Без заголовка Authorization, а также с корректными данными любого из двух пользователей → `200`, `Content-Type: application/json`, ровно:

```json
{"name":"Training Guild"}
```

### GET /recruits/{id}

Доступен пользователям `reader` и `admin` с верными паролями.

- Существующий ID → `200`, JSON с ровно `id`, `name`, `level`, `coins`.
- Отсутствующий или удалённый ID → `404` после успешной проверки доступа.
- Без учётных данных, неизвестный логин, неверный или пустой пароль → `401` с `WWW-Authenticate`, начинающимся с `Basic`. Так же отвечай при запросе отсутствующего ID без доступа.
- ID имеет диапазон `long`, включая значения больше `Integer.MAX_VALUE`.
- Возвращай текущие значения PostgreSQL, включая уровень/монеты 0 и `Integer.MAX_VALUE`, имя длиной 40 символов, кириллицу, кавычки и пробелы вокруг непустого имени. Внешние изменение и удаление отражаются в следующем GET.
- Используй DTO, не возвращай сущность напрямую. Пароли, хеши и роли в DTO рекрута не включай.

### GET /guild/recruit-count

- Пользователь `admin` с верным паролем → `200`, JSON с ровно `count`: актуальное количество рекрутов. В DTO количество имеет тип `long`. Пустая таблица → `{"count":0}`.
- Пользователь `reader` с верным паролем → `403`, в том числе при пустой таблице.
- Нет учётных данных или они неверны → `401` и заголовок Basic.

Для остальных URL настрой общее требование аутентификации. Например, анонимный GET `/unmapped` → `401`. Все успешные и отклонённые запросы должны сохранять строки рекрутов без изменений. Проверку логина/пароля/роли выполняет Spring Security.

## Примеры

1. GET `/guild/info` без пароля → `200`; GET `/recruits/12` без пароля → `401`.
2. Верные данные `reader` и существующий рекрут 12 → `200` с DTO. Тот же пользователь запрашивает `/guild/recruit-count` → `403`.
3. Верные данные `admin` и три строки в базе → `/guild/recruit-count` возвращает `{"count":3}`. GET отсутствующего рекрута → `404`; неверный пароль → `401`.

Проверка через терминал:

```shell
curl -i http://localhost:8080/guild/info
curl -i -u reader http://localhost:8080/recruits/12
curl -i -u admin http://localhost:8080/guild/recruit-count
```

`curl -u` с одним логином запросит пароль отдельно. Для проверки в Postman выбери Authorization → Basic Auth, введи логин и пароль.

## Файлы, тесты и готовность

Сам создавай файлы приложения в `src/main/java/learning/task085/`: стартовый класс, сущность, репозиторий, контроллер, DTO и конфигурацию безопасности; каждый самостоятельный класс — отдельный файл. Сервис добавляй по необходимости. Конфигурацию, зависимости и перенос миграций также выполняешь сам.

Тесты тренера:

- `src/test/java/learning/task085/SecureRecruitReadApiTest.java` — 8 интеграционных тестов и предварительные проверки конфигурации/миграций/схемы;
- `src/test/java/learning/task085/PostgresTestDatabase.java` — отдельная случайная схема `task085_<идентификатор>` для каждого запуска.

Тесты используют настоящий PostgreSQL, направляют приложение в собственную схему и удаляют только её. Данные обычного запуска не используются. Авторизация проверяется настоящими заголовками Basic через фильтры Spring Security: тесты не подставляют готового пользователя и не отключают фильтры. Отдельно проверяются роли и BCrypt-хеши настроенных тестовых паролей через стандартные интерфейсы Spring Security; имена твоих классов свободны.

H2, `JdbcTemplate`, `@Query`, ручной SQL для чтения рекрутов и локальное хранение рекрутов в приложении не используются. Тесты применяют JDBC для независимой подготовки и проверки строк. Ограничения и использование DTO дополнительно проверяются при просмотре кода.

Готовность: публичный и защищённые маршруты, корректные `401`/`403`/`404`/`200`, оба пользователя и правильные роли, пароли из конфигурации в виде BCrypt-хешей, актуальные DTO и количество, сохранение базы без изменений, все тесты прошли. До успешной проверки задача не засчитывается.

До реализации ожидается ошибка отсутствующей Spring Boot конфигурации или зависимостей/миграций. Для тестов №85 задай `COURSE_PG_URL`, `COURSE_PG_USER`, `COURSE_PG_PASSWORD` в новой конфигурации IntelliJ или терминале. Для обычного запуска приложения дополнительно задай `COURSE_READER_PASSWORD` и `COURSE_ADMIN_PASSWORD`; пароль PostgreSQL и пароли API — разные настройки.

Запуск из корня проекта с уже заданными переменными PostgreSQL:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```
