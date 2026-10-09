# №87 — Банк: регистрация клиента

Сложность: **базовая**. Приложение снова проектируешь и собираешь самостоятельно в `learning.task087`.

Закрепляем: PostgreSQL, Flyway, JPA, identity ID, DTO, Bean Validation, `201`/`400`/`409`, HTTP Basic, `UserDetailsService`, `Principal` и роли.

Два новых приёма:

1. Регистрация учётной записи: пароль приходит открытым в запросе, перед сохранением превращается в BCrypt-хеш; роль назначает сервер.
2. Стандартная CSRF-защита Spring Security: получение токена и отправка изменяющего запроса с этим токеном и той же HTTP-сессией.

## Сценарий

В №86 пользователей добавляли тесты через базу. Теперь посетитель должен сам зарегистрироваться через API, а затем войти с тем же логином и паролем. Новая учётная запись всегда получает `CLIENT`. Роль `AUDITOR` позволяет видеть количество клиентов; посетитель не должен получать её, добавив поле в JSON.

Реальные деньги, счета и переводы здесь ещё не моделируем. Фокус — небольшой законченный путь регистрации и входа.

## Как связаны регистрация и вход

```text
JSON username + password
  → Spring MVC читает JSON в DTO
  → твоя валидация проверяет поля
  → твой код вызывает PasswordEncoder.encode(password)
  → твой код создаёт сущность с хешем и ролью CLIENT
  → JPA сохраняет строку, PostgreSQL выдаёт ID
  → контроллер возвращает DTO с ID, логином и ролью

следующий запрос с HTTP Basic
  → Spring Security получает логин и открытый пароль
  → твой UserDetailsService читает строку из PostgreSQL
  → возвращает сохранённый хеш и роль
  → Spring Security сравнивает пароль с хешем через matches
  → разрешает запрос; Principal содержит логин
```

`encode` нужен при создании пароля. В `UserDetailsService` повторно хешировать прочитанный хеш нельзя. При входе Spring Security сам проверяет пароль. Эти операции уже знакомы по №85–86; теперь соединяем их с записью через JPA.

У BCrypt есть случайная соль: два вызова `encode` для одного пароля обычно дают разные хеши, но `matches` принимает оба. Поэтому проверяем пароль через `matches`, а не сравнением `encode(password).equals(сохранённыйХеш)`. [Хранение паролей в Spring Security](https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html).

### Отдельный пример: секрет профиля социальной сети

