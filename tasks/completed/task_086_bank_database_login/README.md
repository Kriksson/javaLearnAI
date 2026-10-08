# Задача №86 — Банк: вход пользователей из PostgreSQL

**Сложность: базовая. Самостоятельное проектирование и сборка с нуля.**

Собери приложение в пакете `learning.task086`: клиент входит в свой профиль банка, сотрудник аудита читает количество клиентов. Учётные записи, хеши паролей и роли хранятся в PostgreSQL. Java-код не содержит списка конкретных пользователей.

Закрепляем: HTTP Basic, BCrypt, роли, `401`/`403`, PostgreSQL, Flyway, JPA, производные методы, DTO и MockMvc.

Впервые: **собственный `UserDetailsService`, загружающий пользователя через JPA**, и **`Principal` для получения логина вошедшего пользователя в контроллере**. Регистрация, переводы денег, JWT и Docker будут отдельными шагами. Сейчас работаем с входом в банковское API.

## Как связать Spring Security и PostgreSQL

В №85 `InMemoryUserDetailsManager` находил пользователя в памяти. Теперь ты создаёшь свою реализацию интерфейса `UserDetailsService`. Его метод `loadUserByUsername(String username)` получает логин и возвращает `UserDetails` — сведения для проверки входа.

Путь запроса:

1. Клиент передаёт логин и пароль в заголовке Basic.
2. Spring Security вызывает твой `loadUserByUsername` с полученным логином.
3. Твой сервис вызывает репозиторий и находит строку PostgreSQL по точному логину.
4. Сервис преобразует найденную JPA-сущность в `UserDetails`: логин, **уже сохранённый** хеш и роль.
5. Spring Security проверяет присланный пароль через `PasswordEncoder.matches`, затем проверяет доступ к URL.
6. После успешной проверки запрос доходит до контроллера. Spring передаёт ему `Principal`, из которого можно получить логин через `getName()`.

Ты пишешь поиск через JPA, преобразование в `UserDetails`, исключение `UsernameNotFoundException` при отсутствии логина и правила URL. Проверку заголовка, пароля и доступа выполняет Spring Security. Не проверяй пароль самостоятельно в контроллере.

При загрузке пользователя не вызывай `encode()` для прочитанного хеша: `.password(...)` получает хеш из базы без изменения. `encode()` нужен при первоначальном сохранении нового пароля; в этой задаче строки готовят тесты или клиент базы.

Роль хранится как `CLIENT` или `AUDITOR`. `.roles(значениеИзБазы)` добавляет стандартный префикс `ROLE_`; в `hasRole` указываешь имя без префикса. Логин может быть любым допустимым значением и не обязан совпадать с ролью.

## Отдельный работающий пример: учебный портал

В отдельном Spring Boot приложении портала подключены `spring-boot-starter-web` и `spring-boot-starter-security`. У него уже есть стартовый класс. Следующие классы показывают контракт собственного сервиса и получение текущего логина. Размещать их в решении банковской задачи не нужно.

В примере источник сведений — коллекция, чтобы сосредоточиться на вызове интерфейса. В банковской задаче источник заменяется JPA-репозиторием.

Отдельный файл `PortalIdentity.java` описывает сохранённые сведения. Этот record играет роль строки источника данных; он не является `UserDetails`:

```java
package example.portal;

public record PortalIdentity(String login, String passwordHash, String category) {}
```

Файл `PortalSecurity.java`:

```java
package example.portal;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class PortalSecurity {
    @Bean
    PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService directory(PasswordEncoder encoder,
                                @Value("${portal.demo-password}") String password) {
        Map<String, PortalIdentity> entries = Map.of("dana",
                new PortalIdentity("dana", encoder.encode(password), "LEARNER"));

        return username -> {
            PortalIdentity found = entries.get(username);
            if (found == null) {
                throw new UsernameNotFoundException("Пользователь не найден");
            }
            return User.withUsername(found.login())
                    .password(found.passwordHash())
                    .roles(found.category())
                    .build();
        };
    }

    @Bean
    SecurityFilterChain access(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(rules -> rules
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
```

