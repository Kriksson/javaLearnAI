# Задача №94 — Банк: запуск после готовности PostgreSQL

Сложность: базовая. Приложение и конфигурацию создаёшь вручную с нуля в пакете `learning.task094`. В качестве справочника можно открыть архив №93; рабочие файлы создаёшь самостоятельно.

Закрепляем Spring Boot, PostgreSQL, Flyway, DTO, настройки через окружение, Dockerfile, Compose и именованный volume. Впервые: **healthcheck** и зависимость **depends_on с service_healthy**. JDBC или JPA выбираешь сам.

## Зачем это нужно

Раньше ты запускал db, проверял `pg_isready`, затем запускал api. Контейнер PostgreSQL может уже работать, пока база ещё создаёт каталог данных. API, запущенное в этот момент, получает отказ в соединении и завершается. Сообщение базы «ready» позднее само по себе не перезапускает API.

Теперь Docker регулярно выполняет короткую команду внутри db. Это **healthcheck** — проверка готовности сервиса. Состояние начинается с `starting`, после успешной проверки становится `healthy`, после заданного числа неудач — `unhealthy`.

Compose читает зависимость api от db. Условие `service_healthy` означает: перед запуском api дождаться успешной проверки db. Короткая запись `depends_on: [db]` ждёт запуска контейнера, но не готовности базы.

Это механизм порядка запуска. Если база станет недоступной позднее, healthcheck покажет проблему, но сам не перезапустит контейнеры и не обеспечит восстановление приложения. Политики перезапуска разберём отдельно.

## Небольшой работающий пример на другом случае

Производитель отчёта создаёт файл через три секунды. Читатель запускается после появления файла. Сохрани пример **за пределами рабочего решения**, например `/tmp/report-ready-demo/compose.yaml`:

```yaml
services:
  producer:
    image: postgres:17-alpine
    entrypoint: ["sh", "-c"]
    command: ["sleep 3; touch /tmp/ready; tail -f /dev/null"]
    healthcheck:
      test: ["CMD-SHELL", "test -f /tmp/ready"]
      interval: 1s
      timeout: 1s
      retries: 5
      start_period: 1s
  reader:
    image: postgres:17-alpine
    entrypoint: ["sh", "-c"]
    command: ["printf '%s\n' 'Report is ready'"]
    depends_on:
      producer:
        condition: service_healthy
```

Образ PostgreSQL здесь используется только как среда с `sh`; базу пример не запускает.

```shell
cd /tmp/report-ready-demo
docker compose -p report-ready-demo up -d
docker compose -p report-ready-demo ps -a
docker compose -p report-ready-demo logs reader
docker compose -p report-ready-demo down
```

Разбор ключевых строк:

- `sleep 3; touch ...` имитирует подготовку сервиса, `tail` оставляет контейнер запущенным.
- `CMD-SHELL` выполняет следующую строку через оболочку внутри контейнера.
- `test -f` возвращает код `0`, когда файл существует; Docker считает это успехом.
- `interval` задаёт период проверок, `timeout` ограничивает одну проверку, `retries` — число последовательных неудач до `unhealthy`.
- `start_period` даёт время на первоначальный запуск: неудачи в этот период не расходуют число попыток. Успех может дать `healthy` уже во время этого периода.
- `service_healthy` связывает запуск reader с результатом healthcheck producer.

Путь данных: файл ещё отсутствует → команда проверки возвращает ненулевой код → Docker сохраняет состояние запуска → файл появился → проверка возвращает `0` → состояние `healthy` → Compose запускает reader → в логах появляется `Report is ready`.

Проверку файла и зависимость в примере написал автор YAML. В задании **ты пишешь проверку PostgreSQL и зависимость API**.

## PostgreSQL: какую команду проверять

Ты уже выполнял вручную:

```shell
pg_isready -h db -U bank_app -d bank_lab
```

Это пример для значений по умолчанию. В healthcheck нужны настройки текущего контейнера, потому что файл окружения может менять логин и имя базы. PostgreSQL-образ получает их как `POSTGRES_USER` и `POSTGRES_DB`.

Используй `pg_isready` по TCP с `-h db`. Во время первой инициализации образ PostgreSQL запускает временный сервер только с Unix-сокетом. Проверка без `-h` может принять этот временный сервер за готовность к соединению API по сети.

`pg_isready` проверяет доступность сервера, а не правильность пароля приложения. Согласование настроек двух сервисов остаётся твоей обязанностью; успешный запрос API подтвердит соединение, миграцию и чтение данных.

## Почему в healthcheck встречается $$

В обычном `environment` запись `${BANK_DB_USER:-bank_app}` обрабатывает **Compose на компьютере**. В строке проверки мы хотим использовать переменную **внутри контейнера**. Для этого знак `$` экранируется вторым `$`.

Отдельный фрагмент на другом имени:

```yaml
environment:
  REPORT_NAME: monthly
healthcheck:
  test: ["CMD-SHELL", "test -n \"$${REPORT_NAME}\""]
```

