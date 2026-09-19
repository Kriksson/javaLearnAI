package learning.task008;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardFileRepositoryTest {
    @TempDir
    Path tempDir;

    private final LeaderboardFileRepository repository = new LeaderboardFileRepository();

    @Test
    void savesAndLoadsEntriesInOriginalOrder() throws IOException {
        Path file = tempDir.resolve("leaderboard.txt");
        List<ScoreEntry> entries = List.of(
                new ScoreEntry("Kirill", 1500),
                new ScoreEntry("Player One", 700),
                new ScoreEntry("Игрок", 250)
        );

        repository.save(file, entries);

        assertEquals(entries, repository.load(file));
        assertEquals(
                List.of("Kirill;1500", "Player One;700", "Игрок;250"),
                Files.readAllLines(file, StandardCharsets.UTF_8)
        );
    }

    @Test
    void createsMissingParentDirectories() throws IOException {
        Path file = tempDir.resolve("saves").resolve("season-1").resolve("scores.txt");

        repository.save(file, List.of(new ScoreEntry("Kirill", 10)));

        assertTrue(Files.exists(file));
    }

    @Test
    void overwritesExistingFileAndSupportsEmptyList() throws IOException {
        Path file = tempDir.resolve("scores.txt");
        Files.writeString(file, "old data", StandardCharsets.UTF_8);

        repository.save(file, List.of());

        assertEquals("", Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(List.of(), repository.load(file));
    }

    @Test
    void returnsMutableEmptyListWhenFileDoesNotExist() throws IOException {
        List<ScoreEntry> entries = repository.load(tempDir.resolve("missing.txt"));

        assertInstanceOf(ArrayList.class, entries);
        entries.add(new ScoreEntry("Kirill", 1));
        assertEquals(1, entries.size());
    }

    @Test
    void ignoresEmptyLinesAndTrimsFields() throws IOException {
        Path file = tempDir.resolve("scores.txt");
        Files.writeString(file, "\n  Kirill  ; 42 \n\nAlex;7\n", StandardCharsets.UTF_8);

        assertEquals(
                List.of(new ScoreEntry("Kirill", 42), new ScoreEntry("Alex", 7)),
                repository.load(file)
        );
    }

    @Test
    void rejectsMalformedLines() throws IOException {
        assertMalformed("Kirill");
        assertMalformed("Kirill;10;extra");
        assertMalformed(" ;10");
        assertMalformed("Kirill;abc");
        assertMalformed("Kirill;-1");
    }

    private void assertMalformed(String content) throws IOException {
        Path file = tempDir.resolve("malformed.txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> repository.load(file));
    }
}