В отдельном эксперименте с зависимостью Spring Security можно запустить такой класс. Он не создаёт банковское API и не сохраняет строки базы.

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordExperiment {
    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String firstHash = encoder.encode("Social_42!");
        String secondHash = encoder.encode("Social_42!");

        System.out.println(firstHash.equals(secondHash)); // обычно false
        System.out.println(encoder.matches("Social_42!", firstHash)); // true
        System.out.println(encoder.matches("another", firstHash)); // false
    }
}
```

- `PasswordEncoder` — интерфейс, `BCryptPasswordEncoder` — его реализация.
- `encode` получает открытый пароль и возвращает строку хеша. В базу отправляется именно эта строка.
- `matches` получает открытый пароль и сохранённый хеш, возвращает результат проверки.
- Роль не вычисляется из пароля и не берётся из данных посетителя: её назначает сервер по правилам регистрации.

В задаче ты сам пишешь проверку входного DTO, преобразование пароля, создание сущности, сохранение и DTO ответа.

## Почему permitAll недостаточно для POST

`permitAll()` отвечает на вопрос: «Нужна ли пользователю аутентификация для этого маршрута?» CSRF отвечает на другой вопрос: «Есть ли у изменяющего запроса правильный токен этой сессии?» Публичная регистрация может требовать CSRF-токен.

CSRF защищает от ситуации, когда сторонняя страница заставляет браузер отправить нежелательный запрос с автоматически добавленными учётными данными. HTTP Basic тоже может использоваться браузером, поэтому сам по себе не является основанием отключать CSRF. В этой практике оставляем стандартную защиту Spring Security. [Назначение и работа CSRF](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html).

Путь запроса:

1. Клиент делает публичный `GET /bank/csrf`.
2. Spring Security предоставляет `CsrfToken`; при чтении значения сохраняет ожидаемый токен в HTTP-сессии.
3. Контроллер возвращает название заголовка и значение токена в JSON. Браузер хранит cookie с ID сессии; в MockMvc тест удерживает объект этой сессии.
4. Клиент делает `POST /bank/register`, передавая ту же сессию и токен в заголовке `X-CSRF-TOKEN`.
5. Фильтр Spring Security проверяет токен **до контроллера**. Если токен отсутствует, неверен или относится к другой сессии, ответ — `403`.
6. Если токен подходит, Spring продолжает запрос: чтение DTO, валидация и регистрация.

Токен не является паролем и не назначает роль. Сессия с CSRF-токеном не означает, что пользователь вошёл. Для защищённых маршрутов по-прежнему нужен HTTP Basic. Стандартный Spring Security может возвращать разные текстовые представления токена при повторных GET; не сравнивай их между собой. Используй полученное значение вместе с его сессией. [Хранение токена в сессии и его обработка](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html#csrf-token-repository).

### Отдельный работающий пример: настройка уведомлений

Пример предназначен для **отдельного минимального Spring Boot проекта** с `spring-boot-starter-web` и `spring-boot-starter-security`. Каждый класс/record — отдельный файл в одном пакете. Здесь нет банковских пользователей и базы данных.

`DemoLauncher.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoLauncher {
    public static void main(String[] args) {
        SpringApplication.run(DemoLauncher.class, args);
    }
}
```

`DemoSecurity.java`:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class DemoSecurity {
    @Bean
    SecurityFilterChain rules(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(access -> access
                .requestMatchers("/demo/**").permitAll()
                .anyRequest().denyAll())
                .build();
    }
}
```

`TokenEnvelope.java`, `PreferenceInput.java`, `PreferenceOutput.java`:

```java
public record TokenEnvelope(String headerName, String token) { }
```

```java
public record PreferenceInput(boolean notifications) { }
```

```java
public record PreferenceOutput(boolean notifications) { }
```

`PreferenceEndpoint.java`:

```java
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class PreferenceEndpoint {
    @GetMapping("/demo/token")
    TokenEnvelope token(CsrfToken csrf) {
        return new TokenEnvelope(csrf.getHeaderName(), csrf.getToken());
    }

    @PostMapping("/demo/preferences")
    PreferenceOutput preview(@RequestBody PreferenceInput input) {
        return new PreferenceOutput(input.notifications());
    }
}
```

Разбор ключевых строк:

- `permitAll` делает демонстрационные маршруты публичными. CSRF при этом остаётся включённым.
- Параметр `CsrfToken csrf` заполняет Spring из атрибутов текущего запроса, созданных фильтром безопасности. Ты не создаёшь токен через `new`.
- `getHeaderName()` сообщает клиенту имя заголовка, `getToken()` — значение для следующего запроса.
- `@RequestBody` преобразует JSON `{"notifications":true}` в `PreferenceInput`.
- Вызов `preview` происходит только после успешной проверки CSRF. Ответ `{"notifications":true}` здесь лишь демонстрирует прохождение запроса.

Для проверки примера в терминале:

```shell
curl -s -c /tmp/demo-cookies.txt http://localhost:8080/demo/token
```

Скопируй поле `token` из ответа и отправь:

```shell
curl -i -b /tmp/demo-cookies.txt \
  -H 'X-CSRF-TOKEN: ВСТАВЬ_ПОЛУЧЕННЫЙ_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"notifications":true}' \
  http://localhost:8080/demo/preferences
```

С cookie и правильным токеном → `200`. Убери заголовок или файл cookie → `403`. Здесь `-c` сохраняет cookie, `-b` отправляет её обратно. В Postman используется cookie jar и тот же заголовок. Для банка путь регистрации и JSON будут другими.

## База и конфигурация

Самостоятельно создай единственную миграцию `src/main/resources/db/migration/V1__create_bank_users.sql`. Таблица имеет тот же контракт, что в №86:

