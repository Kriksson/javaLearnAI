# Задача №95 — Банк: сборка приложения внутри Docker

Сложность: базовая. Самостоятельно создаёшь приложение в `learning.task095`, Maven-настройки, миграцию и Docker/Compose-конфигурацию. Имена внутренних классов выбираешь сам, каждый самостоятельный класс — в отдельном файле. Можно сверяться с архивом №94.

Закрепляем REST API, JPA или JDBC, PostgreSQL, Flyway, окружение, volume и ожидание готовности базы. Новое понятие: **сборка Docker-образа в несколько этапов (multi-stage)**. Внутри него разберём различие среды сборки JDK и среды запуска JRE.

## Назначение

Раньше ты сначала запускал Maven на компьютере, получал `target/bank-service.jar`, затем Docker копировал этот JAR. Без готового JAR сборка образа не работала. Теперь образ должен собираться непосредственно из исходников: Docker запускает Maven внутри первого этапа, а во второй переносит готовое приложение.

JDK содержит средства запуска Java и компилятор `javac`. JRE содержит среду запуска, без компилятора. Компилятор нужен для сборки, а работающему API достаточно JRE.

Каждая инструкция `FROM` начинает отдельный этап с собственной файловой системой. Название после `AS` позволяет ссылаться на этап через `COPY --from=<имя>`. Файлы первого этапа не переходят во второй автоматически: нужно явно выбрать, что перенести.

## Работающий пример: Maven собирает JAR внутри Docker

Возьмём отдельное приложение, которое выводит сообщение об отчёте. Пример использует тот же путь сборки, что и задание: **pom.xml + Maven Wrapper → Maven внутри JDK-этапа → исполняемый JAR → JRE-этап → java -jar**. Предметная область простая, а механизм сборки показан полностью.

### 1. Подготовь отдельный каталог и Wrapper

Запусти из корня учебного проекта:

```shell
mkdir -p /tmp/report-maven-demo/src/main/java/demo
cp mvnw /tmp/report-maven-demo/mvnw
cp -R .mvn /tmp/report-maven-demo/
cd /tmp/report-maven-demo
```

`mvnw` и `.mvn/` вместе образуют Maven Wrapper. Скрипт читает адрес Maven из `.mvn/wrapper/maven-wrapper.properties` и при необходимости скачивает указанную версию. Поэтому копируем оба элемента. Установленный на компьютере Maven для сборки образа не нужен.

Создай в этом отдельном каталоге файлы:

```text
report-maven-demo/
  pom.xml
  mvnw
  .mvn/wrapper/maven-wrapper.properties
  .dockerignore
  Dockerfile
  src/main/java/demo/Report.java
```

### 2. Исходник приложения

`src/main/java/demo/Report.java`:

```java
package demo;

public class Report {
    public static void main(String[] args) {
        System.out.println("Report generated");
    }
}
```

Это обычный main: после запуска JAR увидим `Report generated`. Логики базы и HTTP здесь нет; мы изучаем сборку проекта через Maven.

### 3. pom.xml: что собирает Maven

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>demo</groupId>
    <artifactId>report-demo</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <build>
        <finalName>report-demo</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>3.5.16</version>
                <configuration>
                    <mainClass>demo.Report</mainClass>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

Что здесь важно:

- `maven.compiler.release=21` и compiler plugin задают компиляцию для Java 21.
- Maven по стандарту ищет исходники в `src/main/java`; этот путь не приходится прописывать вручную.
- `finalName=report-demo` задаёт имя результата: `target/report-demo.jar`.
- Используем тот же Spring Boot Maven plugin с `repackage`, который уже применялся в твоём API. В фазе `package` Maven создаёт JAR, затем плагин делает его исполняемым, добавляя загрузчик.
- `mainClass=demo.Report` указывает, какой main запускать. Здесь Spring-контекст не создаётся: плагин упаковывает приложение, а наш main просто выводит строку. В задании main запускает Spring Boot и HTTP-сервер.

### 4. .dockerignore: что передаём сборке

В корне примера создай `.dockerignore`:

```text
.git
.idea
.maven-home
target
.env
```

`target` исключён: Docker должен собрать JAR сам. `src`, `pom.xml`, `mvnw` и `.mvn` не исключаем — они нужны Maven внутри builder.

### 5. Dockerfile: два этапа

```dockerfile
FROM eclipse-temurin:21-jdk AS packager
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src
RUN bash ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /report
COPY --from=packager /workspace/target/report-demo.jar /report/report-demo.jar
ENTRYPOINT ["java", "-jar", "/report/report-demo.jar"]
```

Разбор каждой инструкции:

