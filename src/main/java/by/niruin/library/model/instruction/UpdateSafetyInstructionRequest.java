package by.niruin.library.model.instruction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateSafetyInstructionRequest(
        @NotNull
        @Pattern(regexp = "[а-яА-Я0-9-\\s:]+")
        String number,
        @NotNull
        String description) {
}
