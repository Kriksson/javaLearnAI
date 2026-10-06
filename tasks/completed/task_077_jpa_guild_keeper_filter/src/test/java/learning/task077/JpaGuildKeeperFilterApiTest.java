package learning.task077;

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
import java.util.stream.StreamSupport;
import org.springframework.http.MediaType;
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
        "spring.datasource.url=jdbc:h2:mem:task077-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaGuildKeeperFilterApiTest {
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
        assertEquals("create-drop", applicationContext.getEnvironment()
                .getProperty("spring.jpa.hibernate.ddl-auto"));
        assertTrue(entityManagerFactory.getMetamodel().getEntities().stream()
                        .filter(entity -> entity.getJavaType().getPackageName().startsWith("learning.task077"))
                        .count() >= 2,
                "Создай отдельные JPA-сущности гильдии и хранителя.");
        boolean hasManyToOne = entityManagerFactory.getMetamodel().getEntities().stream()
                .flatMap(entity -> entity.getAttributes().stream())
                .anyMatch(attribute -> attribute.getPersistentAttributeType()
                        == Attribute.PersistentAttributeType.MANY_TO_ONE);
        assertTrue(hasManyToOne, "Свяжи хранителя с гильдией через @ManyToOne.");
        var keeperType = entityManagerFactory.getMetamodel().getEntities().stream()
                .filter(entity -> entity.getJavaType().getPackageName().startsWith("learning.task077"))
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
        assertFalse(keeperRepositories.isEmpty(), "Создай JpaRepository для сущности хранителя.");
        boolean hasDerivedRelationQuery = keeperRepositories.stream()
                .flatMap(information -> StreamSupport.stream(information.getQueryMethods().spliterator(), false))
                .filter(method -> !method.isAnnotationPresent(Query.class))
                .anyMatch(method -> {
                    try {
                        var tree = new PartTree(method.getName(), keeperType.getJavaType());
                        var order = tree.getSort().getOrderFor(idName);
                        return tree.getParts().toList().size() == 3
                                && tree.getParts().stream().allMatch(part -> part.getType() == Part.Type.SIMPLE_PROPERTY)
                                && order != null && order.isAscending()
                                && tree.getParts().stream().anyMatch(part -> {
                                    var path = part.getProperty();
                                    return path.hasNext() && !path.next().hasNext()
                                            && keeperType.getAttribute(path.getSegment()).getPersistentAttributeType()
                                            == Attribute.PersistentAttributeType.MANY_TO_ONE;
                                });
                    } catch (RuntimeException exception) {
                        throw new AssertionError("Не удалось проверить производный метод "
                                + method.toGenericString(), exception);
                    }
                });
        assertTrue(hasDerivedRelationQuery,
                "Объяви производный метод репозитория: три условия равенства (гильдия, имя, уровень) и порядок по ID хранителя.");
        assertTrue(entityManagerFactory.getMetamodel().getEntities().stream()
                        .filter(entity -> entity.getJavaType().getPackageName().startsWith("learning.task077"))
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
    void combinesGuildNameAndLevelAndSortsByKeeperId() throws Exception {
        long guildId = 3_000_000_000L;
        jdbc.update("INSERT INTO guilds (id, name) VALUES (?, ?)", guildId, "G".repeat(40));
        long otherGuild = insertGuild("Other");
        String name = "K".repeat(40);
        jdbc.update("INSERT INTO guild_keepers (id, name, level, guild_id) VALUES (?, ?, ?, ?)",
                3_000_000_003L, name, Integer.MAX_VALUE, guildId);
        jdbc.update("INSERT INTO guild_keepers (id, name, level, guild_id) VALUES (?, ?, ?, ?)",
                3_000_000_001L, name, Integer.MAX_VALUE, guildId);
        insertKeeper("Other name", Integer.MAX_VALUE, guildId);
        insertKeeper(name, 0, guildId);
        insertKeeper(name.toLowerCase(Locale.ROOT), Integer.MAX_VALUE, guildId);
        insertKeeper(name, Integer.MAX_VALUE, otherGuild);
        assertGetPreservesRows(guildId, name, String.valueOf(Integer.MAX_VALUE), 200,
                keeperJson(3_000_000_001L, name, Integer.MAX_VALUE,
                        3_000_000_003L, name, Integer.MAX_VALUE));
    }

    @Test
    void returnsEmptyArrayForExistingGuildWithoutMatches() throws Exception {
        long guildId = insertGuild("First");
        long otherGuild = insertGuild("Other");
        long emptyGuild = insertGuild("Empty");
        insertKeeper("Mira", 2, guildId);
        insertKeeper("Mira", 3, otherGuild);
        assertGetPreservesRows(guildId, "Mira", "3", 200, "[]");
        assertGetPreservesRows(guildId, "Other", "2", 200, "[]");
        assertGetPreservesRows(emptyGuild, "Mira", "3", 200, "[]");
    }

    @Test
    void returnsNotFoundForUnknownGuildWithOrWithoutOtherRows() throws Exception {
        assertGetPreservesRows(9_000_000_000L, "Mira", "3", 404, null);
        long guildId = insertGuild("Existing");
        insertKeeper("Mira", 3, guildId);
        assertGetPreservesRows(9_000_000_000L, "Mira", "3", 404, null);
    }

    @Test
    void readsCurrentMembershipNameAndLevelAfterDatabaseChanges() throws Exception {
        long firstGuild = insertGuild("First");
        long secondGuild = insertGuild("Second");
        long keeperId = insertKeeper("Before", 0, firstGuild);
        assertGetPreservesRows(firstGuild, "Before", "0", 200, keeperJson(keeperId, "Before", 0));
        String changedName = "Рин \"Лис\"";
        jdbc.update("UPDATE guild_keepers SET guild_id = ?, name = ?, level = ? WHERE id = ?",
                secondGuild, changedName, 9, keeperId);
        assertGetPreservesRows(firstGuild, "Before", "0", 200, "[]");
        assertGetPreservesRows(secondGuild, "Before", "0", 200, "[]");
        assertGetPreservesRows(secondGuild, changedName, "9", 200, keeperJson(keeperId, changedName, 9));
    }

    @Test
    void rejectsInvalidFiltersWithoutChangingRows() throws Exception {
        long guildId = insertGuild("Existing");
        insertKeeper("Mira", 3, guildId);
        String[][] invalidFilters = {
                {null, "3"}, {"Mira", null}, {"", "3"}, {"   ", "3"},
                {"N".repeat(41), "3"}, {"Mira", "-1"},
                {"Mira", "abc"}, {"Mira", "2147483648"}
        };
        for (String[] filter : invalidFilters) {
            assertGetPreservesRows(guildId, filter[0], filter[1], 400, null);
        }
    }

    @Test
    void databaseRejectsMissingAndNullGuildReferences() {
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO guild_keepers (name, level, guild_id) VALUES (?, ?, ?)",
                        "Orphan", 1, 999L));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO guild_keepers (name, level, guild_id) VALUES (?, ?, ?)",
                        "Orphan", 1, null));
        assertEquals(List.of(), storedKeepers());
        assertEquals(List.of(), storedGuilds());
    }

    private void assertGetPreservesRows(long guildId, String name, String level,
                                       int expectedStatus, String expectedJson) throws Exception {
        List<List<Object>> keepersBefore = storedKeepers();
        List<List<Object>> guildsBefore = storedGuilds();
        var request = get("/guilds/{guildId}/keepers", guildId).accept(MediaType.APPLICATION_JSON);
        if (name != null) request.param("name", name);
        if (level != null) request.param("level", level);
        var response = mockMvc.perform(request).andExpect(status().is(expectedStatus));
        if (expectedJson != null) {
            response.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(expectedJson, JsonCompareMode.STRICT));
        }
        assertEquals(keepersBefore, storedKeepers(), "GET не должен изменять хранителей.");
        assertEquals(guildsBefore, storedGuilds(), "GET не должен изменять гильдии.");
    }

    private String keeperJson(Object... values) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < values.length; i += 3) {
            rows.add(Map.of("id", values[i], "name", values[i + 1], "level", values[i + 2]));
        }
        return new ObjectMapper().writeValueAsString(rows);
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
