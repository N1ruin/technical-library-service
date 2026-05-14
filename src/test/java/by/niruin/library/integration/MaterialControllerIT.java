package by.niruin.library.integration;

import by.niruin.library.domain.Material;
import by.niruin.library.repository.MaterialRepository;
import by.niruin.library.repository.TransactionOutboxRepository;
import by.niruin.library.service.MaterialService;
import by.niruin.library.service.MessageBrokerService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MaterialControllerIT extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private TransactionOutboxRepository outboxRepository;
    @Autowired
    private MessageBrokerService messageBrokerService;
    @Autowired
    private MaterialRepository materialRepository;

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

    @Test
    void createMaterial_shouldReturnSavedMaterial() throws Exception {
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
                        jsonPath("$.supplierCode").value("245"));

        assertThat(outboxRepository.findAll()).hasSize(1);

        messageBrokerService.sendMessages();
        Thread.sleep(1000);
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> outboxRepository.findAll().isEmpty());
    }

    @Test
    void createMaterial_shouldThrowsValidationException() throws Exception {
        var requestBuilder = post("/api/v1/library-service/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_OIL_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400));
    }

    @Test
    void findById_shouldThrowsEntityNotFound() throws Exception {
        var requestBuilder = get("/api/v1/library-service/materials/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404));
    }

    @Test
    void findById_shouldReturnMaterial() throws Exception {
        var material = createLitolMaterial();
        materialService.save(material);

        var requestBuilder = get("/api/v1/library-service/materials/{id}", material.getId())
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isOk(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json(VALID_LITOL_JSON),
                        jsonPath("$.id").value(material.getId()),
                        jsonPath("$.name").value(material.getName()),
                        jsonPath("$.description").value(material.getDescription()),
                        jsonPath("$.standard").value(material.getStandard()));
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
                        jsonPath("$.content[*].id").exists(),
                        jsonPath("$.content[*].name").value(Matchers.containsInAnyOrder(
                                "Смазка литол-24", "Клей 88ПХ", "Клей 88СА")),
                        jsonPath("$.content[*].description").value(Matchers.containsInAnyOrder(
                                "Для смазки трущихся поверхностей. Хранится в стальных бочках",
                                "Для склеивания поверхностей",
                                "Для склеивания поверхностей")),
                        jsonPath("$.content[*].standard").value(Matchers.containsInAnyOrder(
                                "ГОСТ 24277-2017", "ГОСТ 2213-2022", "ГОСТ 1231-2022")),
                        jsonPath("$.content[*].supplierCode").value(Matchers.containsInAnyOrder(
                                "245", "246", "247")),
                        jsonPath("$.content[*].createdDate").exists(),
                        jsonPath("$.content[*].updatedDate").exists(),
                        jsonPath("$.totalElements").value(3),
                        jsonPath("$.totalPages").value(1));
    }

    @Test
    void deleteById_shouldReturnNotFound_whenMaterialDoesNotExist() throws Exception {
        var requestBuilder = delete("/api/v1/library-service/materials/{id}", 9999L);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json("""
                                {
                                    "error": "Entity not found",
                                    "message": "Entity with id 9999 not found",
                                    "code": 404
                                }
                                """));
    }

    @Test
    void update_shouldUpdateAndReturnUpdatedMaterial() throws Exception {
        var material = createLitolMaterial();
        var id = materialService.save(material).getId();

        mockMvc.perform(put("/api/v1/library-service/materials/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_OIL_JSON))
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        jsonPath("$.id").value(id),
                        jsonPath("$.name").value("Масло И20А"),
                        jsonPath("$.description").value("Новое описание"),
                        jsonPath("$.standard").value("ISO 12312-23"),
                        jsonPath("$.supplierCode").value("222"),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists());
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
                        jsonPath("$.code").value(400));
    }

    @Test
    void update_throwsEntityNotFound_whenMaterialDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/v1/library-service/materials/{id}", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_OIL_JSON))
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404));
    }

    @Test
    void findById_shouldReturnCachedObject_AfterDeletionFromRepository() {
        var material = materialService.save(createLitolMaterial());
        materialRepository.deleteById(material.getId());

        var cached = materialService.findById(material.getId());

        assertThat(cached).usingRecursiveComparison()
                .isEqualTo(material);
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