Путь значения: Compose превращает `$$` в буквальный `$` → Docker передаёт оболочке `test -n "${REPORT_NAME}"` → оболочка подставляет `monthly` из окружения контейнера → `test -n` проверяет непустую строку и возвращает `0`.

Для db применишь этот же приём к `POSTGRES_USER` и `POSTGRES_DB`. Не подставляй в проверку фиксированные `bank_app` и `bank_lab`: тесты запускают другую базу с другим логином. Допустимы `$${POSTGRES_USER}` и `$$POSTGRES_USER`; значения удобно заключать в двойные кавычки оболочки.

## Условие и внешний контракт

Банк считает заявки на оплату. Нужно самостоятельно собрать приложение и запускать API вместе с PostgreSQL **одной командой**, включая первый запуск с пустым volume.

### HTTP и данные

`GET /bank/payments/count` — публичный маршрут без аутентификации. Ответ `200`, `Content-Type: application/json`, единственное поле `count` — целое число строк, актуальное на момент запроса:

```json
{"count":0}
```

После SQL-вставки двух заявок:

```json
{"count":2}
```

Пустая таблица — `200` и `0`. Отсутствующий маршрут — `404`, `POST /bank/payments/count` — `405`. Данные не хранятся в коллекции приложения и не вычисляются при старте.

Flyway создаёт таблицу `payment_requests` миграцией `src/main/resources/db/migration/V1__create_payment_requests.sql`:

| Колонка | Ограничения |
|---|---|
| id | BIGINT, identity, PRIMARY KEY |
| description | VARCHAR(100), NOT NULL |

Не добавляй стартовые строки. Flyway — единственный создатель схемы. При выборе JPA используй `ddl-auto=validate`, отключи Open Session in View и явно отобрази сущность на `payment_requests`. При JDBC Hibernate не нужен. SQL-схему и зависимости подключаешь сам.

История успешной V1, строки и последовательность identity сохраняются после пересоздания db и `down` без удаления volumes. Следующая вставка не переиспользует уже выданный ID. Другое имя проекта получает отдельную пустую базу. Только явное удаление volume создаёт новое хранилище.

### Сборка

Исполняемый `target/bank-service.jar`, исходный код Java 21, стартовый класс в `learning.task094`. Обычный Dockerfile с одним `FROM`: Java 21, рабочий каталог, копирование готового JAR и exec-форма запуска `java -jar`. Сборка в несколько этапов пока не требуется.

В `.dockerignore` исключи `.git`, `.idea`, `.maven-home`, `src`, `tasks`, `.env`. JAR остаётся в контексте сборки. `.env` исключи также из Git.

### Compose

Ровно два сервиса: `api` и `db`. Общая стандартная сеть Compose, без собственной networks-конфигурации, `network_mode`, `container_name` или `env_file` сервиса.

`api`:

- `build`: текущий каталог и `Dockerfile`, без `image`.
- Единственный TCP-порт: `127.0.0.1:${BANK_HTTP_PORT:-8085}:8080`. Значение `0` даёт свободный порт.
- Без mounts и healthcheck API.
- Зависит только от `db`, условие `service_healthy`, зависимость обязательная.
- Через environment передаёт `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`; приложение использует их для соединения.

`db`:

- `postgres:17-alpine` или `docker.io/library/postgres:17-alpine`, без build и публикации портов.
- Ровно один writable named volume `bank_data` в `/var/lib/postgresql/data`. Объяви его на верхнем уровне volumes; локальный, без external, driver_opts или фиксированного имени. Не переопределяй `PGDATA`.
- Передаёт `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`. Без trust-аутентификации.
- В healthcheck ровно два элемента test: `CMD-SHELL` и строка с `pg_isready -h db`, `-U` из переменной контейнера `POSTGRES_USER`, `-d` из `POSTGRES_DB`. Используй `$$` для передачи `$` оболочке.
- Явно задаёт `interval` и `timeout` целым числом секунд от 1 до 5 включительно, `retries` от 1 до 10 включительно, `start_period` целым числом секунд от 0 до 15 включительно. Эти небольшие интервалы подходят нашему учебному запуску и ограничивают время проверок.
- Healthcheck включён. У db нет собственных зависимостей.

Пока не задавай restart policy ни одному сервису. Не монтируй файлы приложения с компьютера.

Оба сервиса используют **одни входные переменные Compose**:

| Источник для Compose | По умолчанию при отсутствии или пустой строке | В контейнере db | В контейнере api |
|---|---|---|---|
| BANK_DB_NAME | bank_lab | POSTGRES_DB | имя базы в JDBC URL |
| BANK_DB_USER | bank_app | POSTGRES_USER | SPRING_DATASOURCE_USERNAME |
| BANK_DB_PASSWORD | learning-only | POSTGRES_PASSWORD | SPRING_DATASOURCE_PASSWORD |