- `directory` возвращает bean интерфейса `UserDetailsService`. Здесь лямбда реализует его единственный метод `loadUserByUsername`. Можно вместо лямбды создать отдельный класс, реализующий интерфейс.
- `username` — логин, который Spring Security извлёк из запроса. Ты не вызываешь эту лямбду вручную из контроллера.
- `entries.get(username)` ищет сведения. В банковском приложении на этом месте будет обращение к JPA-репозиторию.
- `found` — данные источника, а `User.withUsername(...).build()` преобразует их в понятный Spring Security объект `UserDetails`. Логин, хеш и роль берутся из найденной записи. Хеш не кодируется повторно.
- `UsernameNotFoundException` сообщает механизму входа, что такой учётной записи нет. Защищённый HTTP-запрос получает `401`.
- Возвращается `UserDetails`, который Spring использует для проверки присланного пароля и получения прав.

Файл контроллера примера `PortalController.java`:

```java
package example.portal;

import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalController {
    @GetMapping("/portal/who")
    Map<String, String> who(Principal principal) {
        return Map.of("login", principal.getName());
    }
}
```

`Principal` — сведения о текущем вошедшем пользователе. Spring подставляет этот параметр после успешной проверки входа; `principal.getName()` даёт проверенный логин. Клиент не передаёт его отдельным параметром URL.

В конфигурации примера задай `portal.demo-password=${PORTAL_DEMO_PASSWORD}`. Запрос `curl -i -u dana http://localhost:8080/portal/who` спросит пароль. С правильным паролем ответ — `200` и `{"login":"dana"}`, с неправильным — `401` до вызова контроллера.

[UserDetailsService](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/user-details-service.html), [проверка пароля](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/dao-authentication-provider.html), [Principal в аргументах контроллера](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-methods/arguments.html).

## Схема и конфигурация

Это новая схема приложения, а не продолжение таблицы рекрутов. Сам создай единственную миграцию `src/main/resources/db/migration/V1__create_bank_users.sql`. Старые миграции остаются в архиве №85. Для обычного запуска используй отдельную учебную базу PostgreSQL: в базе с историей миграций №85 нельзя заменять применённую V1.

Таблица `bank_users`:

| Колонка | Требования |
|---|---|
| `id` | `BIGINT`, identity BY DEFAULT, первичный ключ; Java ID — `Long` |
| `username` | `VARCHAR(40)`, NOT NULL, UNIQUE, CHECK длины больше 0 |
| `password_hash` | `VARCHAR(100)`, NOT NULL; сохранённый BCrypt-хеш |
| `role` | `VARCHAR(16)`, NOT NULL, CHECK: только `CLIENT` или `AUDITOR` |

У пользователя ровно одна роль. Логин длиной 1–40 символов сравнивается точно, с учётом регистра; не меняй его и не обрезай пробелы. Входные логины для этой практики не содержат двоеточия, разделяющего логин и пароль в Basic. Пароли передаются через Basic; в таблице хранится только хеш.

Не создавай начальных пользователей в миграции или при запуске приложения. Тесты сами добавляют строки с хешами. Для ручной проверки пароль можно хешировать через `new BCryptPasswordEncoder().encode(пароль)` в отдельном эксперименте и добавить строку через клиент базы; результат хеширования не является открытым паролем для `curl`.

В `src/main/resources/application.properties` нужны:

| Свойство | Значение |
|---|---|
| `spring.datasource.url` | `${COURSE_PG_URL}` без значения по умолчанию |
| `spring.datasource.username` | `${COURSE_PG_USER}` |
| `spring.datasource.password` | `${COURSE_PG_PASSWORD}` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |
| `spring.jpa.open-in-view` | `false` |

Пароли API теперь берутся из строк базы. Переменные `COURSE_READER_PASSWORD` и `COURSE_ADMIN_PASSWORD` из №85 не нужны. Не задавай фиксированную схему в SQL или `@Table`; Flyway и Hibernate используют стандартное подключение.

Сам создай JPA-сущность, репозиторий, собственный bean `UserDetailsService`, BCrypt `PasswordEncoder`, `SecurityFilterChain`, контроллер и DTO. Имена внутренних классов и методов свободны. Поиск и количество — через Spring Data JPA; `JdbcTemplate`, ручной SQL, `@Query`, `InMemoryUserDetailsManager`, список пользователей в Java и кеш учётных записей не используются. Тесты применяют JDBC только для независимой подготовки данных.

## HTTP-контракт