| Колонка | Требование |
|---|---|
| `id` | `BIGINT GENERATED BY DEFAULT AS IDENTITY`, PRIMARY KEY; Java ID — `Long` |
| `username` | `VARCHAR(40)`, NOT NULL, UNIQUE, CHECK длины больше 0 |
| `password_hash` | `VARCHAR(100)`, NOT NULL; BCrypt-хеш |
| `role` | `VARCHAR(16)`, NOT NULL, CHECK только `CLIENT` или `AUDITOR` |

Начальных строк нет. Тесты добавляют свои учётные записи в отдельную случайную схему и удаляют только эту схему после запуска. Миграция не задаёт фиксированное имя схемы; `@Table` тоже его не задаёт. Для ручного запуска используй отдельную учебную базу; не заменяй применённые миграции предыдущих задач.

В самостоятельно созданном `src/main/resources/application.properties`:

| Свойство | Значение |
|---|---|
| `spring.datasource.url` | `${COURSE_PG_URL}` |
| `spring.datasource.username` | `${COURSE_PG_USER}` |
| `spring.datasource.password` | `${COURSE_PG_PASSWORD}` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |
| `spring.jpa.open-in-view` | `false` |

Учетные записи, роли и хеши берутся только из PostgreSQL через JPA. Используй собственный bean `UserDetailsService` и BCrypt `PasswordEncoder`. При последующих запросах изменения пароля, роли или удаление пользователя должны учитываться без перезапуска.

## HTTP-контракт

Формат тела ошибки свободный. Успешные ответы содержат **только** указанные поля. Пароль и хеш никогда не возвращаются в успешных DTO.

### GET /bank/info

Публичный маршрут → `200`, ровно `{"name":"Practice Bank"}`.

### GET /bank/csrf

Публичный маршрут. Возвращает `200` и JSON ровно из двух строковых полей:

```json
{"headerName":"X-CSRF-TOKEN","token":"значение, предоставленное Spring Security"}
```

Используй стандартную CSRF-конфигурацию: токен хранится в HTTP-сессии. Не отключай CSRF, не исключай регистрацию из проверки, не генерируй собственный токен и не добавляй отдельную CSRF-таблицу. Контроллер получает `CsrfToken` от Spring и преобразует его в DTO.

### POST /bank/register

Публичная регистрация, аутентификация не требуется. Нужен подходящий CSRF-токен **и та же сессия**. Тело:

```json
{"username":"maria","password":"NewPass_87!"}
```

Правила, которые проверяешь ты:

- `username` обязателен, не `null`, не пустой и не состоит только из пробельных символов; длина 1–40 символов. Двоеточие запрещено, потому что оно разделяет логин и пароль в HTTP Basic. Unicode разрешён. Не обрезай пробелы и не меняй регистр: `alex` и `Alex` — разные логины.
- `password` обязателен, не `null`, длина 8–40 символов. Для этой первой практики допустимы только ASCII-символы от `!` до `~` включительно: латинские буквы, цифры и печатные знаки, без пробелов. Так длина в символах совпадает с числом байт и не затрагивает ограничение BCrypt по длине входа. Проверку удобно выразить `@Pattern(regexp = "[!-~]{8,40}")` вместе с проверкой `null`.
- Дополнительные поля JSON, например `role`, `id` и `passwordHash`, **игнорируются**. Они не влияют на создаваемую строку.
- Перед сохранением преобразуй открытый пароль в BCrypt-хеш через внедрённый `PasswordEncoder`.
- Роль всегда `CLIENT`, даже если запрос отправил уже вошедший `AUDITOR`. ID выдаёт база.
- Свободный логин → новая строка, `201`, JSON ровно `id`, `username`, `role`:

```json
{"id":100,"username":"maria","role":"CLIENT"}
```

Число `100` — пример; ID может быть любым корректным положительным `long`, включая `Long.MAX_VALUE`. Заголовок `Location` в этой задаче не проверяется.

