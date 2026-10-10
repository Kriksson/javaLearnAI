# №92 — Банк: API и PostgreSQL в Docker Compose

**Статус: решена.** Проверка 10 октября 2026: 9 контейнерных тестов, 0 ошибок и пропусков; полный `clean test` — 21,080 с.

Сложность: **базовая**. Создаёшь приложение и конфигурацию с нуля. Пакет Java — `learning.task092`; внутренние классы, методы и разбиение выбираешь сам.

Закрепляем: Compose, Dockerfile, файл окружения, Spring Boot REST, DTO, Spring JDBC, SQL `COUNT(*)` и первую миграцию Flyway.

Впервые: **PostgreSQL в контейнере** и **подключение между контейнерами по имени сервиса**. Volumes и healthcheck вводим отдельно. Здесь учимся подключать уже знакомое приложение к базе внутри Compose, без новых операций регистрации, перевода денег и безопасности.

## Сценарий

Банк хранит заявки на платежи. Небольшое информационное API показывает их количество, каждый раз читая текущие данные PostgreSQL. API и база запускаются в отдельных контейнерах; настройки соединения передаются при запуске.

На этой практике endpoint публичный. Заявки добавляем и удаляем SQL-командами, поэтому POST для создания заявки не нужен.

## PostgreSQL в контейнере: что изменилось

Раньше PostgreSQL работал отдельно на компьютере. Теперь официальный образ PostgreSQL запускает сервер в сервисе Compose. На первом запуске с пустым каталогом данных образ создаёт пользователя и базу по переменным:

| Переменная внутри контейнера базы | Назначение |
|---|---|
| `POSTGRES_DB` | имя создаваемой базы |
| `POSTGRES_USER` | имя пользователя PostgreSQL |
| `POSTGRES_PASSWORD` | его пароль |

