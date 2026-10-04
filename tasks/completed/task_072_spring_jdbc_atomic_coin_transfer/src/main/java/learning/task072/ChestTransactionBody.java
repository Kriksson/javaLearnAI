package learning.task072;

import jakarta.validation.constraints.Min;

public record ChestTransactionBody(@Min(1) long fromId, @Min(1) long toId) {
}
