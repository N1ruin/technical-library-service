package com.example.library.model.material;

public record CreateMaterialRequest(String name,
                                    String description,
                                    String standard,
                                    String supplierCode) {
}
