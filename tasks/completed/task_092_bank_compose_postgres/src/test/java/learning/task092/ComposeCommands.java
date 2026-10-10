package learning.task092;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Infrastructure owned by the trainer; the application is created by the learner. */
final class ComposeCommands {
    record Result(int code, String output) {}
    private final Path root;
    private final Path logs;
    private int number;

    ComposeCommands(Path root) throws IOException {
        this.root = root;
        logs = root.resolve("target/task092-checks");
        Files.createDirectories(logs);
    }

    Result run(List<String> arguments, Duration timeout, Map<String, String> environment) throws Exception {
        Path log = logs.resolve(String.format("%03d-%s.log", ++number, Path.of(arguments.getFirst()).getFileName()));
        ProcessBuilder builder = new ProcessBuilder(arguments).directory(root.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        // Host values must not accidentally satisfy or override this test's env file.
        for (String key : List.of("BANK_DB_NAME", "BANK_DB_USER", "BANK_DB_PASSWORD", "BANK_HTTP_PORT", "COMPOSE_ENV_FILES", "COMPOSE_PROFILES")) {
            builder.environment().remove(key);
        }
        builder.environment().putAll(environment);
        Process process = builder.start();
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                terminate(process);
                throw new AssertionError("Команда превысила " + timeout.toSeconds() + " секунд: "
                        + arguments.getFirst() + ". Лог: " + log);
            }
            return new Result(process.exitValue(), Files.readString(log));
        } catch (InterruptedException interrupted) {
            terminate(process);
            Thread.currentThread().interrupt();
            throw interrupted;
        }
    }

    String checked(List<String> arguments, Duration timeout, Map<String, String> environment) throws Exception {
        Result result = run(arguments, timeout, environment);
        if (result.code() != 0) throw new AssertionError("Ошибка команды " + arguments.getFirst() + ":\n" + result.output());
        return result.output();
    }

    private void terminate(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    static void deleteTree(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
}
