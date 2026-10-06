package learning.task075;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:task075-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaKeeperGuildRelationApiTest {
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
                        .filter(entity -> entity.getJavaType().getPackageName().startsWith("learning.task075"))
                        .count() >= 2,
                "Создай отдельные JPA-сущности гильдии и хранителя.");
        boolean hasManyToOne = entityManagerFactory.getMetamodel().getEntities().stream()
                .flatMap(entity -> entity.getAttributes().stream())
                .anyMatch(attribute -> attribute.getPersistentAttributeType()
                        == Attribute.PersistentAttributeType.MANY_TO_ONE);
        assertTrue(hasManyToOne, "Свяжи хранителя с гильдией через @ManyToOne.");
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
    void returnsKeeperWithItsGuildAndPreservesTheRows() throws Exception {
        String guildName = "Silver Hawks";
        String keeperName = "K".repeat(40);
        long guildId = insertGuild(guildName);
        long keeperId = insertKeeper(keeperName, Integer.MAX_VALUE, guildId);
        List<List<Object>> keepersBefore = storedKeepers();
        List<List<Object>> guildsBefore = storedGuilds();

        mockMvc.perform(get("/guild/keepers/{id}", keeperId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":" + keeperId
                        + ",\"name\":\"" + keeperName + "\",\"level\":" + Integer.MAX_VALUE
                        + ",\"guild\":{\"id\":" + guildId + ",\"name\":\"" + guildName + "\"}}"));

        assertEquals(keepersBefore, storedKeepers());
        assertEquals(guildsBefore, storedGuilds());
    }

    @Test
    void returnsNotFoundForUnknownKeeper() throws Exception {
        mockMvc.perform(get("/guild/keepers/99"))
                .andExpect(status().isNotFound());
        assertEquals(List.of(), storedKeepers());
        assertEquals(List.of(), storedGuilds());
    }

    @Test
    void databaseRejectsKeeperThatReferencesUnknownGuild() {
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO guild_keepers (name, level, guild_id) VALUES (?, ?, ?)",
                        "Orphan", 1, 999L));
        assertEquals(List.of(), storedKeepers());
        assertEquals(List.of(), storedGuilds());
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
