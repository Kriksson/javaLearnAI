package learning.task067;


import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class RaidRepository {

    Map<Long, Integer> maxReservations = new HashMap<>();

    public void addReservation(long id) {
        if (id < 1) throw new IllegalStateException();
        if (maxReservations.containsKey(id)) {
            maxReservations.put(id, maxReservations.get(id) + 1);
        } else {
            maxReservations.put(id, 1);
        }
    }

    public int getReservations(long id) {
        if (id < 1) throw new IllegalStateException();
        return maxReservations.getOrDefault(id, 0);
    }
}
