# №89 — Банк: доступ к своему профилю

Сложность: **базовая**. Самостоятельно создаёшь приложение в `learning.task089`.

Закрепляем: Spring Boot REST, HTTP Basic, BCrypt, пользователи из PostgreSQL, JPA, DTO, Flyway, `@EnableMethodSecurity`, `@PreAuthorize` и вызов внедрённого сервиса.

Новое понятие: **параметр метода в выражении доступа**. Проверка учитывает, чей профиль запросили, и сравнивает этот логин с логином вошедшего пользователя.

## Сценарий

Клиенту банка доступен его профиль. Аудитор может прочитать профиль любого пользователя. Сам факт успешного входа ещё не даёт клиенту права смотреть чужие данные.

Защити чтение профиля на методе сервиса. Контроллер передаёт логин из пути запроса; решение о доступе принимает Spring Security перед выполнением метода. Все операции задачи читают данные.

## Как параметр попадает в проверку

В №88 выражение проверяло только роль. Теперь ему нужны два разных значения:

- параметр метода — **кого запросили**;
- `authentication.name` — **кто вошёл**. Его установил Spring Security после проверки HTTP Basic через твой `UserDetailsService`.

Параметры в выражении обозначаются знаком `#`. Например, `#owner` обращается к параметру, доступному под именем `owner`. Это выражение Spring Expression Language, сокращённо SpEL. Для сравнения строк здесь используется `==`: оно сравнивает значения строк. Это правило SpEL, а не сравнение Java-ссылок через `==`.

Небольшой пример: в приложении заметок пользователь читает заметку для собственного логина. Банковских профилей, JPA и особого доступа аудитора в этом примере нет.

```java
@PreAuthorize("#owner == authentication.name")
public Map<String, String> readNote(@P("owner") String requestedLogin) {
    return Map.of("text", "Personal note for " + requestedLogin);
}
```

- `@P("owner")` даёт параметру имя для проверки; Java-переменная может называться `requestedLogin`.
- `#owner` содержит аргумент, с которым вызвали метод.
- `authentication.name` содержит логин текущего пользователя.
- Если строки равны, Spring вызывает метод. Если различаются — отклоняет вызов до входа в его тело.

