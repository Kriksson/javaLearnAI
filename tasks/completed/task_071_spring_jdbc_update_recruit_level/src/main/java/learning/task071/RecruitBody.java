package learning.task071;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RecruitBody(@Min(1) @Max(100) int level) {
}
