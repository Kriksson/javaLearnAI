package learning.task096;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExpenseBody(
        @NotNull @Min(1) Long amountMinor,
        @NotBlank @Size(max=100) String description) {
}
