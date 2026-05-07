package com.example.library.service;

import com.example.library.domain.Material;
import org.springframework.stereotype.Service;

@Service
public class MaterialService {

    public Material createMaterial(Material material) {
        return new Material();
    }
}
