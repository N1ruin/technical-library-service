package by.niruin.library.model.material;

public record CreateMaterialResponse(Long id,
                                     String name,
                                     String description,
                                     String standard,
                                     String supplierCode) {
}
