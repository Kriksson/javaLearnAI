package learning.task027;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BattleHistory {

    private final List<MatchResult> matchResultList = new ArrayList<>();

    public List<MatchResult> all() {
        return List.copyOf(matchResultList);
    }

    public void add(MatchResult result) {
        if (result == null) throw new IllegalArgumentException();
        matchResultList.add(result);
    }

    public int size() {
        return matchResultList.size();
    }

    public List<MatchResult> top(int limit) {
        if (limit < 0) throw new IllegalArgumentException();
        if (limit == 0) return List.of();
        final List<MatchResult> topResult = new ArrayList<>(matchResultList);
        topResult.sort(Comparator.comparingInt(MatchResult::score)
                .reversed()
                .thenComparingInt(MatchResult::durationSeconds)
                .thenComparing(MatchResult::playerName));
        return List.copyOf(topResult.subList(0, Math.min(limit, topResult.size())));
    }
}
