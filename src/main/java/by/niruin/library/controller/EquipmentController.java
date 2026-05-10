package by.niruin.library.controller;

import by.niruin.library.mapper.EquipmentMapper;
import by.niruin.library.model.equipment.CreateEquipmentRequest;
import by.niruin.library.model.equipment.CreateEquipmentResponse;
import by.niruin.library.model.equipment.EquipmentDto;
import by.niruin.library.model.equipment.UpdateEquipmentRequest;
import by.niruin.library.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/library-service/equipments")
public class EquipmentController {
    private final EquipmentService equipmentService;
    private final EquipmentMapper equipmentMapper;

    public EquipmentController(EquipmentService equipmentService, EquipmentMapper equipmentMapper) {
        this.equipmentService = equipmentService;
        this.equipmentMapper = equipmentMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateEquipmentResponse> createEquipment(@Valid @ModelAttribute CreateEquipmentRequest request) {
        var equipment = equipmentMapper.toEquipment(request);

        var saved = equipmentService.save(equipment, request.file());

        var response = equipmentMapper.toCreateResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentDto> findById(@PathVariable Long id) {
        var equipment = equipmentService.findById(id);

        var response = equipmentMapper.toDto(equipment);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<EquipmentDto>> findAll() {
        var equipments = equipmentService.findAll();

        var response = equipmentMapper.toDtoList(equipments);

        return ResponseEntity.ok(response);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EquipmentDto> update(@PathVariable Long id,
                                               @Valid @ModelAttribute UpdateEquipmentRequest request) {
        var updated = equipmentService.update(id, request);

        var response = equipmentMapper.toDto(updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        equipmentService.deleteById(id);
    }
}
