package by.niruin.library.controller;

import by.niruin.library.mapper.MaterialMapper;
import by.niruin.library.model.material.CreateMaterialRequest;
import by.niruin.library.model.material.CreateMaterialResponse;
import by.niruin.library.model.material.MaterialDto;
import by.niruin.library.model.material.UpdateMaterialRequest;
import by.niruin.library.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/library-service/materials")
public class MaterialController {
    private final MaterialService materialService;
    private final MaterialMapper materialMapper;

    public MaterialController(MaterialService materialService, MaterialMapper materialMapper) {
        this.materialService = materialService;
        this.materialMapper = materialMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
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
    public ResponseEntity<Page<MaterialDto>> findAll(@PageableDefault(size = 20, sort = "id",
            direction = Sort.Direction.DESC) Pageable pageable) {
        var materialPage = materialService.findAll(pageable);

        var dtoPage = materialPage.map(materialMapper::toDto);

        return ResponseEntity.ok(dtoPage);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<MaterialDto> update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateMaterialRequest request) {
        var updated = materialService.update(id, request);

        var response = materialMapper.toDto(updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        materialService.deleteById(id);
    }
}
