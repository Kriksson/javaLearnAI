package learning.task069;


import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
public class RecruitsController {

    private final RecruitsRepository recruitsRepository;
    public RecruitsController(RecruitsRepository recruitsRepository) {
        this.recruitsRepository = recruitsRepository;
    }


    @PostMapping("/guild/recruits")
    ResponseEntity<Recruit> registerRecruit(@Valid @RequestBody Recruit recruit) {
        try {
            boolean result = recruitsRepository.registerRecruit(recruit);
            URI loc = URI.create("/guild/recruits/" + recruit.id());
            return ResponseEntity.created(loc).body(recruit);
        } catch (DuplicateKeyException e) {
            return  ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/guild/recruits/{id}")
    ResponseEntity<Recruit> getRecruit(@PathVariable("id") long id) {
        Optional<Recruit> r = recruitsRepository.getRecruit(id);
        return r.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
