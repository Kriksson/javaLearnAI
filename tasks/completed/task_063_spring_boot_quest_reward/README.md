# Задача №63 — Награда за квест через Spring Boot

**Сложность:** базовая.

**Практика:** Spring Boot, REST-маршрут `GET`, `@PathVariable`, сервис, внедрение зависимости и ответы `200`/`404`.

**Новые понятия:** нет. Задача закрепляет основы Spring на небольшом игровом API.

## Коротко о Spring

- `@RestController` связывает методы класса с HTTP-запросами и автоматически превращает возвращённые объекты в JSON.
- `@GetMapping` задаёт маршрут для `GET`, а `@PathVariable("questId")` берёт число из URL.
- `@Service` регистрирует класс с логикой в Spring. Если передать сервис контроллеру через конструктор, Spring сам создаст и передаст нужный объект.
- `ResponseEntity` позволяет выбрать HTTP-статус и тело ответа.

Пример на отдельном маршруте — приветствие игрока по имени:

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class WelcomeController {
    @GetMapping("/welcome/{playerName}")
    String welcome(@PathVariable("playerName") String playerName) {
        return "Привет, " + playerName;
    }
}
```

- `@RestController` делает класс обработчиком HTTP и возвращает строку в теле ответа.
- `@GetMapping` связывает метод с `GET /welcome/{playerName}`.
- `@PathVariable("playerName")` помещает часть URL в параметр метода.

## Что сделать

В пакете `learning.task063` готовы `QuestApplication` и запись `QuestReward`. Реализуй `QuestRewardService` и `QuestRewardController`.

### Сервис

- Пометь класс `@Service`.
- Доступны три квеста: ID `1` даёт `40` монет, ID `2` — `75`, ID `3` — `120`.
- Реализуй метод `Optional<QuestReward> findReward(int questId)`: верни награду для известного ID или `Optional.empty()` для неизвестного.
- Не меняй таблицу наград при запросе.

### Контроллер

- Пометь класс `@RestController`.
- Получи `QuestRewardService` через конструктор.
- Создай маршрут `GET /quests/{questId}/reward`.
- Для найденного квеста верни `200 OK` и JSON с `questId` и `coins`.
- Для неизвестного ID верни `404 Not Found` без тела.
- Используй явное имя в `@PathVariable("questId")`.

Некорректный текст вместо числа, например `/quests/abc/reward`, Spring сам отклонит со статусом `400 Bad Request`.

## Примеры

- `GET /quests/2/reward` → `200`, `{"questId":2,"coins":75}`.
- `GET /quests/3/reward` → `200`, `{"questId":3,"coins":120}`.
- `GET /quests/99/reward` → `404`, пустое тело.
- `GET /quests/abc/reward` → `400`.

## Ограничения и готовность

- Используй готовые зависимости Spring Boot; новые зависимости не нужны.
- Логику поиска держи в сервисе, HTTP-ответы — в контроллере.
- Тесты проверяют несколько наград, неизвестный ID, неверный тип ID, статус и JSON.
- Реализуй только два класса с `TODO`; остальные файлы не меняй.

## Файлы

- Реализовать: `src/main/java/learning/task063/QuestRewardService.java` и `QuestRewardController.java`.
- Готовые: `QuestApplication.java` и `QuestReward.java` в `src/main/java/learning/task063/`.
- Тесты: `src/test/java/learning/task063/QuestRewardControllerTest.java`.

## Запуск тестов

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
