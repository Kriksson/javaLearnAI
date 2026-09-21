package learning.task023;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchReportTest {
    private static final Path REPORT = Path.of(
            "tasks/active/task_023_git_branch/branch-report.md");

    @Test
    void reportExplainsBranchWorkflow() throws IOException {
        String report = Files.readString(REPORT);

        assertTrue(report.contains("task-023-git-branch"));
        assertTrue(report.contains("git status -sb"));
        assertTrue(report.contains("git add tasks/active/task_023_git_branch/branch-report.md"));
        assertTrue(report.contains("ветк") || report.contains("Ветк"));
    }
}