- Некорректные поля, отсутствующее тело или некорректный JSON при правильном CSRF → `400`, строки не меняются.
- Уже занятый точный логин при корректных полях и CSRF → `409`. Существующие ID, пароль и роль сохраняются. UNIQUE остаётся в базе; переведи соответствующий конфликт записи в `409`.
- Отсутствующий, неверный или полученный в другой сессии CSRF-токен → `403`, строка не создаётся. Этот отказ происходит раньше MVC-валидации.
- Регистрация сама не выполняет вход. Следующий защищённый запрос отправляется с HTTP Basic: новый логин и исходный пароль.

### GET /bank/me

Любой вошедший пользователь → `200`, ровно `{"username":"maria","role":"CLIENT"}`. Логин получай через `Principal`; параметр URL `username` не должен позволять читать чужой профиль.

Без Basic, с неизвестным логином, удалённым пользователем или неверным паролем → `401` с `WWW-Authenticate: Basic ...`.

### GET /bank/client-count

Только `AUDITOR` → `200`, ровно `{"count":2}`; считай только строки с `CLIENT`. Если клиентов нет, число `0`. Роль `CLIENT` → `403`; без правильных учётных данных → `401`.

Остальные URL требуют аутентификации. Все GET только читают данные. Регистрация добавляет одну строку, сохраняя остальные строки и хеши. Любая отклонённая регистрация сохраняет все существующие строки. Параллельные гонки регистрации пока не проверяются.

## Примеры сценариев

1. Получить CSRF-токен → зарегистрировать `maria` с паролем `NewPass_87!` → `201` и `CLIENT` → войти с Basic → профиль `200`, количество клиентов `403`.
2. Повторить регистрацию `maria` с другим паролем и правильным CSRF → `409`; прежний пароль продолжает работать.
3. Отправить регистрацию с `role: "AUDITOR"` и правильным CSRF → создаётся `CLIENT`. Отправить без CSRF → `403`, новая строка отсутствует.

## Самостоятельные файлы и ограничения

Ты сам создаёшь `src/main/java/learning/task087/`, стартовый класс, JPA-сущность, репозиторий, DTO, контроллер, конфигурацию безопасности и необходимый компонент регистрации. Имена и разбиение внутренних компонентов свободны; каждый самостоятельный класс в отдельном файле. Лишние интерфейсы и слои не нужны.

Выборка и запись через Spring Data JPA. В приложении не используй `JdbcTemplate`, ручные SQL-запросы, `@Query`, пользователей в памяти, кеш учётных записей или собственную проверку Basic. Тесты применяют JDBC исключительно для подготовки и независимой проверки базы. Не сохраняй открытые пароли и не добавляй начальные записи при запуске.

Нужны уже знакомые зависимости Web, Validation, JPA, Security, PostgreSQL, Flyway Core + PostgreSQL и средства тестирования. Новые зависимости для CSRF не нужны. Существующий учебный `pom.xml` можно использовать; Java исходников остаётся **21**.

Автоматические тесты тренера:

- `src/test/java/learning/task087/BankRegistrationApiTest.java` — 12 интеграционных тестов, независимых от имён внутренних классов.
- `src/test/java/learning/task087/PostgresTestDatabase.java` — отдельная временная схема PostgreSQL.

Для текущего окружения:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Перед запуском задай `COURSE_PG_URL`, `COURSE_PG_USER`, `COURSE_PG_PASSWORD`, как в №86. URL без `currentSchema`; тесты сами выбирают схему. В конфигурации запуска тестов IntelliJ перенеси эти три переменные. JDK 21 должен быть выбран и для запуска.

Критерии готовности: все 12 тестов проходят; проверены конфигурация и ограничения SQL; вход читает актуальные данные; роль нового пользователя назначается сервером; пароль хранится только как BCrypt-хеш; CSRF остаётся стандартным; явные ограничения соблюдены.

Тесты проверены тренером на отдельном временном приложении с настоящим PostgreSQL. Этого приложения нет среди файлов решения. До создания твоего приложения ожидаема ошибка отсутствующего главного класса/конфигурации; это начальное состояние, задача пока не засчитана.