URL API: `jdbc:postgresql://db:5432/<имя базы>`. `db` — DNS-имя сервиса внутри сети. Значения environment одного сервиса не являются источником подстановки другого сервиса.

### Ошибка готовности

Если проверка db постоянно возвращает ненулевой код, Docker должен показать `unhealthy`, а Compose — отказать в запуске остановленного API из-за зависимости. После восстановления правильной проверки база становится healthy, обычный запуск API снова работает; volume и данные сохраняются.

Тесты имитируют это отдельным override-файлом только в своём временном проекте. Писать такую имитацию в своём решении не нужно.

## Ручная проверка

Сначала создай код, ресурсы и конфигурацию. Собери JAR:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" -Dmaven.test.skip=true package
```

Здесь пропуск тестов нужен, чтобы сначала получить JAR для ручной сборки образа. Задача засчитывается только после полного `clean test` ниже.

Из корня проекта:

```shell
docker compose -p bank-ready-lab up -d --build
docker compose -p bank-ready-lab ps
```

Первая команда сама запускает db, ждёт `healthy`, затем запускает api. Не запускай db отдельно и не используй `--no-deps` в этом сценарии. `up -d` ещё не гарантирует готовность HTTP-сервера API; посмотри его логи и проверь ответ:

```shell
docker compose -p bank-ready-lab logs api
curl -i http://127.0.0.1:8085/bank/payments/count
```

Если порт 8085 занят прежней практикой, используй другой порт с тем же значением во всех командах либо `.env`:

```shell
BANK_HTTP_PORT=8086 docker compose -p bank-ready-lab up -d --build
curl -i http://127.0.0.1:8086/bank/payments/count
```

Добавь две строки, используя значения логина и базы из своего окружения:

```shell
docker compose -p bank-ready-lab exec db psql -U bank_app -d bank_lab -c "INSERT INTO payment_requests(description) VALUES ('Rent'), ('Travel');"
curl -i http://127.0.0.1:8085/bank/payments/count
docker compose -p bank-ready-lab down
docker compose -p bank-ready-lab up -d --no-build
```

После последнего запуска данные остаются. В командах после смены порта снова передавай выбранный `BANK_HTTP_PORT`; адрес curl также меняется. Внешний файл можно выбирать через `docker compose --env-file <путь> -p bank-ready-lab ...`.

Просмотреть здоровье базы:

```shell
docker inspect --format '{{json .State.Health}}' "$(docker compose -p bank-ready-lab ps -q db)"
```

Внешняя команда получает ID контейнера через `compose ps -q db`, затем `inspect` показывает статус и последние результаты проверок. Для штатного завершения используй `down`; `down --volumes` удаляет учебные данные, поэтому применяй его только при намеренном сбросе собственной базы.

## Файлы и проверка

Создаёшь самостоятельно:

- `src/main/java/learning/task094/`: стартовый класс, HTTP-контроллер, DTO и доступ к данным; имена и разделение выбираешь сам, каждый самостоятельный класс в отдельном файле.
- `src/main/resources/application.properties` и SQL-миграцию по указанному пути.
- `Dockerfile`, `.dockerignore`, `compose.yaml`; зависимости приложения и Maven repackage.

Тесты тренера:

- `src/test/java/learning/task094/ReadyPostgresComposeApiTest.java`
- `src/test/java/learning/task094/ComposeCommands.java`

15 тестов проверяют контракт API и схемы, настройки по умолчанию/пустые значения/файл окружения, реальную сеть и volume, сохранность данных/Flyway/identity, изоляцию проектов, запуск обоих сервисов одной командой, успешный TCP-healthcheck и отказ в запуске API при unhealthy с восстановлением без потери строк.

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Нужен Docker Engine и доступ процесса к docker.sock. Тесты собирают образ один раз, используют своё имя проекта и свободный порт, удаляют только свои ресурсы. Логи команд: `target/task094-checks`. При первой загрузке образов проверка может длиться дольше.

Критерий готовности: все 15 тестов прошли; первый запуск с пустым volume требует одной команды Compose и не ручного ожидания db; при unhealthy API не запускается; данные сохраняются. Решение ученика прошло все проверки.

Материал: [порядок запуска Compose](https://docs.docker.com/compose/how-tos/startup-order/), [параметры healthcheck](https://docs.docker.com/reference/compose-file/services/#healthcheck), [pg_isready PostgreSQL 17](https://www.postgresql.org/docs/17/app-pg-isready.html).

Проверка тестов тренера: все 15 тестов прошли 10.10.2026 на отдельном временном приложении через Docker Engine; полный запуск — около 67 секунд. Учебный пример с файлом также проверен в контейнерах. Временное приложение удалено; решение №94 остаётся за учеником. Начальная проверка рабочего проекта ожидаемо падает из-за отсутствия compose.yaml.

Проверено 11.10.2026: все 15 тестов прошли через Docker Engine, без ошибок и пропусков; полный clean test — около 63 секунд. Задача засчитана.