| Инструкция | Что происходит |
|---|---|
| Первый FROM | Выбирает JDK 21 с компилятором; называет этап packager |
| WORKDIR /workspace | Создаёт рабочий каталог внутри первого этапа и делает его текущим |
| COPY pom.xml mvnw ./ | Берёт два файла из контекста на компьютере и помещает в /workspace |
| COPY .mvn .mvn | Переносит настройки Wrapper в /workspace/.mvn |
| COPY src src | Переносит исходники в /workspace/src |
| RUN bash ./mvnw ... package | Запускает Maven внутри этапа; Maven читает pom и создаёт /workspace/target/report-demo.jar |
| Второй FROM | Начинает отдельный этап на JRE 21, без файлов первого этапа |
| WORKDIR /report | Задаёт рабочий каталог итогового образа |
| COPY --from=packager ... | Берёт готовый JAR из файловой системы первого этапа и переносит во второй |
| ENTRYPOINT [...] | При запуске контейнера выполняет java -jar для перенесённого JAR |

У `COPY` два разных источника: обычный COPY читает **контекст на компьютере**, а COPY с `--from=packager` читает **файловую систему этапа packager**. Поэтому для второй копии не нужен `target` на компьютере.

В команде Maven:

- `bash ./mvnw` запускает скрипт Wrapper без требования executable-бита.
- `-B` включает пакетный режим без интерактивных вопросов.
- `-Dmaven.test.skip=true` пропускает компиляцию и выполнение тестов внутри builder.
- `package` запускает сборку и упаковку, включая настроенный `repackage`.

`RUN` выполняется при **сборке образа**. `ENTRYPOINT` — при **запуске контейнера**. Maven не запускает сервер API в процессе упаковки.

### 6. Собери и запусти

Из `/tmp/report-maven-demo`:

```shell
docker build -t report-maven-demo:95 .
docker run --rm report-maven-demo:95
```

Перед Docker не выполняй Maven package на компьютере. Первая команда сама соберёт JAR в packager, перенесёт его в JRE и создаст итоговый образ. Вторая запустит main из JAR. Результат:

```text
Report generated
```

Сверка путей:

| Место | Что существует |
|---|---|
| Компьютер перед сборкой | pom.xml, Wrapper, src; готовый JAR не требуется |
| Этап packager после Maven | /workspace/target/report-demo.jar |
| Итоговый образ | /report/report-demo.jar и JRE; Maven и исходники первого этапа не перенесены |

Полный путь данных: исходники и pom с компьютера → COPY в packager → Wrapper запускает Maven → Maven компилирует Java и упаковывает JAR → COPY --from переносит JAR в JRE → java -jar запускает main → видим строку.

После примера можно удалить его образ: `docker image rm report-maven-demo:95`.

## Переход от примера к заданию

Ты применишь **тот же механизм** к банковскому Maven-проекту. Различия перечислены явно:

| В примере | В задании |
|---|---|
| Простая программа demo.Report | Твои классы Spring Boot в learning.task095 |
| finalName report-demo | finalName bank-service |
| WORKDIR builder /workspace | WORKDIR builder /build |
| /workspace/target/report-demo.jar | /build/target/bank-service.jar |
| JAR в /report | JAR в /app |
| Контейнер выводит строку и завершается | Контейнер запускает HTTP-сервер; Compose передаёт ему настройки PostgreSQL |

Порядок инструкций и назначение Maven-команды остаются такими же. Твои привычные зависимости API и настройки `repackage` сохраняют свою роль: Maven собирает код и ресурсы, Spring Boot plugin упаковывает зависимости, а Flyway запускается уже вместе с API при старте контейнера.

Внутри builder пропускаем тесты, потому что тесты задания сами запускают Docker: запуск их из Docker-сборки привёл бы к повторным проверкам внутри builder, где нет Docker Engine. Это не отменяет полный `clean test` на компьютере при проверке решения. `-DskipTests` пропускает только выполнение, а `-Dmaven.test.skip=true` — также компиляцию тестов.

Wrapper, Maven и зависимости впервые скачиваются через сеть **внутри этапа сборки**. Кеш `.maven-home` компьютера туда не передаётся. Поэтому первая сборка дольше; повторная может использовать подходящие Docker-слои. Дополнительное кеширование зависимостей пока не требуется.

## Контракт приложения

Сохраняем знакомый сценарий: банковский API возвращает число заявок на оплату.

