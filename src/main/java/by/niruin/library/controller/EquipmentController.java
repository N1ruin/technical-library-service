package by.niruin.library.controller;

import by.niruin.library.mapper.EquipmentMapper;
import by.niruin.library.model.equipment.CreateEquipmentRequest;
import by.niruin.library.model.equipment.CreateEquipmentResponse;
import by.niruin.library.model.equipment.EquipmentDto;
import by.niruin.library.model.equipment.UpdateEquipmentRequest;
import by.niruin.library.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/library-service/equipments")
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
    public ResponseEntity<Page<EquipmentDto>> findAll(@PageableDefault(size = 20, sort = "id",
            direction = Sort.Direction.DESC) Pageable pageable) {
        var equipmentPage = equipmentService.findAll(pageable);

        var dtoPage = equipmentPage.map(equipmentMapper::toDto);

        return ResponseEntity.ok(dtoPage);
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
        equipmentService.delete(id);
    }
}
