package com.example.library.controller;

import com.example.library.converter.equipment.MaterialMapper;
import com.example.library.model.material.CreateMaterialRequest;
import com.example.library.model.material.CreateMaterialResponse;
import com.example.library.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/library-service/material")
public class MaterialController {
    private final MaterialService materialService;
    private final MaterialMapper materialMapper;

    public MaterialController(MaterialService materialService, MaterialMapper materialMapper) {
        this.materialService = materialService;
        this.materialMapper = materialMapper;
    }

    @PostMapping
    public ResponseEntity<CreateMaterialResponse> createMaterial(@Valid CreateMaterialRequest request) {
        var material = materialMapper.toMaterial(request);

        var created = materialService.createMaterial(material);

        var response = materialMapper.toCreateResponse(created);

        return ResponseEntity.ok(response);
    }
}
