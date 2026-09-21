package learning.task026;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GitRestoreReportTest {
    @Test
    void reportExplainsDiffAndRestore() throws IOException {
        String report = Files.readString(
                Path.of("tasks/active/task_026_git_restore/restore-report.md"))
                .toLowerCase();

        assertFalse(report.contains("todo"));
        assertTrue(report.contains("git diff"));
        assertTrue(report.contains("git restore"));
        assertTrue(report.contains("pom.xml"));
    }
}
