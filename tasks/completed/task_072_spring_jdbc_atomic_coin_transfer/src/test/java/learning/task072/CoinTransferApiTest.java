package learning.task072;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:task072-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoinTransferApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    void tableStartsEmpty() throws Exception {
        assertNotNull(dataSource, "Настрой H2 и Spring JDBC.");
        assertNotNull(getClass().getClassLoader().getResource("schema.sql"),
                "Создай src/main/resources/schema.sql.");
        assertNotNull(getClass().getClassLoader().getResource("application.properties"),
                "Создай src/main/resources/application.properties.");
        assertEquals(List.of(), storedRows(), "Таблица должна начинаться пустой.");
    }

    @BeforeEach
    void clearsRows() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM guild_chests");
        }
    }

    @Test
    void registersSpringComponentsAndChecksSchema() throws Exception {
        assertFalse(applicationContext.getBeansWithAnnotation(Repository.class).isEmpty());
        assertFalse(applicationContext.getBeansWithAnnotation(Service.class).isEmpty());
        assertFalse(applicationContext.getBeansWithAnnotation(RestController.class).isEmpty());
        assertNotNull(applicationContext.getBean(JdbcTemplate.class));
        assertNotNull(applicationContext.getBean(PlatformTransactionManager.class));

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT id, name, coins FROM guild_chests WHERE 1 = 0")) {
            ResultSetMetaData columns = rows.getMetaData();
            assertEquals(Types.BIGINT, columns.getColumnType(1));
            assertEquals("id", columns.getColumnName(1).toLowerCase(Locale.ROOT));
            assertTrue(columns.isAutoIncrement(1));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(1));
            assertEquals(Types.VARCHAR, columns.getColumnType(2));
            assertEquals(30, columns.getPrecision(2));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(2));
            assertEquals(Types.BIGINT, columns.getColumnType(3));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(3));
            List<String> primaryKeys = new ArrayList<>();
            try (ResultSet keys = connection.getMetaData().getPrimaryKeys(
                    connection.getCatalog(), null, "GUILD_CHESTS")) {
                while (keys.next()) {
                    primaryKeys.add(keys.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            assertEquals(List.of("id"), primaryKeys);
        }

        assertThrows(SQLException.class, () -> {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO guild_chests (name, coins) VALUES ('Bad', -1)");
            }
        }, "Ограничение схемы не должно разрешать отрицательный баланс.");
    }

    @Test
    void transfersOneCoinAndLeavesOtherChestsUnchanged() throws Exception {
        long sourceId = insert("North", 3);
        long targetId = insert("South", 8);
        long untouchedId = insert("West", 17);

        mockMvc.perform(post("/guild/coin-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(sourceId, targetId)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertEquals(List.of(
                List.of(sourceId, "North", 2L),
                List.of(targetId, "South", 9L),
                List.of(untouchedId, "West", 17L)), storedRows());
    }

    @Test
    void returnsConflictWhenSourceHasNoCoins() throws Exception {
        long sourceId = insert("Empty", 0);
        long targetId = insert("Target", 5);
        List<List<Object>> before = storedRows();

        mockMvc.perform(post("/guild/coin-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(sourceId, targetId)))
                .andExpect(status().isConflict());

        assertEquals(before, storedRows());
    }

    @Test
    void returnsConflictWhenSourceIsMissing() throws Exception {
        long targetId = insert("Target", 5);
        List<List<Object>> before = storedRows();

        mockMvc.perform(post("/guild/coin-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Long.MAX_VALUE, targetId)))
                .andExpect(status().isConflict());

        assertEquals(before, storedRows());
    }

    @Test
    void rollsBackSourceDebitWhenDestinationIsMissing() throws Exception {
        long sourceId = insert("Source", 4);
        List<List<Object>> before = storedRows();

        mockMvc.perform(post("/guild/coin-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(sourceId, Long.MAX_VALUE)))
                .andExpect(status().isNotFound());

        assertEquals(before, storedRows(), "Списание должно откатиться вместе с неудачным переводом.");
    }

    @Test
    void rejectsTransferToSameChestWithoutChangingBalance() throws Exception {
        long id = insert("Single", 6);
        List<List<Object>> before = storedRows();

        mockMvc.perform(post("/guild/coin-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(id, id)))
                .andExpect(status().isBadRequest());

        assertEquals(before, storedRows());
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void rejectsInvalidBodyWithoutChangingRows(String body) throws Exception {
        insert("Source", 3);
        insert("Target", 8);
        List<List<Object>> before = storedRows();

        mockMvc.perform(post("/guild/coin-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertEquals(before, storedRows());
    }

    private static Stream<String> invalidBodies() {
        return Stream.of(
                "{}",
                "{\"fromId\":1}",
                "{\"toId\":2}",
                "{\"fromId\":null,\"toId\":2}",
                "{\"fromId\":1,\"toId\":null}",
                "{\"fromId\":0,\"toId\":2}",
                "{\"fromId\":1,\"toId\":0}",
                "{\"fromId\":-1,\"toId\":2}",
                "{\"fromId\":1,\"toId\":-2}",
                "{\"fromId\":\"abc\",\"toId\":2}",
                "{\"fromId\":1,\"toId\":\"abc\"}",
                "{\"fromId\":9223372036854775808,\"toId\":2}",
                "{\"fromId\":1,\"toId\":9223372036854775808}",
                "[]",
                "",
                "{\"fromId\":1,"
        );
    }

    @Test
    void acceptsIdsLargerThanIntegerRange() throws Exception {
        restartIdentity(5_000_000_007L);
        long sourceId = insert("Large source", 2);
        long targetId = insert("Large target", 4);
        assertEquals(5_000_000_007L, sourceId);
        assertEquals(5_000_000_008L, targetId);

        mockMvc.perform(post("/guild/coin-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(sourceId, targetId)))
                .andExpect(status().isNoContent());

        assertEquals(List.of(
                List.of(sourceId, "Large source", 1L),
                List.of(targetId, "Large target", 5L)), storedRows());
    }

    private String json(long fromId, long toId) throws Exception {
        return objectMapper.writeValueAsString(new TransferRequest(fromId, toId));
    }

    private long insert(String name, long coins) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO guild_chests (name, coins) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, name);
            insert.setLong(2, coins);
            assertEquals(1, insert.executeUpdate());
            try (ResultSet keys = insert.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    private void restartIdentity(long nextId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE guild_chests ALTER COLUMN id RESTART WITH " + nextId);
        }
    }

    private List<List<Object>> storedRows() throws Exception {
        List<List<Object>> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT id, name, coins FROM guild_chests ORDER BY id")) {
            while (result.next()) {
                rows.add(List.of(result.getLong("id"), result.getString("name"), result.getLong("coins")));
            }
        }
        return rows;
    }

    private record TransferRequest(long fromId, long toId) {
    }
}
