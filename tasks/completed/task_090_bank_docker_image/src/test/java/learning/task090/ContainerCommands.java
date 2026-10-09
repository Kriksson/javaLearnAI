package learning.task090;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Test infrastructure: argument lists, bounded waits and logs instead of pipe deadlocks. */
final class ContainerCommands {
    record Result(int code, String output) {}
    private final Path root;
    private final Path logs;
    private int number;

    ContainerCommands(Path root) throws IOException {
        this.root = root;
        logs = root.resolve("target/task090-checks"); Files.createDirectories(logs);
    }

    Result run(List<String> arguments, Duration timeout) throws Exception {
        Path log = logs.resolve(String.format("%02d-%s.log", ++number, Path.of(arguments.getFirst()).getFileName()));
        Process process = new ProcessBuilder(arguments).directory(root.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                terminate(process);
                throw new AssertionError("Команда превысила " + timeout.toSeconds() + " секунд: " + arguments.getFirst()
                        + ". Лог: " + log);
            }
            return new Result(process.exitValue(), Files.readString(log));
        } catch (InterruptedException interrupted) {
            terminate(process); Thread.currentThread().interrupt(); throw interrupted;
        }
    }

    String checked(List<String> arguments, Duration timeout) throws Exception {
        Result result = run(arguments, timeout);
        if (result.code() != 0) throw new AssertionError("Команда " + arguments.getFirst() + " "
                + (arguments.size() > 1 ? arguments.get(1) : "") + " завершилась с кодом " + result.code() + ":\n" + result.output());
        return result.output();
    }

    private void terminate(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    static void deleteTree(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
}
