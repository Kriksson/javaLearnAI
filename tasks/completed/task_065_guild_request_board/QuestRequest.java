package learning.task065;

import jakarta.validation.constraints.*;

public record QuestRequest(
        @NotBlank @Size(max=80) String title,
        @NotNull @Min(1) @Max(10000) int reward) {
}
