# Задача №67 — Лимит мест в рейде из конфигурации

**Сложность:** мини-проект, базовый Spring Boot.

**Закрепи:** самостоятельное проектирование Spring MVC API, DTO и модели ответа, состояние в памяти, обработку JSON и тесты `MockMvc`.

**Новая тема:** привязка внешних настроек к типу через `@ConfigurationProperties`.

## Материал: от числа в файле до проверки лимита

**Настройка даёт твоему коду число. Правило ограничения задаёт написанный тобой `if`.**

Например, раньше проверка могла выглядеть так:

```java
if (reservations >= 5) {
    // Отказать в новом бронировании.
}
```

С настройкой меняется источник числа:

```java
if (reservations >= settings.maxReservations()) {
    // Отказать в новом бронировании.
}
```

Имя `maxReservations` мы выбрали для понятности. Spring связывает ключ из файла с полем Java-объекта; смысл «это максимум бронирований» задаётся использованием этого числа в проверке. [Привязка настроек в Spring Boot](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html#features.external-config.typesafe-configuration-properties).

Вся цепочка:

```text
application.properties: shop.max-reservations=5
    ↓ Spring читает значение и создаёт объект настройки
ShopSettings: maxReservations = 5
    ↓ Spring передаёт этот объект в конструктор ShopController
settings.maxReservations() возвращает 5
    ↓ твой код сравнивает количество бронирований с этим числом
reservations >= 5 → отказ; reservations < 5 → можно добавить
```

Ниже отдельный рабочий пример про лимит бронирований магазина. В нём четыре файла в одном Spring Boot проекте со `spring-boot-starter-web`:

```text
src/main/java/demo/DemoApplication.java
src/main/java/demo/ShopSettings.java
src/main/java/demo/ShopController.java
src/main/resources/application.properties
```

**1. Значение в файле** `src/main/resources/application.properties`:

```properties
shop.max-reservations=5
```

Spring Boot читает этот файл при запуске. Число `5` станет настройкой приложения.

**2. Java-тип для настройки** `src/main/java/demo/ShopSettings.java`:

```java
package demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shop")
public record ShopSettings(int maxReservations) {
}
```

`prefix = "shop"` выбирает ключи, начинающиеся с `shop.`. Оставшаяся часть ключа `max-reservations` соответствует компоненту record `maxReservations`. Spring преобразует текст `5` в `int`.

**3. Включение поиска типов настроек** `src/main/java/demo/DemoApplication.java`:

```java
package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

`@ConfigurationPropertiesScan` ищет `@ConfigurationProperties` в пакете `demo` и его подпакетах. После этого Spring создаёт объект `ShopSettings` со значением из файла.

**4. Чтение настройки и проверка ограничения** `src/main/java/demo/ShopController.java`:

```java
package demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShopController {
    private final ShopSettings settings;
    private int reservations = 0;

    public ShopController(ShopSettings settings) {
        this.settings = settings;
    }

    @GetMapping("/demo/limit")
    public int limit() {
        return settings.maxReservations();
    }

    @PostMapping("/demo/reservations")
    public ResponseEntity<String> reserve() {
        if (reservations >= settings.maxReservations()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Свободных мест нет");
        }

        reservations++;
        return ResponseEntity.ok("Бронирование создано");
    }
}
```

Разбор:

- Конструктор получает созданный Spring объект `ShopSettings` и сохраняет его в `settings`.
- `reservations` хранит текущее количество бронирований. Это состояние приложения.
- `settings.maxReservations()` возвращает число из настройки. Здесь это `5`.
- `if` проверяет количество **до добавления**: когда уже заняты все пять мест, срабатывает отказ.
- `return` внутри `if` завершает метод, поэтому `reservations++` при отказе не выполняется.
- Когда место есть, `reservations++` добавляет одно бронирование.

Запусти `DemoApplication.main()` в IntelliJ IDEA и открой `http://localhost:8080/demo/limit`: ответ — `5`. Затем отправляй последовательно из терминала:

```shell
curl -i -X POST http://localhost:8080/demo/reservations
```

| Запрос | Бронирований до запроса | Проверка | Ответ |
| --- | --- | --- | --- |
| Первый | 0 | `0 >= 5` — false | `200`, бронирование создано |
| Пятый | 4 | `4 >= 5` — false | `200`, бронирование создано |
| Шестой | 5 | `5 >= 5` — true | `409`, свободных мест нет |

Поменяй свойство на `7` и перезапусти приложение: тот же `if` теперь сравнивает с `7`, а счётчик снова начинается с нуля. **Польза настройки: можно менять число в файле, сохраняя код проверки.**

### Где это находится в задаче №67

В `src/main/resources/application.properties` уже записано `guild.raids.max-participants-per-raid=3`. При обычном запуске Spring берёт оттуда `3`.

В стартовом файле `src/main/java/learning/task067/RaidNightApplication.java` поиск настроек включается так:

```java
package learning.task067;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RaidNightApplication {
    public static void main(String[] args) {
        SpringApplication.run(RaidNightApplication.class, args);
    }
}
```

Отдельный файл `src/main/java/learning/task067/RaidSettings.java` может выглядеть так:

```java
package learning.task067;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "guild.raids")
public record RaidSettings(int maxParticipantsPerRaid) {
}
```

Теперь Spring создаст `RaidSettings` со значением `3`. Передай его через конструктор выбранному тобой компоненту по образцу `ShopController` и получи лимит вызовом `settings.maxParticipantsPerRaid()`. Имя типа и структуру остальных компонентов выбирай сам.

Дальше твой код должен посчитать записи именно на выбранный `raidId`, сравнить их количество с полученным лимитом и при заполнении вернуть `409` до сохранения новой записи. В примере магазина счётчик общий; в задании количество записей определяется отдельно для каждого рейда.

В тесте `@SpringBootTest(properties = "guild.raids.max-participants-per-raid=2")` переопределяет значение из файла: там этот же вызов вернёт `2`. Для привязки к `record` Spring Boot использует его конструктор; в здешнем `pom.xml` включён необходимый флаг `<parameters>true</parameters>`. Аннотация `@Component` на `RaidSettings` не нужна: тип регистрирует `@ConfigurationPropertiesScan`. [Документация Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html).

## Проект

Гильдия записывает игроков на рейды с ограниченным количеством мест. Администратор задаёт вместимость в конфигурации: после изменения настройки и перезапуска приложение принимает другое количество игроков без изменения Java-кода. Каждый рейд заполняется независимо.

Спроектируй API записи игроков. Выбери модели и структуру компонентов самостоятельно. Маршрут и внешний HTTP-контракт заданы ниже.

### Правила

- `raidId` — положительный `long`.
- `playerName` — от 1 до 30 непробельных символов.
- Максимум записей на один рейд задаётся свойством `guild.raids.max-participants-per-raid`; в `application.properties` уже указано значение по умолчанию `3`.
- Для тестов значение будет переопределено на `2`, поэтому приложение должно читать свойство из Spring-конфигурации.
- Лимит применяется отдельно к каждому рейду.
- Повторная запись имени игрока разрешена и занимает отдельное место.
- Каждая успешная запись получает положительный ID, уникальный внутри своего рейда. В разных рейдах ID могут совпадать: конкретную запись определяет пара `(raidId, id)`, присутствующая в её адресе.
- Состояние хранится в памяти и очищается после перезапуска. SQL и база данных не нужны.

### HTTP-контракт

- `POST /guild/raid-nights/{raidId}/participants` принимает:

  ```json
  {"playerName":"Кирилл"}
  ```

  Если место есть, верни `201 Created`, заголовок `Location: /guild/raid-nights/{raidId}/participants/{id}` и JSON с `id`, `raidId`, `playerName`.
- Если лимит этого рейда заполнен, верни `409 Conflict` без тела.
- Нулевой, отрицательный, нечисловой или выходящий за диапазон `long` `raidId` возвращает `400`.
- Пустое, отсутствующее, `null`, пробельное или слишком длинное имя и синтаксически неверный JSON возвращают `400`.

### Примеры

1. При наличии места запрос с `{"playerName":"Кирилл"}` к `/guild/raid-nights/42/participants` создаёт запись и возвращает `201` с ответом вроде `{"id":1,"raidId":42,"playerName":"Кирилл"}`.
2. Если свойство лимита равно `2` и на рейд `42` уже записаны два игрока, следующая запись на этот рейд получает `409`.
3. При том же лимите рейд `43` всё ещё может принять двух игроков: вместимость считается отдельно для каждого `raidId`.

Например, `/guild/raid-nights/42/participants/1` и `/guild/raid-nights/43/participants/1` — адреса разных записей. Общая нумерация между рейдами не требуется.

## Самостоятельное проектирование

- Сам выбери DTO, модель ответа, структуру хранения и разделение логики.
- Сам реши, как Spring-компонент получит типизированную настройку лимита; используй `@ConfigurationProperties` и подключи сканирование настроек.
- Размещай самостоятельные модели и компоненты в отдельных `.java`-файлах.
- Сохрани указанные маршруты, имена полей, статусы, лимиты и `Location`.

## Критерии готовности

- Тесты подтверждают успешную запись и `Location`, положительные ID без повторений внутри одного рейда, границы имени и ошибки запроса.
- Тесты подтверждают, что переопределённое свойство задаёт лимит `2`, полный рейд отвечает `409`, а другой рейд имеет свой лимит.
- В полном рейде отклонённая запись не возвращает `201`.
- Используй подключённые зависимости; новые зависимости добавлять не нужно.

## Файлы

- Стартовый класс: `src/main/java/learning/task067/RaidNightApplication.java`.
- Настройка по умолчанию: `src/main/resources/application.properties`.
- Контрактные тесты: `src/test/java/learning/task067/RaidNightControllerTest.java`.
- Всю реализацию спроектируй самостоятельно в `src/main/java/learning/task067/`.

## Запуск тестов

```shell
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH bash ./mvnw clean test
```
