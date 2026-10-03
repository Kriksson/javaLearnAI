package learning.task066;


import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RaidService {

    RaidRepository raidRepository;
    public RaidService(RaidRepository raidRepository) {
        this.raidRepository = raidRepository;
    }

    public long addHeroToRaid(UserBody userBody) {
        try {
            return raidRepository.addRaid(userBody);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public UserBody getRaidInfo(long id) {
        Optional<UserBody> u =  raidRepository.getRaid(id);
        if (u.isPresent()) {
            return u.get();
        } else {
            throw new IllegalArgumentException(String.format("rid %d not found", id));
        }
    }
}
