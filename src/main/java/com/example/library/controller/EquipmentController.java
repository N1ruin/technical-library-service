package com.example.library.controller;

import com.example.library.model.equipment.CreateEquipmentRequest;
import com.example.library.model.equipment.CreateEquipmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/library-service/equipment")
public class EquipmentController {
    @PostMapping
    public ResponseEntity<CreateEquipmentResponse> createEquipment(@Valid CreateEquipmentRequest request) {

    }
}
