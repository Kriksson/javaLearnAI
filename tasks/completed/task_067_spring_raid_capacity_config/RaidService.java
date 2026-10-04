package learning.task067;


import org.springframework.stereotype.Service;

@Service
public class RaidService {

    RaidRepository raidRepository;
    public RaidService(RaidRepository raidRepository) {
        this.raidRepository = raidRepository;
    }

    public void addReservations(long id) {
        try {
            raidRepository.addReservation(id);
        } catch (IllegalStateException e) {
            throw new IllegalStateException();
        }
    }

    public int getReservations(long id) {
        try {
            return raidRepository.getReservations(id);
        } catch (IllegalStateException e) {
            throw new IllegalStateException();
        }
    }
}
