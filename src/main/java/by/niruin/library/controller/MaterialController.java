package by.niruin.library.controller;

import by.niruin.library.mapper.MaterialMapper;
import by.niruin.library.model.material.CreateMaterialRequest;
import by.niruin.library.model.material.CreateMaterialResponse;
import by.niruin.library.model.material.MaterialDto;
import by.niruin.library.model.material.UpdateMaterialRequest;
import by.niruin.library.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/library-service/materials")
public class MaterialController {
    private final MaterialService materialService;
    private final MaterialMapper materialMapper;

    public MaterialController(MaterialService materialService, MaterialMapper materialMapper) {
        this.materialService = materialService;
        this.materialMapper = materialMapper;
    }

    @PostMapping
    public ResponseEntity<CreateMaterialResponse> createMaterial(@Valid @RequestBody CreateMaterialRequest request) {
        var material = materialMapper.toMaterial(request);

        var created = materialService.save(material);

        var response = materialMapper.toCreateResponse(created);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialDto> findById(@PathVariable Long id) {
        var material = materialService.findById(id);

        var response = materialMapper.toDto(material);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<MaterialDto>> findAll() {
        var materials = materialService.findAll();

        var dtoList = materialMapper.toDtoList(materials);

        return ResponseEntity.ok(dtoList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialDto> update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateMaterialRequest request) {
        var updated = materialService.update(id, request);

        var response = materialMapper.toDto(updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        materialService.deleteById(id);
    }
}
