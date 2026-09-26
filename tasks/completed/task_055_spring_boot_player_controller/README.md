# Задача №55 — Первый контроллер Spring Boot

**Сложность:** средняя; первый шаг со Spring Boot.

**Закрепляем:** HTTP `GET`, статусы `200/400/404`, `Map`, `record` и JSON.

**Впервые:** контроллер Spring MVC через `@RestController` и `@GetMapping`; Spring сам выбирает метод по маршруту и сериализует Java-объект в JSON. В задаче используется Spring Boot 3.5.16, совместимый с Java 21 и уже знакомыми Jackson/JUnit 5.

## Материал для изучения

В задачах №53–54 ты вручную реализовывал `HttpHandler.handle`, читал путь, выставлял заголовки и записывал байты. Spring MVC делает HTTP-часть за тебя: вызывает метод контроллера по аннотации и превращает возвращённый объект в JSON.

Небольшой **отдельный** пример — `GET /health`:

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {
    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("ok");
    }
}

record HealthResponse(String status) {}
```

`@RestController` сообщает Spring, что результат метода — тело HTTP-ответа. `@GetMapping("/health")` привязывает метод к `GET /health`. Возвращённый `record` становится JSON `{"status":"ok"}`: вручную создавать `ObjectMapper`, считать байты и закрывать `HttpExchange` не нужно.

Чтобы взять число из адреса, используется `@PathVariable`. Пример из **другого** маршрута:

```java
@GetMapping("/double/{number}")
public int doubleNumber(@PathVariable("number") int number) {
    return number * 2;
}
```

Запрос `GET /double/4` передаст в метод `number == 4`. Имя в `@PathVariable("number")` должно совпадать с `{number}` в маршруте; указывай его явно. Для выбора статуса с телом или без него возвращай `ResponseEntity<T>`: например, `ResponseEntity.ok(value)` даёт `200` и тело, `ResponseEntity.notFound().build()` — `404` без тела. При невозможности преобразовать `{number}` в `int` Spring сам вернёт `400`.

Файл `PlayerApplication.java` — готовая точка входа приложения; **не изменяй его**. Тест запускает Spring-контекст и отправляет запросы через MockMvc, не поднимая реальный сетевой порт. Это проверяет настоящий маршрут и JSON без ручного запуска приложения.

## Что сделать

В пакете `learning.task055`:

- `PlayerApplication.java` — готовая инфраструктура с `@SpringBootApplication` и `main`; оставь без изменений.
- `PlayerView.java` — `public record PlayerView(int playerId, String name) {}`; заготовка готова.
- `PlayerController.java` — реализуй контроллер с методом:

```java
public ResponseEntity<PlayerView> find(@PathVariable("id") int id)
```

- Пометь класс `@RestController`, а метод сопоставь с `GET /players/{id}`. Имя переменной пути — `id`.
- Данные игроков уже заданы в контроллере: `1 → "Alice"` и `7 → "Кот \"Рыцарь\""`. Не меняй их.
- Для положительного ID из карты верни `200` и `PlayerView(id, name)`. Spring должен сериализовать его в JSON-объект ровно с полями `playerId` и `name`.
- Для положительного ID вне карты верни `404` без тела; для `id <= 0` — `400` без тела.
- Для нечислового или выходящего за диапазон `int` ID Spring возвращает `400` сам. `POST` на этот маршрут не должен выполнять метод `find`.
- Не используй в контроллере `HttpExchange`, `HttpServer` и ручную сериализацию JSON.

## Примеры

- `GET /players/1` → `200`, `{"playerId":1,"name":"Alice"}`.
- `GET /players/7` → `200`, `{"playerId":7,"name":"Кот \"Рыцарь\""}`.
- `GET /players/99` → `404` без тела; `GET /players/0` → `400` без тела.

## Ограничения и готовность

- Используй только Spring MVC и зависимости, уже добавленные в `pom.xml` для этой задачи. Версию Java 21 не меняй.
- Не формируй JSON руками: верни `PlayerView` и дай Spring выполнить сериализацию.
- Тесты проверяют маршруты, типизированный JSON, UTF-8, положительные и неверные ID, пустые ответы и неподдерживаемый метод.

## Файлы

- Решение: `src/main/java/learning/task055/PlayerController.java`.
- Готовая инфраструктура: `src/main/java/learning/task055/PlayerApplication.java` и `src/main/java/learning/task055/PlayerView.java`.
- Тесты: `src/test/java/learning/task055/PlayerControllerTest.java`.
- Зависимости: `pom.xml`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```

Материалы Spring: [REST-контроллер](https://docs.spring.io/spring-boot/3.5/reference/web/servlet.html), [тесты контроллера](https://docs.spring.io/spring-boot/3.5/reference/testing/spring-boot-applications.html).
