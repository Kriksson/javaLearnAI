package learning.task015;

import java.util.ArrayList;
import java.util.List;

public class Workshop {

    private final List<Upgradeable> upgradeableList = new ArrayList<>();

    public Workshop() {}

    public void add(Upgradeable item) {
        if (item == null) throw new IllegalArgumentException();
        upgradeableList.add(item);
    }

    public int size() {
        return upgradeableList.size();
    }

    public int upgradeAll() {
        int total = 0;
        for (Upgradeable upgradeable : upgradeableList) {
            boolean result = upgradeable.upgrade();
            if (result) total++;
        }
        return total;
    }
}
