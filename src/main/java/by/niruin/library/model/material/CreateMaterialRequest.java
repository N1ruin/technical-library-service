package by.niruin.library.model.material;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record CreateMaterialRequest(
        @NotNull
        @Pattern(regexp = "^[а-яА-Я0-9:.\\s-]+$")
        String name,
        @Length(max = 300)
        String description,
        String standard,
        @NotNull
        @Pattern(regexp = "^[а-яА-Я0-9:.\\s-]+$")
        String supplierCode) {
}
