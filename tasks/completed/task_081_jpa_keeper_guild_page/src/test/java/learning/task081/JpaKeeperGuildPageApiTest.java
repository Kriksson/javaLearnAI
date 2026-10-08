package learning.task081;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.repository.Repository;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.hibernate.SessionFactory;
import jakarta.persistence.Column;
import java.lang.reflect.AnnotatedElement;
import java.util.stream.StreamSupport;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Map;
import java.util.Arrays;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.repository.query.parser.Part;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.json.JsonCompareMode;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:task081-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
        "spring.jpa.properties.hibernate.cache.use_query_cache=false",
        "spring.jpa.properties.hibernate.default_batch_fetch_size=0"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaKeeperGuildPageApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeAll
    void startsWithEmptyTablesAndRequiredJpaRelation() throws Exception {
        assertNotNull(dataSource, "Настрой H2.");
        assertNotNull(jdbc, "Настрой тестовый источник данных.");
        assertNotNull(entityManagerFactory, "Добавь Spring Data JPA и настрой Hibernate.");
        Properties settings = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(input, "Создай src/main/resources/application.properties.");
            settings.load(input);
        }
        assertTrue(settings.getProperty("spring.datasource.url", "").startsWith("jdbc:h2:mem:"));
        assertEquals("sa", settings.getProperty("spring.datasource.username"));
        assertEquals("", settings.getProperty("spring.datasource.password"));
        assertEquals("create-drop", settings.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", settings.getProperty("spring.jpa.open-in-view"));
        assertEquals("create-drop", applicationContext.getEnvironment()
                .getProperty("spring.jpa.hibernate.ddl-auto"));
        assertTrue(entityManagerFactory.getMetamodel().getEntities().stream()
                        .filter(entity -> entity.getJavaType().getPackageName().startsWith("learning.task081"))
                        .count() >= 2,
                "Создай отдельные JPA-сущности гильдии и хранителя.");
        boolean hasManyToOne = entityManagerFactory.getMetamodel().getEntities().stream()
                .flatMap(entity -> entity.getAttributes().stream())
                .anyMatch(attribute -> attribute.getPersistentAttributeType()
                        == Attribute.PersistentAttributeType.MANY_TO_ONE);
        assertTrue(hasManyToOne, "Свяжи хранителя с гильдией через @ManyToOne.");
        var keeperType = entityManagerFactory.getMetamodel().getEntities().stream()
                .filter(entity -> entity.getJavaType().getPackageName().startsWith("learning.task081"))
                .filter(entity -> entity.getJavaType().isAnnotationPresent(Table.class))
                .filter(entity -> entity.getJavaType().getAnnotation(Table.class).name().equals("guild_keepers"))
                .findFirst().orElseThrow();
        var idName = keeperType.getSingularAttributes().stream()
                .filter(attribute -> attribute.isId()).findFirst().orElseThrow().getName();
        // Inspect each repository factory separately: more than one repository may manage the same entity.
        // Repositories.getRepositoryInformationFor(domainType) selects only one of them.
        var keeperRepositories = Arrays.stream(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                        applicationContext, RepositoryFactoryInformation.class, false, false))
                .map(beanName -> applicationContext.getBean(beanName, RepositoryFactoryInformation.class)
                        .getRepositoryInformation())
                .filter(information -> information.getDomainType().equals(keeperType.getJavaType()))
                .toList();
        assertFalse(keeperRepositories.isEmpty(), "Создай Spring Data JPA репозиторий для сущности хранителя.");
        var levelName = keeperType.getSingularAttributes().stream()
                .filter(attribute -> attribute.getJavaMember() instanceof AnnotatedElement member
                        && member.isAnnotationPresent(Column.class)
                        && member.getAnnotation(Column.class).name().equals("level"))
                .findFirst().orElseThrow(() -> new AssertionError("Явно отобрази поле уровня на колонку level."))
                .getName();
        var guildAttribute = keeperType.getSingularAttributes().stream()
                .filter(attribute -> attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE)
                .findFirst().orElseThrow();
        assertTrue(guildAttribute.getJavaMember() instanceof AnnotatedElement,
                "Связь должна быть явно аннотирована.");
        var relationAnnotation = ((AnnotatedElement) guildAttribute.getJavaMember()).getAnnotation(ManyToOne.class);
        assertNotNull(relationAnnotation);
        assertEquals(FetchType.LAZY, relationAnnotation.fetch(), "Задай @ManyToOne(fetch = FetchType.LAZY).");
        boolean hasGraphPageQuery = keeperRepositories.stream()
                .flatMap(information -> StreamSupport.stream(information.getQueryMethods().spliterator(), false))
                .filter(method -> !method.isAnnotationPresent(Query.class))
                .filter(method -> Page.class.isAssignableFrom(method.getReturnType()))
                .filter(method -> Arrays.stream(method.getParameterTypes()).anyMatch(Pageable.class::isAssignableFrom))
                .anyMatch(method -> {
                    var graph = method.getAnnotation(EntityGraph.class);
                    if (graph == null || !Arrays.asList(graph.attributePaths()).contains(guildAttribute.getName())) return false;
                    var tree = new PartTree(method.getName(), keeperType.getJavaType());
                    var parts = tree.getParts().toList();
                    var orders = tree.getSort().toList();
                    return parts.size() == 1
                            && parts.getFirst().getType() == Part.Type.GREATER_THAN_EQUAL
                            && parts.getFirst().getProperty().toDotPath().equals(levelName)
                            && orders.size() == 2
                            && orders.get(0).getProperty().equals(levelName) && orders.get(0).isDescending()
                            && orders.get(1).getProperty().equals(idName) && orders.get(1).isAscending();
                });
        assertTrue(hasGraphPageQuery,
                "Объяви производный метод: уровень >= порога, уровень DESC / ID ASC, Pageable и Page; добавь @EntityGraph(attributePaths = имя Java-поля гильдии).");
        assertTrue(entityManagerFactory.getMetamodel().getEntities().stream()
                        .filter(entity -> entity.getJavaType().getPackageName().startsWith("learning.task081"))
                        .flatMap(entity -> entity.getAttributes().stream())
                        .noneMatch(attribute -> attribute.getPersistentAttributeType()
                                == Attribute.PersistentAttributeType.ONE_TO_MANY),
                "В этой задаче не добавляй обратную коллекцию @OneToMany.");
        assertFalse(applicationContext.getBeansOfType(Repository.class).isEmpty(),
                "Создай Spring Data JPA репозиторий.");
        assertFalse(applicationContext.getBeansWithAnnotation(RestController.class).isEmpty(),
                "Создай REST-контроллер.");
        assertEquals(List.of(), storedKeepers());
        assertEquals(List.of(), storedGuilds());

        try (Connection connection = dataSource.getConnection()) {
            assertTableColumns(connection, "SELECT id, name FROM guilds WHERE 1 = 0",
                    new int[]{Types.BIGINT, Types.VARCHAR}, new String[]{"id", "name"},
                    new int[]{0, 40});
            assertTableColumns(connection, "SELECT id, name, level, guild_id FROM guild_keepers WHERE 1 = 0",
                    new int[]{Types.BIGINT, Types.VARCHAR, Types.INTEGER, Types.BIGINT},
                    new String[]{"id", "name", "level", "guild_id"}, new int[]{0, 40, 0, 0});
            assertIdentityPrimaryKey(connection, "GUILDS");
            assertIdentityPrimaryKey(connection, "GUILD_KEEPERS");
            assertForeignKey(connection);
        }
    }

    @BeforeEach
    void clearsRows() {
        jdbc.update("DELETE FROM guild_keepers");
        jdbc.update("DELETE FROM guilds");
    }

    @Test
    void returnsSortedPagesAcrossGuildsWithCorrectTotals() throws Exception {
        long[] guilds = new long[7];
        for (int i = 0; i < guilds.length; i++) guilds[i] = insertGuild("Guild " + i);
        Object[][] rows = {{50L, "High", 9, guilds[2]}, {40L, "Later", 8, guilds[1]},
                {20L, "Earlier", 8, guilds[0]}, {60L, "Next", 7, guilds[3]},
                {10L, "Threshold", 5, guilds[4]}, {70L, "Last", 5, guilds[5]}, {30L, "Below", 4, guilds[6]}};
        for (Object[] row : rows) jdbc.update("INSERT INTO guild_keepers (id, name, level, guild_id) VALUES (?, ?, ?, ?)", row);
        assertGet("5", "0", "5", 200, pageJson(0, 5, 6, 2,
                50L, "High", 9, guilds[2], "Guild 2", 20L, "Earlier", 8, guilds[0], "Guild 0",
                40L, "Later", 8, guilds[1], "Guild 1", 60L, "Next", 7, guilds[3], "Guild 3",
                10L, "Threshold", 5, guilds[4], "Guild 4"));
        assertGet("5", "1", "5", 200, pageJson(1, 5, 6, 2, 70L, "Last", 5, guilds[5], "Guild 5"));
        assertGet("5", "2", "5", 200, pageJson(2, 5, 6, 2));
    }

    @Test
    void fetchingFiveDifferentGuildsDoesNotAddPerGuildQueries() throws Exception {
        List<Object> expected = new ArrayList<>();
        long firstId = 0;
        long firstGuild = 0;
        for (int i = 0; i < 6; i++) {
            long guild = insertGuild("Guild " + i);
            long keeper = insertKeeper("Keeper " + i, 20 - i, guild);
            if (i == 0) {firstId = keeper; firstGuild = guild;}
            if (i < 5) expected.addAll(List.of(keeper, "Keeper " + i, 20 - i, guild, "Guild " + i));
        }
        assertGet("0", "0", "1", 200, pageJson(0, 1, 6, 6, firstId, "Keeper 0", 20, firstGuild, "Guild 0"));
        assertGet("0", "0", "5", 200, pageJson(0, 5, 6, 2, expected.toArray()));
        assertGet("0", "0", "5", 200, pageJson(0, 5, 6, 2, expected.toArray()));
    }

    @Test
    void severalKeepersOfSameGuildRemainSeparateItems() throws Exception {
        long guild = insertGuild("Shared");
        long first = insertKeeper("First", 7, guild);
        long second = insertKeeper("Second", 7, guild);
        long third = insertKeeper("Third", 7, guild);
        assertGet("7", "0", "2", 200, pageJson(0, 2, 3, 2,
                first, "First", 7, guild, "Shared", second, "Second", 7, guild, "Shared"));
        assertGet("7", "1", "2", 200, pageJson(1, 2, 3, 2, third, "Third", 7, guild, "Shared"));
    }

    @Test
    void preservesBigIdsBoundaryLevelsAndNamesInBothDtos() throws Exception {
        long guild = 3_000_000_000L;
        String guildName = "Г".repeat(40);
        jdbc.update("INSERT INTO guilds (id, name) VALUES (?, ?)", guild, guildName);
        jdbc.update("INSERT INTO guild_keepers (id, name, level, guild_id) VALUES (?, ?, ?, ?)",
                3_000_000_001L, "N".repeat(40), Integer.MAX_VALUE, guild);
        String name = "Рин \"Лис\"";
        jdbc.update("INSERT INTO guild_keepers (id, name, level, guild_id) VALUES (?, ?, ?, ?)",
                3_000_000_002L, name, 0, guild);
        assertGet("0", "0", "2", 200, pageJson(0, 2, 2, 1,
                3_000_000_001L, "N".repeat(40), Integer.MAX_VALUE, guild, guildName,
                3_000_000_002L, name, 0, guild, guildName));
        assertGet(String.valueOf(Integer.MAX_VALUE), "0", "1", 200, pageJson(0, 1, 1, 1,
                3_000_000_001L, "N".repeat(40), Integer.MAX_VALUE, guild, guildName));
    }

    @Test
    void emptyDatabaseNoMatchesAndPageBeyondLastHaveCorrectTotals() throws Exception {
        assertGet("0", "0", "2", 200, pageJson(0, 2, 0, 0));
        long guild = insertGuild("Empty");
        assertGet("0", "1000", "5", 200, pageJson(1000, 5, 0, 0));
        insertKeeper("Mira", 3, guild);
        assertGet("4", "0", "2", 200, pageJson(0, 2, 0, 0));
        assertGet("0", "1000", "5", 200, pageJson(1000, 5, 1, 1));
    }

    @Test
    void readsCurrentGuildNamesMembershipAndKeeperValues() throws Exception {
        long first = insertGuild("First");
        long second = insertGuild("Second");
        long keeper = insertKeeper("Before", 3, first);
        assertGet("0", "0", "2", 200, pageJson(0, 2, 1, 1, keeper, "Before", 3, first, "First"));
        jdbc.update("UPDATE guilds SET name = ? WHERE id = ?", "Новая \"Гильдия\"", first);
        assertGet("0", "0", "2", 200, pageJson(0, 2, 1, 1, keeper, "Before", 3, first, "Новая \"Гильдия\""));
        jdbc.update("UPDATE guild_keepers SET guild_id = ?, name = ?, level = ? WHERE id = ?", second, "After", 10, keeper);
        assertGet("10", "0", "2", 200, pageJson(0, 2, 1, 1, keeper, "After", 10, second, "Second"));
        jdbc.update("UPDATE guild_keepers SET level = ? WHERE id = ?", 0, keeper);
        assertGet("1", "0", "2", 200, pageJson(0, 2, 0, 0));
    }

    @Test
    void rejectsInvalidParametersWithoutChangingRows() throws Exception {
        long guild = insertGuild("Existing");
        insertKeeper("Mira", 3, guild);
        String[][] invalidValues = {{null, "0", "2"}, {"0", null, "2"}, {"0", "0", null},
                {"-1", "0", "2"}, {"0", "-1", "2"}, {"0", "1001", "2"},
                {"0", "0", "0"}, {"0", "0", "-1"}, {"0", "0", "6"}};
        for (String[] values : invalidValues) assertGet(values[0], values[1], values[2], 400, null);
        for (String invalid : new String[]{"", "   ", "abc", "2.5", "2147483648", "-2147483649"}) {
            assertGet(invalid, "0", "2", 400, null);
            assertGet("0", invalid, "2", 400, null);
            assertGet("0", "0", invalid, 400, null);
        }
    }

    @Test
    void databaseRejectsMissingAndNullGuildReferences() {
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO guild_keepers (name, level, guild_id) VALUES (?, ?, ?)", "Orphan", 1, 999L));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO guild_keepers (name, level, guild_id) VALUES (?, ?, ?)", "Orphan", 1, null));
        assertEquals(List.of(), storedKeepers());
        assertEquals(List.of(), storedGuilds());
    }

    private void assertGet(String minimumLevel, String page, String size,
                           int expectedStatus, String expectedJson) throws Exception {
        List<List<Object>> keepersBefore = storedKeepers();
        List<List<Object>> guildsBefore = storedGuilds();
        var request = get("/keepers").accept(MediaType.APPLICATION_JSON);
        if (minimumLevel != null) request.param("minLevel", minimumLevel);
        if (page != null) request.param("page", page);
        if (size != null) request.param("size", size);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        assertTrue(statistics.isStatisticsEnabled());
        statistics.clear();
        var response = mockMvc.perform(request).andExpect(status().is(expectedStatus));
        long statements = statistics.getPrepareStatementCount();
        if (expectedJson != null) {
            response.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(expectedJson, JsonCompareMode.STRICT));
            assertTrue(statements >= 1 && statements <= 2,
                    "Успешный GET должен выполнять 1–2 SQL-запроса: страница с гильдиями и, при необходимости, общий подсчёт. Фактически: " + statements);
        }
        assertEquals(keepersBefore, storedKeepers(), "GET не должен изменять хранителей.");
        assertEquals(guildsBefore, storedGuilds(), "GET не должен изменять гильдии.");
    }

    private String pageJson(int page, int size, long total, int totalPages, Object... values) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < values.length; i += 5) {
            rows.add(Map.of("id", values[i], "name", values[i + 1], "level", values[i + 2],
                    "guild", Map.of("id", values[i + 3], "name", values[i + 4])));
        }
        return new ObjectMapper().writeValueAsString(Map.of("content", rows, "page", page,
                "size", size, "totalElements", total, "totalPages", totalPages));
    }

    private void assertTableColumns(Connection connection, String sql,
                                    int[] types, String[] names, int[] precisions) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            ResultSetMetaData columns = rows.getMetaData();
            for (int i = 0; i < names.length; i++) {
                assertEquals(types[i], columns.getColumnType(i + 1));
                assertEquals(names[i], columns.getColumnName(i + 1).toLowerCase(Locale.ROOT));
                if (precisions[i] != 0) {
                    assertEquals(precisions[i], columns.getPrecision(i + 1));
                }
                assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(i + 1));
            }
        }
    }

    private void assertForeignKey(Connection connection) throws Exception {
        List<String> references = new ArrayList<>();
        try (ResultSet keys = connection.getMetaData().getImportedKeys(
                connection.getCatalog(), null, "GUILD_KEEPERS")) {
            while (keys.next()) {
                references.add(keys.getString("FKCOLUMN_NAME").toLowerCase(Locale.ROOT) + ":"
                        + keys.getString("PKTABLE_NAME").toLowerCase(Locale.ROOT) + "."
                        + keys.getString("PKCOLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        assertTrue(references.contains("guild_id:guilds.id"),
                "Колонка guild_id должна быть внешним ключом на guilds.id.");
    }

    private void assertIdentityPrimaryKey(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id FROM " + table + " WHERE 1 = 0")) {
            assertTrue(rows.getMetaData().isAutoIncrement(1), table + ".id должна быть identity.");
        }
        List<String> keys = new ArrayList<>();
        try (ResultSet primaryKeys = connection.getMetaData().getPrimaryKeys(
                connection.getCatalog(), null, table)) {
            while (primaryKeys.next()) {
                keys.add(primaryKeys.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        assertEquals(List.of("id"), keys, table + ".id должна быть первичным ключом.");
    }

    private long insertGuild(String name) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO guilds (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            assertEquals(1, statement.executeUpdate());
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    private long insertKeeper(String name, int level, long guildId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO guild_keepers (name, level, guild_id) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setInt(2, level);
            statement.setLong(3, guildId);
            assertEquals(1, statement.executeUpdate());
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    private List<List<Object>> storedKeepers() {
        return jdbc.query("SELECT id, name, level, guild_id FROM guild_keepers ORDER BY id",
                (rows, rowNumber) -> List.of(rows.getLong("id"), rows.getString("name"),
                        rows.getInt("level"), rows.getLong("guild_id")));
    }

    private List<List<Object>> storedGuilds() {
        return jdbc.query("SELECT id, name FROM guilds ORDER BY id",
                (rows, rowNumber) -> List.of(rows.getLong("id"), rows.getString("name")));
    }
}
