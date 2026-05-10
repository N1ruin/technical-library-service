package by.niruin.library.controller;

import by.niruin.library.mapper.SafetyInstructionMapper;
import by.niruin.library.model.instruction.CreateSafetyInstructionRequest;
import by.niruin.library.model.instruction.CreateSafetyInstructionResponse;
import by.niruin.library.model.instruction.SafetyInstructionDto;
import by.niruin.library.model.instruction.UpdateSafetyInstructionRequest;
import by.niruin.library.service.SafetyInstructionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/library-service/safety-instructions")
public class SafetyInstructionController {
    private final SafetyInstructionService safetyInstructionService;
    private final SafetyInstructionMapper safetyInstructionMapper;

    public SafetyInstructionController(SafetyInstructionService safetyInstructionService,
                                       SafetyInstructionMapper safetyInstructionMapper) {
        this.safetyInstructionService = safetyInstructionService;
        this.safetyInstructionMapper = safetyInstructionMapper;
    }

    @PostMapping
    public ResponseEntity<CreateSafetyInstructionResponse> createMaterial(@Valid @RequestBody CreateSafetyInstructionRequest request) {
        var material = safetyInstructionMapper.toInstruction(request);

        var created = safetyInstructionService.save(material);

        var response = safetyInstructionMapper.toCreateResponse(created);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SafetyInstructionDto> findById(@PathVariable Long id) {
        var instruction = safetyInstructionService.findById(id);

        var response = safetyInstructionMapper.toDto(instruction);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SafetyInstructionDto>> findAll() {
        var instructions = safetyInstructionService.findAll();

        var dtoList = safetyInstructionMapper.toDtoList(instructions);

        return ResponseEntity.ok(dtoList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SafetyInstructionDto> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateSafetyInstructionRequest request) {
        var updated = safetyInstructionService.update(id, request);

        var response = safetyInstructionMapper.toDto(updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        safetyInstructionService.deleteById(id);
    }
}