Импорт `@P`: `org.springframework.security.access.method.P`. Вместо этой аннотации можно использовать настоящее имя параметра при компиляции с `-parameters`; учебный Maven-проект уже включает `<parameters>true</parameters>`. Выбери один способ. Имя после `#` должно соответствовать выбранному способу, а не имени поля DTO или имени переменной контроллера. [Параметры методов в Spring Security](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/method-security.html#using-method-parameters).

Логические операции тоже доступны в выражении: `or` означает «достаточно одного условия», `and` — «оба условия». В банковской задаче сформулируй выражение самостоятельно: совпадение логинов **или** роль аудитора.

### Отдельный работающий пример: личная заметка

Это отдельное минимальное приложение с зависимостями Web и Security. Четыре файла расположены в одном пакете `demo.notes`. Пример остаётся в этом материале; в решение задачи его переносить не нужно.

`NotesApplication.java`:

```java
package demo.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotesApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotesApplication.class, args);
    }
}
```

`NotesSecurity.java`:

```java
package demo.notes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class NotesSecurity {
    @Bean
    PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService demoUsers(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("alex").password(encoder.encode("Alex_89!"))
                        .roles("VISITOR").build(),
                User.withUsername("kim").password(encoder.encode("Kim_89!"))
                        .roles("VISITOR").build());
    }

    @Bean
    SecurityFilterChain access(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(rules -> rules.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults()).build();
    }
}
```

`NoteService.java`:

```java
package demo.notes;

import java.util.Map;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class NoteService {
    @PreAuthorize("#owner == authentication.name")
    public Map<String, String> readNote(@P("owner") String requestedLogin) {
        return Map.of("text", "Personal note for " + requestedLogin);
    }
}
```

`NoteEndpoint.java`:

```java
package demo.notes;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoteEndpoint {
    private final NoteService notes;

    public NoteEndpoint(NoteService notes) {
        this.notes = notes;
    }

    @GetMapping("/notes/{owner}")
    public Map<String, String> read(@PathVariable("owner") String loginFromPath) {
        return notes.readNote(loginFromPath);
    }
}
```

После запуска этого отдельного приложения:

```shell
curl -u kim:Kim_89! http://localhost:8080/notes/kim
# 200: {"text":"Personal note for kim"}
curl -i -u kim:Kim_89! http://localhost:8080/notes/alex
# 403
curl -i http://localhost:8080/notes/kim
# 401
```

Путь первого запроса: Basic сообщает, что вошёл `kim` → Spring проверяет пароль → MVC извлекает `kim` из URL в `loginFromPath` → контроллер передаёт строку в `readNote` → proxy получает `#owner = "kim"` и `authentication.name = "kim"` → проверка разрешает вызов → тело сервиса возвращает заметку.

Во втором запросе вошёл всё ещё `kim`, но аргумент равен `alex`. Проверка отклоняет вызов; тело сервиса не выполняется. В третьем запросе вход не выполнен, поэтому запрос останавливается на фильтрах безопасности.

## Внешний контракт банковского приложения

### База и конфигурация

Используй настоящий PostgreSQL и одну миграцию `src/main/resources/db/migration/V1__create_bank_users.sql`. Она создаёт таблицу `bank_users`:

| Колонка | Требование |
|---|---|
| `id` | BIGINT, PRIMARY KEY, GENERATED BY DEFAULT AS IDENTITY |
| `username` | VARCHAR(40), NOT NULL, UNIQUE, CHECK: длина больше нуля |
| `password_hash` | VARCHAR(100), NOT NULL |
| `role` | VARCHAR(16), NOT NULL, CHECK: только CLIENT или AUDITOR |

JPA-сущность отображает эти имена и длины; идентификатор имеет тип `Long`. Не фиксируй имя PostgreSQL-схемы в сущности или миграции. Логин сравнивается точно: регистр, пробелы и символы сохраняются. Данные могут содержать Unicode, логин длиной 1 или 40 символов и ID больше диапазона `int`. Для пути логин передаётся как один URL-сегмент, без `/`.

При запуске приложение не добавляет пользователей. Тесты сами заполняют таблицу BCrypt-хешами и меняют данные между запросами.

В `src/main/resources/application.properties` объяви:

| Свойство | Значение |
|---|---|
| `spring.datasource.url` | `${COURSE_PG_URL}` |
| `spring.datasource.username` | `${COURSE_PG_USER}` |
| `spring.datasource.password` | `${COURSE_PG_PASSWORD}` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |
| `spring.jpa.open-in-view` | `false` |

### Аутентификация и метод сервиса

Собственный `UserDetailsService` загружает пользователя, его сохранённый BCrypt-хеш и роль из PostgreSQL через JPA-репозиторий. Пароли и роли должны читаться заново для следующего HTTP-запроса. Для неизвестного логина явно сообщай `UsernameNotFoundException`.

Включи `@EnableMethodSecurity`. В публичном нефинальном классе `@Service` создай **один публичный нефинальный метод с `@PreAuthorize`**, который:

- принимает единственный параметр `String` — запрошенный логин;
- до выполнения тела разрешает доступ владельцу логина или пользователю с ролью `AUDITOR`;
- при разрешённом доступе читает актуальную строку из JPA-репозитория и возвращает DTO только с `username` и `role`;
- явно обрабатывает отсутствие строки исключением, которое контроллер/обработчик преобразует в `404`; допустим и `ResponseStatusException` с этим статусом.

Имя класса, метода, параметра и DTO выбираешь сам. Эта форма метода нужна тестам для прямой проверки Spring bean; имя тесты не фиксируют. Контроллер вызывает внедрённый сервис. Проверку владельца и особых прав аудитора размести в выражении `@PreAuthorize`. Не дублируй её в контроллере, Java-ветвлениях сервиса или фильтрах URL.

`SecurityFilterChain` делает `/bank/info` публичным, остальные маршруты требуют аутентификации. Включён HTTP Basic; CSRF остаётся стандартным. Все маршруты ниже используют GET, токен им не нужен.

### HTTP

`GET /bank/info` — публичный ответ `200`:

```json
{"name":"Practice Bank"}
```

`GET /bank/users/{username}` — чтение профиля:

| Кто обращается | Запрошенный профиль | Результат |
|---|---|---|
| CLIENT | собственный точный логин | 200, DTO с username и role |
| CLIENT | чужой логин, существующий или отсутствующий | 403 |
| AUDITOR | любой существующий логин, включая собственный | 200, DTO с username и role |
| AUDITOR | отсутствующий логин | 404 |
| Без входа, неверный пароль, неизвестный/удалённый пользователь | любой логин | 401 с WWW-Authenticate: Basic |

Доступ проверяется **до поиска профиля**: клиент не должен узнавать по разнице между `403` и `404`, существует ли чужой логин. Роль и логин из query-параметров не определяют права: запрошенный логин берётся из пути, вошедший — из проверенной аутентификации.

Успешный профиль содержит ровно два поля, например:

```json
{"username":"alex","role":"CLIENT"}
```

ID, пароль и его хеш в ответ не включаются. Требований к телу ошибочных ответов нет. Запросы и отказы в доступе не меняют строки базы. Регистрация и отчёт из №88 не нужны в этой задаче.

### Три примера

1. В базе `alex: CLIENT`, `sam: CLIENT`, `nina: AUDITOR`. `alex` читает `/bank/users/alex` → `200`; `/bank/users/sam` → `403`; `/bank/users/missing` → `403`.
2. `nina` читает `/bank/users/alex` → `200` с профилем **alex**; `/bank/users/missing` → `404`. В ответе роль запрошенного пользователя, а не роль аудитора.
3. Другой компонент вызывает тот же Spring-сервис. При входе как `alex` аргумент `alex` разрешён, `sam` отклонён исключением безопасности. Аудитор читает оба профиля. Без аутентификации прямой вызов отклоняется; HTTP-статуса у такого вызова нет.

## Самостоятельные файлы и проверка

Сам создаёшь `src/main/java/learning/task089/`, стартовый класс, сущность, репозиторий, DTO, сервис, контроллер и настройки. Каждый самостоятельный класс — отдельный файл. Сам подключаешь необходимые зависимости и создаёшь ресурсы. Используй текущий учебный `pom.xml`, исходники остаются Java **21**; новых библиотек для этого приёма не нужно.

Ограничения: в приложении не используй `JdbcTemplate`, ручной SQL, `@Query`, пользователей в памяти, кеш аутентификации, ручную установку `SecurityContextHolder` или собственный разбор Basic. SQL пишешь только в миграции. Защищённый сервис вызывается через внедрённый bean, а не через `new` или `this`.

Тесты тренера:

- `src/test/java/learning/task089/BankProfileAccessApiTest.java` — 12 интеграционных тестов, включая прямые вызовы сервиса;
- `src/test/java/learning/task089/PostgresTestDatabase.java` — отдельная временная схема PostgreSQL с очисткой после проверки.

Перед запуском задай `COURSE_PG_URL`, `COURSE_PG_USER`, `COURSE_PG_PASSWORD`, как в №88. URL без `currentSchema`: схему выбирают тесты. В IntelliJ IDEA настрой переменные для нового класса тестов и выбери полный JDK 21.

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Критерии готовности: все 12 тестов проходят; владелец и аудитор получают разрешённые данные, остальные вызовы отклоняются; поиск отсутствующей строки обработан явно; роли, пароли и DTO отражают актуальную базу; конфигурация и миграция соответствуют контракту; ограничения соблюдены.

Тесты проверены на отдельном временном приложении с настоящим PostgreSQL; учебный пример проверен отдельно. Проверочное приложение не размещено в файлах решения. До создания твоего приложения ошибка отсутствующей конфигурации/главного класса ожидаема; №89 ещё не засчитана.
