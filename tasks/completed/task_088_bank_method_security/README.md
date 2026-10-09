# №88 — Банк: защита метода сервиса

Сложность: **базовая**. Самостоятельно создаёшь приложение в `learning.task088`.

Закрепляем: HTTP Basic, роли из PostgreSQL, собственный `UserDetailsService`, BCrypt, JPA, производные методы, DTO, `Principal`, Flyway и ответы `401`/`403`.

Новый механизм: **проверка доступа перед вызовом метода сервиса** через `@EnableMethodSecurity` и `@PreAuthorize`. Вместе с ним познакомимся с назначением Spring proxy — объекта, который выполняет проверку перед вызовом твоего метода.

## Сценарий

У банка есть отчёт: количество клиентов. Его может получить только сотрудник с ролью `AUDITOR`. В прошлой практике проверка роли была привязана к URL в `SecurityFilterChain`. Теперь отчёт может понадобиться другому компоненту приложения, который вызывает сервис напрямую.

Нужно защитить сам метод, формирующий отчёт. Контроллер и другие компоненты должны получать одинаковое решение о доступе. В этой задаче все HTTP-операции читают данные; регистрацию, изменение и удаление пользователей не реализуем.

## Как работает защита метода

Ты уже знаешь такую проверку:

```java
.requestMatchers("/some/report").hasRole("MANAGER")
```

Она проверяет доступ к HTTP-маршруту. Новый вариант размещается на методе Spring-компонента:

```java
@PreAuthorize("hasRole('MODERATOR')")
public String reviewSummary() {
    return "Ready for review";
}
```

`PreAuthorize` означает «проверить доступ **перед** выполнением метода». В строке `hasRole('MODERATOR')` Spring проверяет роль текущего пользователя. Это выражение обрабатывает Spring Security, а не Java-компилятор. Как и в `SecurityFilterChain`, `hasRole` использует authority `ROLE_MODERATOR`; в выражении префикс `ROLE_` не пишем.

Чтобы аннотация работала, включи механизм на классе конфигурации:

```java
@Configuration
@EnableMethodSecurity
public class AccessSettings {
}
```

Импорты:

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.prepost.PreAuthorize;
```

`spring-boot-starter-security` сам по себе не включает этот механизм. Назначение аннотаций и поведение при отказе описаны в [документации Method Security](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/method-security.html).

### Зачем здесь Spring proxy

Когда Spring создаёт защищённый сервис, другие компоненты получают ссылку на объект-посредник — **proxy**. При вызове метода этот объект сначала запускает проверку безопасности, а затем, если доступ разрешён, вызывает твой метод.

Путь HTTP-запроса:

```text
GET + HTTP Basic
  → Spring Security читает логин и пароль
  → твой UserDetailsService загружает хеш и роль из PostgreSQL
  → Spring Security проверяет пароль и сохраняет сведения о пользователе
  → правило authenticated() разрешает пройти вошедшему пользователю
  → контроллер вызывает внедрённый сервис
  → proxy проверяет @PreAuthorize
  → разрешено: сервис читает количество из JPA, контроллер возвращает DTO
  → запрещено: AccessDeniedException, Spring Security возвращает 403
