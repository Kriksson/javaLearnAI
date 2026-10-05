package learning.task073;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:task073-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaGuildKeepersApiTest {
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
    void startsWithConfiguredEmptyJpaTable() throws Exception {
        assertNotNull(dataSource, "Настрой H2.");
        assertNotNull(jdbc, "Настрой источник данных для тестового окружения.");
        assertNotNull(entityManagerFactory, "Добавь spring-boot-starter-data-jpa и настрой Hibernate.");
        Properties settings = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(input, "Создай src/main/resources/application.properties.");
            settings.load(input);
        }
        assertTrue(settings.getProperty("spring.datasource.url", "").startsWith("jdbc:h2:mem:"),
                "Настрой отдельную in-memory базу H2 в application.properties.");
        assertEquals("create-drop", settings.getProperty("spring.jpa.hibernate.ddl-auto"),
                "Настрой создание таблиц Hibernate при старте и их удаление при остановке.");
        assertEquals("create-drop", applicationContext.getEnvironment()
                .getProperty("spring.jpa.hibernate.ddl-auto"));
        assertTrue(entityManagerFactory.getMetamodel().getEntities().stream()
                        .map(entity -> entity.getJavaType())
                        .anyMatch(type -> type.getPackageName().startsWith("learning.task073")),
                "Создай JPA-сущность для этой задачи.");
        assertFalse(applicationContext.getBeansOfType(Repository.class).isEmpty(),
                "Создай Spring Data JPA репозиторий.");
        assertFalse(applicationContext.getBeansWithAnnotation(RestController.class).isEmpty(),
                "Создай REST-контроллер.");
        assertEquals(List.of(), storedRows(), "Таблица должна начинать работу пустой.");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT id, name, level FROM guild_keepers WHERE 1 = 0")) {
            ResultSetMetaData columns = rows.getMetaData();
            assertEquals(Types.BIGINT, columns.getColumnType(1));
            assertEquals("id", columns.getColumnName(1).toLowerCase(Locale.ROOT));
            assertTrue(columns.isAutoIncrement(1));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(1));
            assertEquals(Types.VARCHAR, columns.getColumnType(2));
            assertEquals("name", columns.getColumnName(2).toLowerCase(Locale.ROOT));
            assertEquals(40, columns.getPrecision(2));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(2));
            assertEquals(Types.INTEGER, columns.getColumnType(3));
            assertEquals("level", columns.getColumnName(3).toLowerCase(Locale.ROOT));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(3));
            List<String> primaryKeys = new ArrayList<>();
            try (ResultSet keys = connection.getMetaData().getPrimaryKeys(
                    connection.getCatalog(), null, "GUILD_KEEPERS")) {
                while (keys.next()) {
                    primaryKeys.add(keys.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            assertEquals(List.of("id"), primaryKeys);
        }
    }

    @BeforeEach
    void clearsRows() {
        jdbc.update("DELETE FROM guild_keepers");
    }

    @Test
    void returnsStoredKeeperAsJsonWithoutChangingRows() throws Exception {
        long id = insert("Mira", 4);
        insert("Doran", 7);
        List<List<Object>> before = storedRows();

        mockMvc.perform(get("/guild/keepers/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"id\":" + id + ",\"name\":\"Mira\",\"level\":4}", true));

        assertEquals(before, storedRows(), "GET не должен менять таблицу.");
    }

    @Test
    void returnsNotFoundWhenTableIsEmpty() throws Exception {
        mockMvc.perform(get("/guild/keepers/1"))
                .andExpect(status().isNotFound());
        assertEquals(List.of(), storedRows());
    }

    @Test
    void returnsNotFoundForUnknownAndMaximumLongIds() throws Exception {
        long existingId = insert("Mira", 4);
        List<List<Object>> before = storedRows();

        mockMvc.perform(get("/guild/keepers/99"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/guild/keepers/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());

        assertEquals(List.of(List.of(existingId, "Mira", 4)), before);
        assertEquals(before, storedRows());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "9223372036854775808", "-9223372036854775809"})
    void returnsBadRequestWhenPathIdCannotBeConvertedToLong(String pathId) throws Exception {
        List<List<Object>> before = storedRows();

        mockMvc.perform(get("/guild/keepers/{id}", pathId))
                .andExpect(status().isBadRequest());

        assertEquals(before, storedRows());
    }

    private long insert(String name, int level) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO guild_keepers (name, level) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, name);
            insert.setInt(2, level);
            assertEquals(1, insert.executeUpdate());
            try (ResultSet keys = insert.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    private List<List<Object>> storedRows() {
        return jdbc.query("SELECT id, name, level FROM guild_keepers ORDER BY id",
                (rows, rowNumber) -> List.of(rows.getLong("id"),
                        rows.getString("name"), rows.getInt("level")));
    }
}
