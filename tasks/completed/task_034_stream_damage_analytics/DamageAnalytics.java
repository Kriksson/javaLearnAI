package learning.task034;

import java.util.*;
import java.util.stream.Collectors;

public class DamageAnalytics {
    public Map<String, Integer> totalDamageByPlayer(List<DamageEvent> events) {

        if (events == null) throw new IllegalArgumentException();

        final Map<String, Integer> totalDamageByPlayer = events.stream()
                .filter(Objects::nonNull)
                .filter((e) -> e.damage() > 0)
                .collect(Collectors.groupingBy(
                        DamageEvent::playerId,
                        Collectors.summingInt(DamageEvent::damage)
                ));
        return Map.copyOf(totalDamageByPlayer);
    }

    public List<String> topPlayers(List<DamageEvent> events, int limit) {
        if (events == null || limit < 0) throw new IllegalArgumentException();
        Map<String, Integer> totalDamageMap = totalDamageByPlayer(events);
        List<String> topPlayers = totalDamageMap.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        return List.copyOf(topPlayers);
    }
}