Эти переменные использует образ PostgreSQL для начальной настройки. Они не являются настройками Spring. Образ не создаёт твои таблицы приложения: таблицу в этой практике создаёт Flyway. При уже существующих данных изменение этих переменных не переименовывает базу и не меняет пароль автоматически. [Официальный образ PostgreSQL](https://hub.docker.com/_/postgres).

Не путай пользователя PostgreSQL с пользователем банковского API и ролью Spring Security: здесь речь только об учётных данных соединения с базой. Тесты используют собственные временные значения, а не твой PostgreSQL на компьютере.

## Сеть Compose: почему адресом будет db

Compose создаёт общую сеть проекта. Сервисы в этой сети доступны друг другу по имени. IP-адреса запоминать не требуется.

Внутри API `localhost` означает сам контейнер API. Чтобы обратиться к PostgreSQL в соседнем сервисе **db**, используй имя **db** и его внутренний порт **5432**. Публиковать порт базы на компьютер для такого соединения не нужно. [Сеть и имена сервисов Compose](https://docs.docker.com/compose/how-tos/networking/).

Путь запроса в задании:

```text
curl → порт компьютера 8085 → API:8080 → Spring MVC
     → JdbcTemplate → db:5432 → PostgreSQL → количество строк → DTO → JSON
```

JDBC URL имеет форму `jdbc:postgresql://имя_сервиса:порт/имя_базы`. Это адрес соединения внутри сети контейнеров. Имя сервиса и имя базы — разные части адреса.

## Отдельный работающий пример обоих новых понятий

Создай отдельную папку `catalog-demo` и запиши в её compose.yaml пример. Он не содержит банковское API, Java-код или миграцию твоего задания.

```yaml
services:
  catalog:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: demo_catalog
      POSTGRES_USER: demo_user
      POSTGRES_PASSWORD: demo-pass
  console:
    image: postgres:17-alpine
    environment:
      PGHOST: catalog
      PGPORT: "5432"
      PGDATABASE: demo_catalog
      PGUSER: demo_user
      PGPASSWORD: demo-pass
```

Разбор ключевых строк:

- `catalog` — имя сервиса сервера; `image` выбирает готовый образ PostgreSQL 17.
- `POSTGRES_*` задают начальные параметры сервера в примере.
- `console` использует тот же образ ради готового SQL-клиента `psql`; отдельный сервер в этом сервисе мы не запускаем.
- `PGHOST: catalog` направляет клиент к другому сервису по его имени.
- `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` читает клиент psql. У Spring названия настроек соединения будут другими.
- Блоков `ports` нет: соединение между сервисами идёт внутри сети Compose.

В папке примера:

```shell
docker compose up -d catalog
docker compose exec catalog pg_isready -h catalog -U demo_user -d demo_catalog
```

Дождись вывода `accepting connections`; если сервер ещё запускается, повтори вторую команду. Важно проверять TCP-адрес через `-h catalog`: при инициализации образ временно запускает сервер только с локальным Unix-сокетом. [Команда pg_isready](https://www.postgresql.org/docs/17/app-pg-isready.html).

Затем выполни запрос из отдельного контейнера клиента:

```shell
docker compose run --rm console psql -c "SELECT current_database(), current_user;"
```

`run` создаёт контейнер сервиса console для указанной команды, а `--rm` удаляет его после завершения. Увидишь `demo_catalog` и `demo_user`.

Путь данных: `PGHOST=catalog` → Docker находит сервер в сети → psql соединяется с ним на 5432 → PostgreSQL выполняет SELECT → результат выводится в терминал. Это соединение из одного контейнера в другой.

После примера:

```shell
docker compose down --volumes
```

Удаляются только ресурсы этого учебного проекта, включая его временные данные базы.

## Контракт API и таблицы

`GET /bank/payments/count` доступен без аутентификации. Ответ `200`, `Content-Type: application/json`, ровно одно поле с целым числом:

```json
{"count":0}
```

Количество — фактическое число строк таблицы **payment_requests** в подключённой PostgreSQL. Не храни счётчик в памяти и не возвращай заранее заданное значение. Каждый запрос должен видеть текущие вставки и удаления.

Таблицу создаёт твоя миграция **src/main/resources/db/migration/V1__create_payment_requests.sql**:

| Колонка | Требование |
|---|---|
| `id` | BIGINT, identity, PRIMARY KEY; ID создаёт PostgreSQL |
| `description` | VARCHAR(100), NOT NULL |

После первого запуска таблица пуста. В `flyway_schema_history` есть одна успешно применённая версия V1 с этим именем SQL-файла. Hibernate и `schema.sql` не создают таблицу вместо Flyway.

`GET /missing` возвращает `404`; `POST /bank/payments/count` — `405`. Тела ошибок не фиксируются. Тесты меняют строки через SQL, поэтому создание заявок через HTTP реализовывать не нужно.

Используй знакомый Spring JDBC: результат SQL-агрегирования преобразуется в `long`, затем в DTO. Отдельная JPA-сущность здесь не нужна. Роли компонентов выбираешь сам; каждый самостоятельный класс — отдельный файл.

## Конфигурация приложения и сборки

Сам подключи Web, Spring JDBC, PostgreSQL JDBC-драйвер и Flyway с модулем PostgreSQL. В текущем учебном окружении уже использовались версии Spring Boot `${spring.boot.version}`, драйвер **42.7.13** и Flyway **11.7.2**. Security, JPA и зависимости для других задач не нужны. Версия исходников — **21**.

На фазе Maven `package` должен получаться исполняемый **target/bank-service.jar** с классами `learning.task092`, зависимостями и SQL-миграцией. Стартовый класс и имена Java-компонентов свободные.

Dockerfile: один этап, runtime Java 21, COPY готового JAR с хоста, ENTRYPOINT в JSON-форме с Java и `-jar`. .dockerignore содержит `.git`, `.idea`, `.maven-home`, `src`, `tasks`, `.env`; JAR доступен для COPY. Локальный `.env` исключён из Git.

Приложение читает стандартные настройки `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` через окружение Spring Boot. В контейнере их задаём именами **SPRING_DATASOURCE_URL**, **SPRING_DATASOURCE_USERNAME**, **SPRING_DATASOURCE_PASSWORD**. Spring Boot создаёт DataSource; JdbcTemplate и Flyway используют это соединение. [Внешняя конфигурация Spring Boot](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html).

Для закрепления знакомого JdbcTemplate отдельный пример агрегирования по другой таблице:

```java
Long total = jdbc.queryForObject("SELECT COUNT(*) FROM documents", Long.class);
```

SQL возвращает одну строку с одним числом. `Long.class` указывает тип результата. Затем число можно передать в DTO. Этот пример не создаёт таблицу и не является кодом твоего endpoint.

## Контракт compose.yaml: какие настройки куда идут

Ровно два сервиса: **api** и **db**. Используем общую сеть Compose по умолчанию: без `network_mode`, собственных сетей и фиксированного container_name.

**api** собирает собственный образ через `build` с контекстом `.` и Dockerfile. Поле `image` ему не задавай. Порт приложения — 8080; публикуется ровно один TCP-порт `127.0.0.1`, номер компьютера берётся из **BANK_HTTP_PORT**, по умолчанию 8085; значение 0 допускает свободный порт в тестах.

**db** использует готовый образ **postgres:17-alpine** (полное имя `docker.io/library/postgres:17-alpine` тоже допустимо). В этом сервисе `build` и публикация портов не нужны.

Через подстановку в `environment` передай одни и те же значения двум потребителям:

| Переменная в .env или терминале | Значение по умолчанию, если отсутствует или пуста | Внутри db | Внутри api |
|---|---|---|---|
| `BANK_DB_NAME` | `bank_lab` | `POSTGRES_DB` | имя базы в конце SPRING_DATASOURCE_URL |
| `BANK_DB_USER` | `bank_app` | `POSTGRES_USER` | `SPRING_DATASOURCE_USERNAME` |
| `BANK_DB_PASSWORD` | `learning-only` | `POSTGRES_PASSWORD` | `SPRING_DATASOURCE_PASSWORD` |

Первый столбец — переменные, которые Compose читает при подстановке в YAML. Например, при `BANK_DB_NAME=training_bank` в выбранном файле окружения выражение `${BANK_DB_NAME:-bank_lab}` даст строку `training_bank`. Через `environment` передай эту строку в контейнер db под именем `POSTGRES_DB`, а в контейнер api — как часть `SPRING_DATASOURCE_URL`: `jdbc:postgresql://db:5432/training_bank`. Само наличие переменной в `.env` не передаёт её автоматически в контейнеры: назначения задаёшь ты в `compose.yaml`.

Пароль по умолчанию — только значение для этой локальной учебной практики. Реальные пароли в Git не добавляй.

JDBC URL API должен получаться **jdbc:postgresql://db:5432/имя_базы**. Не используй localhost или опубликованный порт компьютера. В обеих настройках имя базы должно совпадать; то же относится к пользователю и паролю.

Для подстановки по отсутствующему или пустому значению используй уже знакомый вариант `${VARIABLE:-default}`. Как и в №91, используй `environment` и файл, выбранный через `--env-file`, без поля `env_file` сервиса. У PostgreSQL не задавай `POSTGRES_HOST_AUTH_METHOD=trust`.

Не добавляй `volumes` или `healthcheck` в сервисы. Образ PostgreSQL сам использует каталог данных в своём анонимном volume; управление именованным volume будет следующей темой. На этом шаге не обещаем сохранение базы после её пересоздания. Проверяем сохранение строк при пересоздании **только API**, когда контейнер db продолжает работать.

## Запуск по шагам

Готовность базы проверяем отдельно перед запуском API; автоматическое ожидание через healthcheck будет позже. Одновременный старт процессов сам по себе не гарантирует готовность PostgreSQL к запросам.

1. Создай Java-код, миграцию, конфигурацию, Dockerfile, .dockerignore и compose.yaml. Проверь `docker info` и `docker compose version`.
2. Собери JAR:

   ```shell
   JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" -Dmaven.test.skip=true clean package
   ```

3. Проверь `docker compose config`, затем запусти базу:

   ```shell
   docker compose up -d db
   docker compose exec db pg_isready -h db -U bank_app -d bank_lab
   ```

   Вторая команда использует значения по умолчанию. Если в `.env` задал другие имя и пользователя, подставь их. Дождись `accepting connections`.

4. Запусти API и проверь ответ:

   ```shell
   docker compose up -d --build api
   curl -i http://localhost:8085/bank/payments/count
   ```

5. Логи процессов смотри раздельно:

   ```shell
   docker compose logs db
   docker compose logs api
   ```

## Три примера результата

При стандартных имени базы и пользователе:

```shell
docker compose exec db psql -U bank_app -d bank_lab -c "INSERT INTO payment_requests(description) VALUES ('Rent'), ('Travel');"
curl -i http://localhost:8085/bank/payments/count
```

1. Новая пустая таблица → `{"count":0}`.
2. После двух INSERT → `{"count":2}`; после удаления одной строки → `{"count":1}`.
3. После `docker compose up -d --no-build --no-deps --force-recreate api` число остаётся прежним: пересоздан API, база продолжает работать, V1 повторно не создаёт таблицу.

`--no-deps` в третьем примере запускает только выбранный сервис, без его зависимостей. Изменение данных не требует пересборки образа или перезапуска API.

Когда закончил с временными данными этой практики:

```shell
docker compose down --volumes
```

Здесь `--volumes` удаляет также временные данные базы этого проекта. В следующей практике отдельно разберём сохранение базы и назначение volume.

## Тесты и критерии готовности

- `src/test/java/learning/task092/PostgresComposeApiTest.java` — **9 тестов**: конфигурация двух сервисов; JAR и миграция; пустая база и история V1; актуальность INSERT/DELETE; 125 строк; границы SQL-колонок; пересоздание API с прежней базой; сеть и отсутствие опубликованного порта PostgreSQL; 404/405.
- `src/test/java/learning/task092/ComposeCommands.java` — команды с таймаутами, изолированным окружением и логами.

Тесты создают собственный Compose-проект с временной PostgreSQL, собственными учётными данными и свободным HTTP-портом. Базу на твоём компьютере и твой `.env` не используют. Сначала собирают JAR и образ API, затем запускают db, дожидаются рабочего TCP-соединения и запускают API. Логи — `target/task092-checks/`. После проверки удаляют только собственный проект, его образ API и его временные данные базы; базовые образы остаются в кеше.

Полная проверка:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Для нового образа PostgreSQL первый запуск может дополнительно скачать его из registry. Завершившийся контейнер обнаруживается сразу; API не ждут 90 секунд после его завершения.

Сам проверь через config адрес соединения и совпадение настроек базы и API; через psql — миграцию и строки; через curl — актуальное количество и ошибки. Новые проверки входящего JSON не требуются: операция только читает количество.

Тесты проверены тренером на временном приложении через Docker Engine и Compose с PostgreSQL 17: 9 прошли, 0 ошибок и пропусков; Maven BUILD SUCCESS за 48 секунд. Решение в рабочем проекте создаёшь сам.

Критерии готовности: все 9 тестов проходят, API читает настоящий PostgreSQL внутри сети Compose, изменения строк видны без перезапуска, миграция применяется один раз, ограничения соблюдены. Рабочие файлы создаёшь сам; до реализации падение тестов ожидаемо, №92 подтверждена успешной проверкой решения ученика.
