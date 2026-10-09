package learning.task087;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BankUserBody(
        @NotBlank @Size(max=40) @Pattern(regexp = "[^:]+") String username,
        @Pattern(regexp = "[!-~]{8,40}") @NotNull String password) {
}
