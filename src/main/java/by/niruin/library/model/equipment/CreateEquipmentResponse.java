package by.niruin.library.model.equipment;

public record CreateEquipmentResponse(Long id,
                                      String name,
                                      String index,
                                      String description,
                                      String imageName,
                                      String type) {
}
