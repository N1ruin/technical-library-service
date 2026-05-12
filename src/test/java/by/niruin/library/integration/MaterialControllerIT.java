package by.niruin.library.integration;

import by.niruin.library.domain.EventType;
import by.niruin.library.domain.Material;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.repository.TransactionOutboxRepository;
import by.niruin.library.service.MaterialService;
import by.niruin.library.service.MessageBrokerService;
import jakarta.persistence.EntityManager;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MaterialControllerIT extends BaseTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private EntityManager em;
    @Autowired
    private TransactionOutboxRepository outboxRepository;
    @Autowired
    private MessageBrokerService messageBrokerService;

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

    @AfterEach
    void cleanDatabase() {
        em.createNativeQuery("TRUNCATE TABLE library.material RESTART IDENTITY CASCADE")
                .executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE library.transaction_outbox RESTART IDENTITY CASCADE")
                .executeUpdate();
        outboxRepository.deleteAll();
    }

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
                        jsonPath("$.supplierCode").value("245")
                );

        assertThat(outboxRepository.findAll()).hasSize(1);

        messageBrokerService.sendMessages();

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> outboxRepository.findAll().isEmpty());

        assertThat(outboxRepository.findAll()).hasSize(0);
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
                        jsonPath("$.content[*].id").value(Matchers.containsInAnyOrder(1, 2, 3)),
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
        var id = materialService.save(material).getId();

        mockMvc.perform(put("/api/v1/library-service/materials/{id}", 1L)
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
