package learning.task025;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MergeConflictTest {
    private static final Path DIRECTORY = Path.of("tasks/active/task_025_git_merge_conflict");

    @Test
    void conflictIsResolvedAndDescribed() throws IOException {
        String balance = Files.readString(DIRECTORY.resolve("game-balance.txt")).trim();
        String report = Files.readString(DIRECTORY.resolve("conflict-report.md")).toLowerCase();

        assertEquals("rewardMultiplier=1.20", balance);
        assertFalse(balance.contains("<<<<<<<"));
        assertFalse(balance.contains("======="));
        assertFalse(balance.contains(">>>>>>>"));
        assertFalse(report.contains("todo"));
        assertTrue(report.contains("одн") && report.contains("строк"));
    }
}