- Публичный `GET /bank/payments/count`: `200`, `application/json`, единственное поле `count` с целым числом актуальных строк таблицы. Пустая таблица: `{"count":0}`; после двух вставок: `{"count":2}`; после удаления одной: `{"count":1}`.
- Данные читаются из PostgreSQL при запросе, без списка или счётчика в памяти приложения.
- Отсутствующий маршрут: `404`; `POST /bank/payments/count`: `405`.
- Flyway создаёт таблицу `payment_requests` через `src/main/resources/db/migration/V1__create_payment_requests.sql`, без стартовых строк: `id BIGINT identity PRIMARY KEY`, `description VARCHAR(100) NOT NULL`.
- Flyway — единственный создатель схемы. Если используешь JPA, укажи `ddl-auto=validate`, отключи Open Session in View и отобрази сущность на `payment_requests`. Конфигурацию JPA задавай в `application.properties` или стандартных переменных environment сервиса api.
- Данные, история успешной V1 и последовательность identity сохраняются при пересоздании db и `down/up` без удаления volumes; выданные ID не переиспользуются. Другое имя проекта — отдельная пустая база. Явное удаление volume — свежая база.

## Контракт сборки

В корне обязательны `pom.xml`, `mvnw`, `.mvn/wrapper/maven-wrapper.properties`, `Dockerfile`, `.dockerignore`, `compose.yaml`. Исходный код Java 21 и стартовый класс в `learning.task095`. Maven создаёт исполняемый Spring Boot JAR `target/bank-service.jar`.

Dockerfile содержит **ровно два этапа**:

1. Образ `eclipse-temurin:21-jdk`, именованный этап с выбранным тобой именем. Рабочий каталог `/build`. Копируешь настройки Maven, Wrapper, исходники и ресурсы; собираешь JAR через `bash ./mvnw -B -Dmaven.test.skip=true package`. Допустимы отдельные `COPY` для pom, Wrapper, .mvn и src либо более общее копирование контекста при корректном `.dockerignore`.
2. Образ `eclipse-temurin:21-jre`, рабочий каталог `/app`. Единственный `COPY` переносит **только** `/build/target/bank-service.jar` из первого именованного этапа в `/app/bank-service.jar`. В этом этапе без `RUN` и `ADD`. Exec-форма ENTRYPOINT: `java`, `-jar`, `/app/bank-service.jar`.

Для обоих образов допустим префикс `docker.io/library/`. Имя первого этапа свободное, например `compiler`; тесты читают выбранное имя, а не требуют фиксированное. Никаких обязательных `ARG`, внешних build args или доступа к кешу компьютера. Не устанавливай Maven/JDK в итоговый этап.

В runtime отсутствуют компилятор `javac`, команда `mvn`, каталог `/build`, Maven-кеш `/root/.m2` или `/root/.maven-home`, исходники, pom, Wrapper и локальные файлы проекта в `/app`. Библиотеки Spring и драйвер в исполняемом JAR нужны для работы и остаются в нём.

`.dockerignore` лежит **в корне контекста**, рядом с Dockerfile. Обязательные исключения: `.git`, `.idea`, `.maven-home`, `tasks`, `.env`, **`target`**. Не исключай `src`, `src/main`, `pom.xml`, `mvnw` и `.mvn`: они теперь нужны сборке. `.env` исключён также из Git.

Важно: в №94 исключали `src` и передавали готовый JAR. Здесь исключаем `target`, а исходники передаём builder. Сборка должна работать, даже если каталога `target` на компьютере нет.

## Контракт Compose

Сохраняем конфигурацию №94, которую создаёшь самостоятельно:

- Ровно `api` и `db`, стандартная сеть Compose; без собственной networks-конфигурации, container_name, network_mode, env_file сервисов и restart policy.
- api: build с контекстом `.` и Dockerfile, без image, volumes или healthcheck API. Единственный TCP-порт `127.0.0.1:${BANK_HTTP_PORT:-8085}:8080`; значение `0` позволяет выбрать свободный порт.
- api обязательно зависит только от db, с условием `service_healthy`. Обычный запуск обоих сервисов выполняется одной командой, включая пустой volume.
- db: `postgres:17-alpine` (допустим `docker.io/library/`), без build и опубликованных портов, без trust-аутентификации и PGDATA.
- Единственный writable volume `bank_data:/var/lib/postgresql/data`; верхнеуровневый volume локальный, без external, driver_opts и фиксированного общего имени.
- Healthcheck db: `CMD-SHELL` и строка `pg_isready` по TCP с `-h db`, `-U` из переменной контейнера POSTGRES_USER и `-d` из POSTGRES_DB. Передавай `$` через `$$`. Явные interval/timeout — целые 1–5 секунд, retries — 1–10, start_period — целые 0–15 секунд.
- При постоянно неуспешной проверке db становится unhealthy, а Compose отказывает в запуске остановленного API. После исправления проверки обычный запуск снова работает, данные остаются. Отдельный override для имитации ошибки создаёт тест, не ты.

