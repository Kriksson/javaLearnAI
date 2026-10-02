package learning.task065;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Hero(
        @NotBlank @Size(max=40) String heroName) {
}
