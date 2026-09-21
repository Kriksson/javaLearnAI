package learning.task024;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MergeReportTest {
    private static final Path REPORT = Path.of("tasks/active/task_024_git_merge/merge-report.md");

    @Test
    void reportDescribesMergeIntoMain() throws IOException {
        String report = Files.readString(REPORT).toLowerCase();

        assertFalse(report.contains("todo"));
        assertTrue(report.contains("main"));
        assertTrue(report.contains("merge"));
        assertTrue(report.contains("origin/main"));
    }
}