| Источник Compose | Значение при отсутствии или пустой строке | db.environment | api.environment |
|---|---|---|---|
| BANK_DB_NAME | bank_lab | POSTGRES_DB | имя базы в SPRING_DATASOURCE_URL |
| BANK_DB_USER | bank_app | POSTGRES_USER | SPRING_DATASOURCE_USERNAME |
| BANK_DB_PASSWORD | learning-only | POSTGRES_PASSWORD | SPRING_DATASOURCE_PASSWORD |

URL API: `jdbc:postgresql://db:5432/<имя базы>`. Переменные одного сервиса не являются источником подстановки для другого.

## Ручная проверка

Создай код в `src/main/java/learning/task095/`, конфигурацию и миграцию в `src/main/resources/`, настрой зависимости и repackage в pom. Затем создай Dockerfile и Compose.

Для этой проверки **не запускай Maven package перед Docker**. Убедись, что `target` отсутствует или временно переименуй его, сохранив нужные тебе локальные файлы. Запуск из корня:

```shell
docker compose -p bank-build-lab up -d --build
docker compose -p bank-build-lab logs --tail 60 api
curl -i http://127.0.0.1:8085/bank/payments/count
```

Если 8085 занят, передавай `BANK_HTTP_PORT=8086` перед командами Compose или задай его в .env; curl использует выбранный порт. После изменения исходников `--build` теперь **сам компилирует приложение внутри Docker**.

При настройках по умолчанию добавь строку:

```shell
docker compose -p bank-build-lab exec db psql -U bank_app -d bank_lab -c "INSERT INTO payment_requests(description) VALUES ('Invoice');"
curl -i http://127.0.0.1:8085/bank/payments/count
```

Проверь среду запуска:

```shell
docker compose -p bank-build-lab exec api java -version
docker compose -p bank-build-lab exec api sh -c 'command -v javac; command -v mvn'
```

Первая команда показывает Java 21. Вторая не находит компилятор и Maven, завершается ненулевым кодом — это ожидаемый результат для JRE. Для завершения используй `docker compose -p bank-build-lab down`; volume остаётся.

## Автоматическая проверка

Тесты тренера:

- `src/test/java/learning/task095/MultiStageBankComposeApiTest.java`
- `src/test/java/learning/task095/ComposeCommands.java`

17 тестов проверяют API/SQL/Flyway, границы схемы, параметры окружения, сеть, volume, изоляцию проектов, healthcheck и unhealthy-восстановление; дополнительно сборку из исходников без host target/JAR/кеша и состав JRE-образа. Проверяется исполняемый Java 21 JAR, извлечённый из контейнера, а не заранее собранный на компьютере.

Для контроля пропуска компиляции тестов проверка помещает в своё временное дерево некорректный тестовый исходник. Правильный `-Dmaven.test.skip=true` позволяет собрать приложение. Рабочие файлы ученика при этом не меняются.

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Нужны Docker Engine, доступ к docker.sock и доступ Docker-сборки к Maven Central. Тесты собирают образ один раз, используют собственный проект и свободный порт, затем удаляют только свои ресурсы. Логи: `target/task095-checks`. Первая сборка медленнее из-за скачивания JDK/JRE, Wrapper и Maven-зависимостей. Контейнерные тесты запускаются на компьютере; Maven внутри builder используется только для упаковки приложения.

Критерий готовности: все 17 тестов прошли, Docker собирает приложение без готового JAR, итоговый образ использует Java 21 JRE и содержит только приложение; API и сохранность данных соответствуют контракту. Решение ученика прошло все проверки.

Материал: [сборка в несколько этапов](https://docs.docker.com/build/building/multi-stage/), [официальные образы Eclipse Temurin](https://hub.docker.com/_/eclipse-temurin).

Проверка материала тренера 11.10.2026: все 17 тестов прошли через Docker Engine на временном приложении, собранном из исходников без host JAR и Maven-кеша. Временное приложение удалено. Рабочая начальная проверка ожидаемо падает на отсутствии compose.yaml; решение №95 создаёт ученик.

Обновлённый пример Docker + Maven проверен 11.10.2026: сборка из приведённых pom.xml, исходника, Wrapper и Dockerfile без готового JAR прошла; контейнер вывел Report generated. В runtime проверено наличие JAR и отсутствие Maven, javac и каталога builder. Пример проверялся отдельно от решения ученика.

Проверено 12.10.2026: все 17 тестов прошли через Docker Engine, без ошибок и пропусков; полный clean test — около 73 секунд. Задача засчитана.