```

Без правильных учётных данных запрос останавливается раньше, с `401`. Пользователь `CLIENT` успешно проходит проверку пароля, но получает `403` при попытке вызвать метод отчёта.

Чтобы проверка сработала, используй сервис как Spring bean, внедрённый в контроллер. Создание через `new`, приватный метод или вызов защищённого метода через `this` внутри того же объекта не дают обычного прохода через proxy. В этой первой практике сервис — публичный нефинальный класс, защищённый метод — публичный нефинальный. Не создавай лишний интерфейс только ради этого.

При прямом вызове сервиса без HTTP нет HTTP-ответа: вызывающий код получает исключение безопасности. Тесты специально проверяют и такой путь. Контекст пользователя для прямых вызовов устанавливают **тесты**; приложение не должно вручную назначать себе пользователя или роль.

### Отдельный работающий пример: панель модерации

Это пример для отдельного минимального Spring Boot проекта с зависимостями Web и Security. Каждый класс — отдельный файл в одном пакете. Его пользователи в памяти предназначены только для демонстрации; в банковской задаче учётные записи берутся из PostgreSQL.

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

`DemoAccess.java`:

```java
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
public class DemoAccess {
    @Bean
    PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService demoUsers(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("alex")
                        .password(encoder.encode("Demo_88!"))
                        .roles("MODERATOR").build(),
                User.withUsername("kim")
                        .password(encoder.encode("Visitor_88!"))
                        .roles("VISITOR").build()
        );
    }

    @Bean
    SecurityFilterChain requests(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(rules -> rules.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
```

`ReviewService.java`:

```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {
    @PreAuthorize("hasRole('MODERATOR')")
    public String reviewSummary() {
        return "Ready for review";
    }
}
```

`ReviewEndpoint.java`:

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class ReviewEndpoint {
    private final ReviewService reviewService;

    public ReviewEndpoint(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/demo/review")
    public Map<String, String> review() {
        return Map.of("message", reviewService.reviewSummary());
    }
}
```

Разбор ключевых строк:

- `@EnableMethodSecurity` включает обработку аннотации на методе.
- `@Service` позволяет Spring создать компонент и защитить его вызовы через proxy.
- `@PreAuthorize` проверяет роль до выполнения `return`.
- Конструктор контроллера получает созданный Spring сервис. Контроллер не создаёт его через `new`.
- `authenticated()` разрешает дойти до контроллера обоим вошедшим пользователям. Право получить результат определяет аннотация сервиса.
- Возвращаемая `Map` превращается в JSON с полем `message`.

После запуска примера:

```shell
curl -i http://localhost:8080/demo/review
curl -i -u 'kim:Visitor_88!' http://localhost:8080/demo/review
curl -i -u 'alex:Demo_88!' http://localhost:8080/demo/review
```

Результаты: без Basic → `401`; `kim` с ролью `VISITOR` → `403`; `alex` с ролью `MODERATOR` → `200`, `{"message":"Ready for review"}`.

## Условие банковской задачи

Самостоятельно создай приложение, читающее пользователей из PostgreSQL. Схема знакома по №86–87; новый акцент — проверка роли на методе сервиса.

### База и конфигурация

Единственная миграция: `src/main/resources/db/migration/V1__create_bank_users.sql`.

| Колонка | Требования |
|---|---|
| `id` | `BIGINT GENERATED BY DEFAULT AS IDENTITY`, PRIMARY KEY; в Java `Long` |
| `username` | `VARCHAR(40)`, NOT NULL, UNIQUE, CHECK длины больше 0 |
| `password_hash` | `VARCHAR(100)`, NOT NULL; BCrypt-хеш |
| `role` | `VARCHAR(16)`, NOT NULL, CHECK только `CLIENT` или `AUDITOR` |

Отобрази таблицу через JPA; длины строковых колонок в `@Column` должны соответствовать SQL. Начальных строк при запуске нет. Тесты сами добавляют пользователей с хешами в отдельную случайную схему и удаляют только её. Фиксированное имя схемы не задаётся в SQL и `@Table`. Для ручного запуска используй отдельную учебную базу; применённые миграции прошлых задач не заменяй.

В самостоятельно созданном `src/main/resources/application.properties`:

| Свойство | Значение |
|---|---|
| `spring.datasource.url` | `${COURSE_PG_URL}` |
| `spring.datasource.username` | `${COURSE_PG_USER}` |
| `spring.datasource.password` | `${COURSE_PG_PASSWORD}` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |
| `spring.jpa.open-in-view` | `false` |

Используй собственный bean `UserDetailsService`, BCrypt `PasswordEncoder`, JPA-репозиторий и HTTP Basic. Каждый новый запрос учитывает изменения пароля и роли, добавление или удаление пользователя в базе. Логины сравниваются точно, включая регистр и пробелы, без двоеточия; длина 1–40 символов, Unicode разрешён. Пароли для входа проверяет Spring Security, не твой контроллер.

### Защищённый сервис

- Включи Method Security через `@EnableMethodSecurity`.
- Создай Spring-компонент с `@Service`, который читает **количество CLIENT** через производный метод репозитория.
- Для независимой проверки создай ровно один публичный метод этого сервиса, помеченный `@PreAuthorize`, без параметров, возвращающий `long` или `Long`. Имя сервиса и метода свободное. Метод должен быть доступен только `AUDITOR` и возвращать актуальное количество клиентов.
- Аннотацию размещай на методе класса сервиса. Класс и метод нефинальные. Контроллер вызывает этот bean через внедрение зависимостей.
- В `SecurityFilterChain` для отчёта требуется только `.authenticated()`. Роль проверяется на сервисе; не дублируй её через `.hasRole` для этого URL, в контроллере или ручной проверкой authorities.
- Не перехватывай исключение безопасности, превращая отказ в успешный ответ или число `0`. HTTP-отказ обработает Spring Security.

Числовой метод без параметров нужен, чтобы тесты могли вызвать именно отчёт без привязки к выбранным тобой именам. Тесты находят его по `@Service`, `@PreAuthorize` и форме метода.

### HTTP-контракт

Формат ошибок свободный. Все успешные ответы содержат ровно перечисленные поля.

| Запрос | Доступ | Ответ |
|---|---|---|
| `GET /bank/info` | Публичный | `200`, `{"name":"Practice Bank"}` |
| `GET /bank/me` | Любой вошедший пользователь | `200`, `{"username":"alex","role":"CLIENT"}` |
| `GET /bank/client-count` | Вход на уровне HTTP, `AUDITOR` на методе сервиса | `200`, `{"count":2}` |

Для `/bank/me` получай логин через `Principal`; параметр URL `username` не меняет выбранного пользователя. ID, пароль и хеш не входят в ответы.

Без Basic, при неверном пароле, неизвестном или удалённом пользователе защищённые маршруты возвращают `401` с `WWW-Authenticate: Basic ...`. Вошедший `CLIENT` при запросе отчёта получает `403`. Ноль клиентов — успешный ответ `{"count":0}` для `AUDITOR`, а не отсутствие отчёта. Другие URL требуют аутентификации.

Все операции только читают строки и хеши. CSRF оставляй стандартным; все маршруты задачи используют GET, поэтому токен для них не требуется. Endpoint выдачи CSRF-токена в этой задаче не нужен.

### Примеры

1. В базе `alex: CLIENT`, `sam: CLIENT`, `nina: AUDITOR`. `nina` запрашивает отчёт → `200`, `{"count":2}`. `alex` → `403`, но свой профиль читает с `200`.
2. Тест изменяет роль `alex` на `AUDITOR`. Следующий запрос `alex` получает `200`, `{"count":1}`: клиентом остался `sam`. Приложение не перезапускается.
3. Другой компонент вызывает защищённый Spring-сервис напрямую. При роли `CLIENT` получает `AccessDeniedException`; при `AUDITOR` — актуальное число. Без аутентификации тоже отказ безопасности; в таком вызове HTTP-статуса нет.

## Самостоятельные файлы и проверка

Сам создаёшь `src/main/java/learning/task088/`, стартовый класс, сущность, репозиторий, DTO, контроллер, сервис и конфигурацию безопасности. Каждый самостоятельный класс — отдельный файл. Имена и разбиение свободные, кроме путей ресурсов и описанной формы защищённого метода.

В приложении не используй `JdbcTemplate`, ручной SQL, `@Query`, список пользователей в памяти, кеш учётных записей, самостоятельную установку `SecurityContextHolder` или свой механизм проверки Basic. Тесты используют JDBC и тестовый контекст безопасности для независимой проверки; это не пример кода приложения.

Достаточно уже подключённых зависимостей Web, JPA, Security, PostgreSQL, Flyway и средств тестирования. Для Method Security отдельной зависимости не требуется. Используй существующий учебный `pom.xml`; версия исходников остаётся Java **21**.

Тесты тренера:

- `src/test/java/learning/task088/BankMethodSecurityApiTest.java` — 8 интеграционных тестов, включая два прямых вызова сервиса.
- `src/test/java/learning/task088/PostgresTestDatabase.java` — отдельная временная схема PostgreSQL.

Перед запуском задай `COURSE_PG_URL`, `COURSE_PG_USER`, `COURSE_PG_PASSWORD`, как в №87. URL без `currentSchema`; схему выбирают тесты. В IntelliJ настрой эти переменные для нового класса тестов и выбери JDK 21.

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Критерии готовности: все 8 тестов проходят; роли и пароли читаются из актуальной базы; сервис действительно защищён и при прямом вызове; контроллер использует защищённый bean; конфигурация и отображение SQL соответствуют условию; ограничения соблюдены.

Тесты проверены тренером на отдельном временном приложении с настоящим PostgreSQL. Оно не размещено в файлах решения. До создания твоего приложения ошибка отсутствующей конфигурации/главного класса ожидаема; задача ещё не засчитана.
