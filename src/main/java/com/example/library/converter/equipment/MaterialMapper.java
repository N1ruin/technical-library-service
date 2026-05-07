package com.example.library.converter.equipment;

import com.example.library.domain.Material;
import com.example.library.model.material.CreateMaterialRequest;
import com.example.library.model.material.CreateMaterialResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MaterialMapper {
    Material toMaterial(CreateMaterialRequest request);

    CreateMaterialResponse toCreateResponse(Material material);
}