Все операции только читают данные. Формат ошибок свободный. Используй HTTP Basic, без формы входа; остальные настройки безопасности оставляй стандартными.

### GET /bank/info

Публичный маршрут. Без учётных данных, а также с правильными данными любого пользователя из базы → `200`, JSON ровно:

```json
{"name":"Practice Bank"}
```

### GET /bank/me

Любой успешно вошедший пользователь → `200`, JSON с ровно `username` и `role`. Пример:

```json
{"username":"alex","role":"CLIENT"}
```

Получай текущий логин через `java.security.Principal` в аргументе метода контроллера. Ответ относится к вошедшему пользователю, а не к первой строке таблицы или имени из параметров URL. Например, вход `alex` и запрос `/bank/me?username=nina` всё равно возвращает профиль `alex`. Значение `role` в JSON — без `ROLE_`. ID, пароль, хеш и дополнительные поля не возвращай; используй DTO.

Без учётных данных, неизвестный логин, неправильный или пустой пароль → `401` с `WWW-Authenticate`, начинающимся с `Basic`.

### GET /bank/client-count

- `AUDITOR` с правильным паролем → `200`, JSON ровно `{"count":N}`; количество в DTO имеет тип `long`.
- Считай **только строки с ролью CLIENT**. Сотрудники аудита не входят в число клиентов.
- `CLIENT` с правильным паролем → `403`, даже если клиентов нет других или таблица содержит только этого пользователя.
- Нет учётных данных или они неверны → `401` с заголовком Basic.
- Аудитор существует, но клиентов нет → `{"count":0}`.

Для остальных URL требуется успешный вход: анонимный GET `/unmapped` → `401`.

Каждый новый запрос с Basic должен использовать текущие данные базы: новый пользователь сразу может войти; после удаления вход даёт `401`; после изменения пароля старый пароль не подходит, новый подходит; после изменения роли меняется доступ и профиль. Тесты отправляют отдельные запросы без переноса HTTP-сессии. Ни успешный запрос, ни отказ не меняют таблицу.

## Примеры

1. В базе `alex` с ролью CLIENT и `nina` с ролью AUDITOR. `alex` читает `/bank/me` → свой профиль; `/bank/client-count` → `403`. `nina` читает количество → `{"count":1}`.
2. Через клиент базы добавлен `sam` с ролью CLIENT. Без перезапуска приложения `sam` входит в профиль, а `nina` получает `{"count":2}`.
3. Роль `alex` изменена на AUDITOR. Следующий отдельный запрос с его правильным паролем возвращает новый профиль; доступ к количеству разрешён. Оставшийся клиент `sam` даёт `{"count":1}`.

## Файлы и запуск тестов

Ты самостоятельно создаёшь код в `src/main/java/learning/task086/`, конфигурацию в `src/main/resources/application.properties`, миграцию в `src/main/resources/db/migration/` и проверяешь зависимости `pom.xml`. Исходный код — Java 21. Заготовок приложения нет; каждый самостоятельный класс — отдельный файл.

Тесты тренера:

- `src/test/java/learning/task086/BankDatabaseLoginApiTest.java` — 9 интеграционных тестов и предварительная проверка конфигурации/миграции/JPA;
- `src/test/java/learning/task086/PostgresTestDatabase.java` — изолированная случайная схема PostgreSQL, которая удаляется после проверки.

Нужны три переменные `COURSE_PG_URL`, `COURSE_PG_USER`, `COURSE_PG_PASSWORD`. Для тестов можно использовать то же подключение, что в №85: они направляют приложение в новую собственную схему. Для обычного запуска настрой отдельную учебную базу с новой историей V1.

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Для Windows: `.\mvnw.cmd test`. Для проверки вручную: `curl -i -u alex http://localhost:8080/bank/me` или Postman → Authorization → Basic Auth. Пароль для входа — исходный пароль, из которого получен хеш.

Готовность: все 9 тестов прошли, собственный сервис читает пользователей через JPA, BCrypt-хеш загружается без повторного кодирования, контроллер получает текущий логин через Principal, роль ограничивает количество клиентов, SQL-ограничения действуют, данные не меняются. Ограничения реализации дополнительно проверяются при чтении кода. До создания приложения ожидается ошибка отсутствующей Spring Boot конфигурации или ресурсов; задача пока не засчитывается.
