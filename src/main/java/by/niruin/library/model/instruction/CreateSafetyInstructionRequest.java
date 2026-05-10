package by.niruin.library.model.instruction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record CreateSafetyInstructionRequest(
        @NotNull
        @Pattern(regexp = "[а-яА-Я0-9-\\s:]{1,10}")
        String number,
        @NotNull
        @Length(max = 300)
        String description) {
}
