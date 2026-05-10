package by.niruin.library.model.equipment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

public record UpdateEquipmentRequest(
        @NotNull
        String name,
        @NotNull
        @Pattern(regexp = "[a-zA-Zа-яА-Я:0-9-\\s]+")
        String index,
        @NotNull
        @Length(max = 200)
        String description,
        MultipartFile file,
        @NotNull
        @Pattern(regexp = "^(ASSEMBLY|CONTROL|CUTTING|AUXILIARY)$")
        String type) {
}
