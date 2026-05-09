package by.niruin.library.integration.equipment;

import by.niruin.library.domain.Material;
import by.niruin.library.service.MaterialService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@Transactional
public class MaterialControllerIT extends BaseTest {
    @Autowired
    static PostgreSQLContainer postgreSQLContainer;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private EntityManager em;

    private static final String VALID_LITOL_JSON = """
            {
                "name": "Смазка литол-24",
                "description": "Для смазки трущихся поверхностей. Хранится в стальных бочках",
                "standard": "ГОСТ 24277-2017",
                "supplierCode": "245"
            }
            """;
    private static final String VALID_OIL_JSON = """
            {
                "name": "Масло И20А",
                "description": "Новое описание",
                "standard": "ISO 12312-23",
                "supplierCode": "222"
            }
            """;

    private static final String INVALID_OIL_JSON = """
            {
                "name": "Test oil",
                "description": "Для смазки трущихся поверхностей. Хранится в стальных бочках",
                "standard": "ГОСТ 24277-2017",
                "supplierCode": "245"
            }
            """;

    @BeforeEach
    void cleanDatabase() {
        em.createNativeQuery("TRUNCATE TABLE library.material RESTART IDENTITY CASCADE")
                .executeUpdate();
    }

    @Test
    void createMaterial_returnCreatedMaterial() throws Exception {
        var requestBuilder = post("/api/v1/library-service/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_LITOL_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isCreated(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json(VALID_LITOL_JSON),
                        jsonPath("$.id").exists(),
                        jsonPath("$.name").value("Смазка литол-24"),
                        jsonPath("$.description").value("Для смазки трущихся поверхностей. Хранится в стальных бочках"),
                        jsonPath("$.standard").value("ГОСТ 24277-2017"),
                        jsonPath("$.supplierCode").value("245")
                );
    }

    @Test
    void createMaterial_throwsValidationException() throws Exception {
        var requestBuilder = post("/api/v1/library-service/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_OIL_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400)
                );
    }

    @Test
    void findById_throwsEntityNotFound() throws Exception {
        var requestBuilder = get("/api/v1/library-service/materials/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json("""
                                {
                                    "error": "Entity not found",
                                    "message": "Entity with id 1 not found",
                                    "code": 404
                                }
                                """)
                );
    }

    @Test
    void findById_shouldReturnMaterial() throws Exception {
        var litol = createLitolMaterial();
        materialService.save(litol);

        var requestBuilder = get("/api/v1/library-service/materials/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isOk(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json(VALID_LITOL_JSON),
                        jsonPath("$.id").exists()
                );
    }

    @Test
    void findAll_shouldReturnListWithSizeThree() throws Exception {
        var materials = List.of(createLitolMaterial(), createGluePHMaterial(), createGlueSAMaterial());
        materials.forEach(materialService::save);

        var requestBuilder = get("/api/v1/library-service/materials")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isOk(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.length()").value(3),
                        jsonPath("$[0].id").value(1),
                        jsonPath("$[0].name").value("Смазка литол-24"),
                        jsonPath("$[0].description").value("Для смазки трущихся поверхностей. Хранится в стальных бочках"),
                        jsonPath("$[0].standard").value("ГОСТ 24277-2017"),
                        jsonPath("$[0].supplierCode").value("245"),
                        jsonPath("$[0].createdDate").exists(),
                        jsonPath("$[0].updatedDate").exists(),

                        jsonPath("$[1].id").value(2),
                        jsonPath("$[1].name").value("Клей 88ПХ"),
                        jsonPath("$[1].description").value("Для склеивания поверхностей"),
                        jsonPath("$[1].standard").value("ГОСТ 2213-2022"),
                        jsonPath("$[1].supplierCode").value("246"),
                        jsonPath("$[1].createdDate").exists(),
                        jsonPath("$[1].updatedDate").exists(),

                        jsonPath("$[2].id").value(3),
                        jsonPath("$[2].name").value("Клей 88СА"),
                        jsonPath("$[2].description").value("Для склеивания поверхностей"),
                        jsonPath("$[2].standard").value("ГОСТ 1231-2022"),
                        jsonPath("$[2].supplierCode").value("247"),
                        jsonPath("$[2].createdDate").exists(),
                        jsonPath("$[2].updatedDate").exists()
                );
    }

    @Test
    void deleteById_shouldReturnNotFound_whenMaterialDoesNotExist() throws Exception {
        var requestBuilder = delete("/api/v1/library-service/materials/{id}", 1L);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json("""
                                {
                                    "error": "Entity not found",
                                    "message": "Entity with id 1 not found",
                                    "code": 404
                                }
                                """)
                );
    }

    @Test
    void update_shouldUpdateAndReturnUpdatedMaterial() throws Exception {
        var material = createLitolMaterial();
        materialService.save(material);

        mockMvc.perform(put("/api/v1/library-service/materials/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_OIL_JSON))
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        jsonPath("$.id").value(1),
                        jsonPath("$.name").value("Масло И20А"),
                        jsonPath("$.description").value("Новое описание"),
                        jsonPath("$.standard").value("ISO 12312-23"),
                        jsonPath("$.supplierCode").value("222"),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        content().json("""
                                {
                                    "id": 1,
                                    "name": "Масло И20А",
                                    "description": "Новое описание"
                                }
                                """)
                );
    }

    @Test
    void update_throwsValidationException() throws Exception {
        mockMvc.perform(put("/api/v1/library-service/materials/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_OIL_JSON))
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400)
                );
    }

    @Test
    void update_throwsEntityNotFound_whenMaterialDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/v1/library-service/materials/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_OIL_JSON))
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404)
                );
    }

    private Material createLitolMaterial() {
        var material = new Material();
        material.setName("Смазка литол-24");
        material.setStandard("ГОСТ 24277-2017");
        material.setDescription("Для смазки трущихся поверхностей. Хранится в стальных бочках");
        material.setSupplierCode("245");

        return material;
    }

    private Material createGluePHMaterial() {
        var material = new Material();
        material.setName("Клей 88ПХ");
        material.setStandard("ГОСТ 2213-2022");
        material.setDescription("Для склеивания поверхностей");
        material.setSupplierCode("246");

        return material;
    }

    private Material createGlueSAMaterial() {
        var material = new Material();
        material.setName("Клей 88СА");
        material.setStandard("ГОСТ 1231-2022");
        material.setDescription("Для склеивания поверхностей");
        material.setSupplierCode("247");

        return material;
    }
}
