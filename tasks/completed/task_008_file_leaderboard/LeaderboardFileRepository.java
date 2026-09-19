package learning.task008;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardFileRepository {
    public void save(Path path, List<ScoreEntry> entries) throws IOException {

        Path parent = path.getParent();

        List<String> lines = new ArrayList<>();

        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        for (ScoreEntry entry : entries) {
            lines.add(String.format("%s;%d", entry.getPlayerName(), entry.getScore()));
        }

        Files.write(path, lines, StandardCharsets.UTF_8);
    }


    public List<ScoreEntry> load(Path path) throws IOException {
        if (Files.notExists(path)) {
            return new ArrayList<>();
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<ScoreEntry> entries = new ArrayList<>();
        for(String line : lines) {
            if (line.isBlank()) continue;
            int sep = line.indexOf(';');
            if (sep > 0 || sep != line.lastIndexOf(';')) {
                String name = line.substring(0, sep).trim();
                int score = Integer.parseInt(line.substring(sep + 1).trim());
                entries.add(new ScoreEntry(name, score));
            } else {
                throw new IllegalArgumentException();
            }
        }
        return entries;
    }
}
