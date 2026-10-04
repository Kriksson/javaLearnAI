package learning.task068;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:task068-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GuildRosterApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private DataSource dataSource;

    @BeforeAll
    void checksDataLoadedByStartupSqlScript() throws SQLException {
        assertNotNull(dataSource, "Подключи Spring JDBC и H2 и настрой DataSource.");
        assertSeedRows();
    }

    @BeforeEach
    void restoresInitialRows() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO guild_recruits (id, name, level) VALUES (?, ?, ?)")) {
            statement.executeUpdate("DELETE FROM guild_recruits");
            long[] ids = {10, 20, 30};
            String[] names = {"Лира", "Кирилл", "Мира"};
            int[] levels = {25, 1, 100};
            for (int i = 0; i < ids.length; i++) {
                insert.setLong(1, ids[i]);
                insert.setString(2, names[i]);
                insert.setInt(3, levels[i]);
                insert.executeUpdate();
            }
        }
    }

    @Test
    void registersRepositoryControllerAndJdbcTemplate() throws Exception {
        assertFalse(applicationContext.getBeansWithAnnotation(Repository.class).isEmpty(),
                "Нужен Spring-компонент @Repository.");
        assertFalse(applicationContext.getBeansWithAnnotation(RestController.class).isEmpty(),
                "Нужен Spring-компонент @RestController.");
        assertNotNull(applicationContext.getBean(
                Class.forName("org.springframework.jdbc.core.JdbcTemplate")));
    }

    @ParameterizedTest
    @MethodSource("initialRecruits")
    void returnsStoredRecruit(long id, String name, int level) throws Exception {
        expectRecruit(id, name, level);
    }

    private static Stream<Arguments> initialRecruits() {
        return Stream.of(
                Arguments.of(10L, "Лира", 25),
                Arguments.of(20L, "Кирилл", 1),
                Arguments.of(30L, "Мира", 100)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "999", "9223372036854775807", "-9223372036854775808"})
    void missingLongIdReturnsNotFound(String id) throws Exception {
        mockMvc.perform(get("/guild/recruits/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808", "-9223372036854775809"})
    void idThatCannotBeConvertedToLongReturnsBadRequest(String id) throws Exception {
        mockMvc.perform(get("/guild/recruits/{id}", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readsLongIdInsertedAfterApplicationStartup() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO guild_recruits (id, name, level) VALUES (?, ?, ?)")) {
            insert.setLong(1, 5_000_000_007L);
            insert.setString(2, "Новичок");
            insert.setInt(3, 42);
            insert.executeUpdate();
        }
        expectRecruit(5_000_000_007L, "Новичок", 42);
    }

    @Test
    void observesChangesAndDeletionThroughAnotherConnection() throws Exception {
        expectRecruit(10, "Лира", 25);
        String changedName = " Лира \"Пламя\" ";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE guild_recruits SET name = ?, level = ? WHERE id = ?")) {
            update.setString(1, changedName);
            update.setInt(2, 70);
            update.setLong(3, 10);
            assertEquals(1, update.executeUpdate());
        }
        expectRecruit(10, changedName, 70);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement delete = connection.prepareStatement(
                     "DELETE FROM guild_recruits WHERE id = ?")) {
            delete.setLong(1, 10);
            assertEquals(1, delete.executeUpdate());
        }
        mockMvc.perform(get("/guild/recruits/10"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void repeatedGetDoesNotChangeDatabase() throws Exception {
        expectRecruit(20, "Кирилл", 1);
        expectRecruit(20, "Кирилл", 1);
        assertSeedRows();
    }

    @Test
    void schemaProtectsIdWithPrimaryKey() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO guild_recruits (id, name, level) VALUES (?, ?, ?)")) {
            insert.setLong(1, 10);
            insert.setString(2, "Другой");
            insert.setInt(3, 50);
            assertThrows(SQLException.class, insert::executeUpdate);
        }
        assertSeedRows();
    }

    private void expectRecruit(long id, String name, int level) throws Exception {
        MvcResult result = mockMvc.perform(get("/guild/recruits/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertTrue(response.path("id").isIntegralNumber(), "id должен быть JSON-числом.");
        assertEquals(id, response.path("id").asLong());
        assertTrue(response.path("name").isTextual(), "name должен быть JSON-строкой.");
        assertEquals(name, response.path("name").asText());
        assertTrue(response.path("level").isIntegralNumber(), "level должен быть JSON-числом.");
        assertEquals(level, response.path("level").asInt());
    }

    private void assertSeedRows() throws SQLException {
        long[] ids = {10, 20, 30};
        String[] names = {"Лира", "Кирилл", "Мира"};
        int[] levels = {25, 1, 100};
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT id, name, level FROM guild_recruits ORDER BY id")) {
            for (int i = 0; i < ids.length; i++) {
                assertTrue(rows.next(), "В таблице должны быть три начальных рекрута из условия.");
                assertEquals(ids[i], rows.getLong("id"));
                assertEquals(names[i], rows.getString("name"));
                assertEquals(levels[i], rows.getInt("level"));
            }
            assertFalse(rows.next(), "Начальная таблица должна содержать ровно три записи.");
        }
    }
}
