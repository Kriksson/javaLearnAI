# №93 — Банк: PostgreSQL с постоянным хранилищем

Сложность: **базовая**. Приложение и конфигурацию создаёшь вручную с нуля в пакете `learning.task093`. Новое понятие — **именованный Docker volume**. Закрепляем Compose, сеть сервисов, внешнюю конфигурацию, Flyway, SQL и чтение через REST.

## Зачем это нужно

В №92 мы заменяли контейнер API, сохраняя работающий PostgreSQL. Теперь нужно сохранить платёжные заявки даже после удаления контейнеров через обычный `docker compose down` и следующего запуска.

Контейнер PostgreSQL запускает программу. Именованный volume хранит файлы базы на диске Docker-хоста и подключается к контейнеру по известному имени. При создании нового контейнера подключаем прежний volume — PostgreSQL открывает существующие данные. Volume не является резервной копией: SQL DELETE и ошибки изменения данных тоже сохраняются. [Жизненный цикл volume](https://docs.docker.com/engine/storage/volumes/).

```text
старый контейнер db → volume с файлами PostgreSQL
                          ↑
новый контейнер db ────────┘
```

Это сохранение на том же Docker-хосте, с тем же именем проекта и volume. Перенос на другой сервер и резервные копии здесь не изучаем. Не подключай один каталог данных одновременно к двум серверам PostgreSQL.

## Отдельный работающий пример: файл отчёта

Создай отдельную папку `report-volume-demo`. В ней compose.yaml:

```yaml
services:
  writer:
    image: postgres:17-alpine
    entrypoint: ["sh", "-c"]
    volumes:
      - report_store:/notes
volumes:
  report_store:
```

Здесь образ используется ради готовой оболочки `sh`; PostgreSQL не запускаем, потому что заменили entrypoint. Пример хранит текстовый файл, а не базу задания.

- Верхний `volumes` находится на одном уровне с `services`: объявляет хранилище `report_store`.
- `writer.volumes` подключает его к сервису.
- Слева от двоеточия `report_store` — имя хранилища, справа `/notes` — каталог внутри контейнера. Запись в `/notes` попадает в volume.
- Имя проекта ограничивает область хранилища: с `-p report-volume-demo` Docker создаст `report-volume-demo_report_store`.

В каталоге примера выполни:

```shell
docker compose -p report-volume-demo run --rm writer 'printf "%s\n" "report saved" > /notes/report.txt'
docker compose -p report-volume-demo down
docker compose -p report-volume-demo run --rm writer 'cat /notes/report.txt'
```

Первая команда создаёт контейнер, записывает файл и удаляет контейнер после завершения (`--rm`). Обычный `down` сохраняет именованный volume. Третья команда создаёт другой контейнер, подключает прежний volume и выводит **report saved**. Значит, файл пережил удаление контейнера. [Volumes в Compose](https://docs.docker.com/engine/storage/volumes/#use-a-volume-with-docker-compose).

После примера удалить его учебные данные можно явно:

```shell
docker compose -p report-volume-demo down --volumes
```

## Сценарий и внешний контракт

Сохраним знакомый API, чтобы сосредоточиться на хранилище. Банк хранит заявки на платежи. `GET /bank/payments/count` публичный, возвращает `200`, `application/json` и ровно одно поле:

```json
{"count":0}
```

`count` — целое число, фактическое количество строк в PostgreSQL. Каждый запрос читает текущие данные. Создание и удаление заявок выполняем через SQL, поэтому HTTP POST для записи не нужен. `GET /missing` → `404`; `POST /bank/payments/count` → `405`. Тела ошибок свободные.

Путь данных: curl → опубликованный порт → Spring MVC → JDBC или JPA → db:5432 → SQL COUNT → long → DTO → JSON.

**Способ доступа выбираешь сам: JdbcTemplate или Spring Data JPA.** Внутренние имена компонентов свободные. Если используешь JPA, явно отобрази сущность на таблицу миграции и используй Hibernate `ddl-auto=validate`. Схему создаёт только Flyway. Без JPA настройки `spring.jpa.*` не нужны. Spring Security в этой практике не требуется.

## Таблица и миграция

Обязательный файл: `src/main/resources/db/migration/V1__create_payment_requests.sql`.

Таблица **payment_requests**:

| Колонка | Требования |
|---|---|
| `id` | BIGINT, identity, PRIMARY KEY |
| `description` | VARCHAR(100), NOT NULL |

Новая база пуста. В Flyway одна успешная версия V1 с именем `V1__create_payment_requests.sql`. После замены контейнеров сохраняются строки, их ID, история Flyway и последовательность identity. Не очищай таблицы при старте, не меняй V1 между запусками, не используй `schema.sql` или Hibernate для создания схемы.

Три примера:

1. Новое хранилище → `{"count":0}`.
2. Добавил Rent и Travel → `{"count":2}`; выполнил down/up без удаления volume → снова `{"count":2}`.
3. Добавил строки с ID 1 и 2, удалил вторую, заменил контейнер db → строка 1 сохранилась, следующая вставка получает ID 3.

## Compose: обязательные настройки

Ровно два сервиса **api** и **db**, общая сеть Compose по умолчанию. Без собственных сетей, `network_mode` и `container_name`. Подстановки — через `environment`, без `env_file` сервиса.

- **api**: build с контекстом `.` и Dockerfile; без image. Ровно один опубликованный TCP-порт: `127.0.0.1:${BANK_HTTP_PORT:-8085}:8080`. Значение `0` позволяет выбрать свободный порт. Без mounts/volumes.
- **db**: `postgres:17-alpine` или `docker.io/library/postgres:17-alpine`, без build и опубликованных портов, без `POSTGRES_HOST_AUTH_METHOD=trust`.

| Переменная в .env или терминале | Если отсутствует или пуста | В db | В api |
|---|---|---|---|
| `BANK_DB_NAME` | `bank_lab` | `POSTGRES_DB` | имя базы в URL |
| `BANK_DB_USER` | `bank_app` | `POSTGRES_USER` | `SPRING_DATASOURCE_USERNAME` |
| `BANK_DB_PASSWORD` | `learning-only` | `POSTGRES_PASSWORD` | `SPRING_DATASOURCE_PASSWORD` |

В API URL: `jdbc:postgresql://db:5432/имя_базы`. Два сервиса читают одну исходную переменную, например BANK_DB_PASSWORD, и получают её под разными именами. Они не читают environment друг друга.

### Новое требование к хранилищу

Объяви ровно один именованный volume с ключом **bank_data** на верхнем уровне Compose. Подключи его только к **db**, ровно один mount с типом volume, с доступом на запись и каталогом назначения **/var/lib/postgresql/data**. Это путь данных PostgreSQL 17. Не меняй PGDATA. [Каталог данных официального образа](https://hub.docker.com/_/postgres#pgdata).

Имя Docker-volume должно определяться именем проекта: для проекта `bank-volume-lab` получится `bank-volume-lab_bank_data`. Используй обычный локальный volume, без external, driver_opts или общего фиксированного имени. Таким образом два проекта с одним Compose-файлом получат разные базы. Проверка задаёт собственное имя проекта.

Сохраняй имя проекта при повторном запуске: изменение `-p` создаёт другое окружение и другое хранилище. `POSTGRES_*` инициализируют пустое хранилище; изменение пароля в .env не меняет пароль уже существующего пользователя базы.

Healthcheck и автоматическое ожидание готовности введём в №94. Здесь запускаем db отдельно, дожидаемся TCP-готовности, потом запускаем API. Не добавляй healthcheck в сервисы этой задачи.

## Dockerfile, сборка и самостоятельно создаваемые файлы

- Пакет `src/main/java/learning/task093/`: старт приложения, REST-контроллер, DTO и необходимые компоненты доступа к данным. Каждый самостоятельный класс — отдельный файл.
- `src/main/resources/application.properties`: настройки соединения из окружения и необходимые настройки выбранного способа доступа.
- SQL-миграция по указанному пути.
- В корне: Dockerfile, .dockerignore, compose.yaml и локальный .env по желанию.
- Сам подключи только необходимые зависимости: Web, JDBC либо JPA, PostgreSQL JDBC-драйвер и Flyway с модулем PostgreSQL. В окружении проверки доступны Spring Boot 3.5.16, PostgreSQL JDBC 42.7.13, Flyway 11.7.2. Исходники Java 21.
- Maven package создаёт исполняемый `target/bank-service.jar` с JarLauncher, стартовым классом `learning.task093` и миграцией внутри.
- Dockerfile: один FROM с Java 21, COPY готового JAR и JSON ENTRYPOINT для `java -jar`. Сборку в несколько этапов изучим позже.
- .dockerignore исключает .git, .idea, .maven-home, src, tasks, .env. .gitignore исключает .env. Пароль по умолчанию — только для учебной локальной базы.

Заготовок решения нет. После архивации №92 её файлы находятся в tasks/completed и не участвуют в рабочей сборке. Учебный pom.xml остаётся окружением проверки; зависимости и конфигурацию своей задачи настраиваешь сам.

## Ручная проверка без случайного удаления базы

Выбери отдельное имя проекта для этой практики, например **bank-volume-lab**. Все команды ниже выполняются из корня учебного проекта и используют именно его. Контейнеры прежних задач с другим именем проекта не затрагиваются.

1. Собери JAR в Maven IDEA через package или командой:

   ```shell
   JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" -Dmaven.test.skip=true clean package
   ```

   Пропуск тестов здесь временный, чтобы получить JAR для проверки образа. Финальный clean test обязателен.

2. Запусти только базу и проверь готовность:

   ```shell
   docker compose -p bank-volume-lab up -d db
   docker compose -p bank-volume-lab exec db pg_isready -h db -U bank_app -d bank_lab
   ```

   Повторяй вторую команду до `accepting connections`. Если изменил имя базы/пользователя, подставь свои значения.

3. Запусти API после готовности базы:

   ```shell
   docker compose -p bank-volume-lab up -d --build --no-deps api
   curl -i http://127.0.0.1:8085/bank/payments/count
   ```

   Если порт 8085 занят API прежней задачи, задай другой BANK_HTTP_PORT в .env и используй его в curl.

4. Добавь строки и проверь счётчик:

   ```shell
   docker compose -p bank-volume-lab exec db psql -U bank_app -d bank_lab -c "INSERT INTO payment_requests(description) VALUES ('Rent'), ('Travel');"
   curl -i http://127.0.0.1:8085/bank/payments/count
   ```

5. Удали контейнеры, сохраняя хранилище, затем запусти их вновь:

   ```shell
   docker compose -p bank-volume-lab down
   docker volume inspect bank-volume-lab_bank_data
   docker compose -p bank-volume-lab up -d db
   docker compose -p bank-volume-lab exec db pg_isready -h db -U bank_app -d bank_lab
   ```

   После accepting connections:

   ```shell
   docker compose -p bank-volume-lab up -d --no-build --no-deps api
   curl -i http://127.0.0.1:8085/bank/payments/count
   ```

   Ожидаем count 2, прежние строки и одну запись Flyway V1. INSERT после пересоздания не должен сбрасывать последовательность ID.

| Действие | Что происходит с именованным volume |
|---|---|
| stop/start | сохраняется |
| замена контейнера db | подключается то же хранилище |
| down, затем up с тем же именем проекта | сохраняется и подключается снова |
| down --volumes | удаляется; следующий запуск создаст пустую базу |
| запуск с другим -p | используется другое хранилище |

Для завершения обычной работы достаточно **down без --volumes**. Команда `docker compose -p bank-volume-lab down --volumes` предназначена для сознательного удаления учебных данных этого проекта. Автотесты проверяют этот сценарий только на своей временной базе. [Правила down](https://docs.docker.com/reference/cli/docker/compose/down/).

## Автоматические тесты и готовность

Тесты тренера:

- `src/test/java/learning/task093/PersistentPostgresComposeApiTest.java`
- `src/test/java/learning/task093/ComposeCommands.java`

13 тестов проверяют настройки по умолчанию, пустые переменные и .env; исполняемый Java 21 JAR; API и актуальные SQL-данные; ограничения таблицы; историю Flyway; runtime-сеть и именованный volume; пересоздание API; пересоздание db с сохранением строк и identity; down/up; независимость двух проектов; явное удаление volume и свежую базу; 404/405.

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Нужен работающий Docker Engine и доступ текущего процесса к docker.sock. Тесты собирают образ один раз, используют свободный порт и собственное имя проекта, ждут реальную готовность базы и удаляют только свои контейнеры, образ и volumes. Учебные данные твоего ручного запуска не используются. Логи команд: target/task093-checks.

Критерий готовности: все 13 тестов прошли; выполнен внешний контракт; данные, история миграции и последовательность ID переживают пересоздание контейнеров; разные имена проекта разделяют хранилища. Решение ученика прошло все проверки.

Проверено 10.10.2026: все 13 тестов прошли через Docker Engine; полная сборка и проверка — 51,693 секунды. Задача засчитана.
