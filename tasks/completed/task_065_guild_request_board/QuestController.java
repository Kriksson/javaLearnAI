package learning.task065;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
public class QuestController {

    QuestService questService;
    public QuestController(QuestService questService, QuestRepository questRepository) {
        this.questService = questService;
        this.questRepository = questRepository;

    }
    QuestRepository questRepository;

    @PostMapping("/guild/requests") // Вернуть URI Location и создать объявление
    ResponseEntity<QuestAnswer> createQuestRequest(@Valid @RequestBody QuestRequest questRequest) {
        int id = questService.createQuest(questRequest);
        if (id > -1) {
            URI location = URI.create("/guild/requests/" + id);
            Optional<QuestAnswer> questAnswer = questService.getQuestById(id);
            return questAnswer.map(e -> ResponseEntity.created(location).body(e)).orElseGet(() -> ResponseEntity.badRequest().build());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/guild/requests") // Вернуть Коллекцию квестов которая так же содержит их ID
    ResponseEntity<List<QuestAnswer>> getAllQuests() {
        return ResponseEntity.ok(questService.getAllQuests());
    }

    @GetMapping("/guild/requests/{id}") // Вернуть объект квеста по ID
    ResponseEntity<QuestAnswer> getQuestById(@PathVariable("id") int id) {
        Optional<QuestAnswer> questAnswer = questService.getQuestById(id);
        return questAnswer.map(e -> ResponseEntity.ok(e)).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/guild/requests/{id}/claim") // Принять квест и вернуть измененный объект квеста
    ResponseEntity<QuestAnswer> claimQuestById(@PathVariable("id") int id, @Valid @RequestBody Hero hero) {
        if (!questRepository.isQuest(id)) return  ResponseEntity.notFound().build();
        try {
            Optional<QuestAnswer> q = questService.claimQuestById(id, hero);
            if (q.isPresent()) {
                QuestAnswer questAnswer = q.get();
                return ResponseEntity.ok(questAnswer);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (QuestAlreadyClaimedException e) {
            return ResponseEntity.status(409).build();
        }
    }
}
