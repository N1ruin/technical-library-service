package by.niruin.library.integration;

import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.service.SafetyInstructionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class SafetyInstructionControllerIT extends BaseTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SafetyInstructionService safetyInstructionService;
    @Autowired
    private EntityManager em;

    private static final String VALID_INSTRUCTION_JSON = """
            {
            "number": "106п",
            "description": "Тестовое описание"
            }
            """;
    private static final String INVALID_INSTRUCTION_JSON = """
            {
            "number": "3fdsaFSAD321",
            "description": null
            }
            """;

    @BeforeEach
    void cleanDatabase() {
        em.createNativeQuery("TRUNCATE TABLE library.safety_instruction RESTART IDENTITY CASCADE")
                .executeUpdate();
    }

    @Test
    void saveInstruction_shouldReturnCreatedInstruction() throws Exception {
        var requestBuilder = post("/api/v1/library-service/safety-instructions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_INSTRUCTION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isCreated(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json(VALID_INSTRUCTION_JSON),
                        jsonPath("$.id").value(1),
                        jsonPath("$.number").value("106п"),
                        jsonPath("$.description").value("Тестовое описание")
                );
    }

    @Test
    void saveInstruction_throwsEntityAlreadyExist() throws Exception {
        var requestBuilder = post("/api/v1/library-service/safety-instructions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_INSTRUCTION_JSON);
        var instruction = new SafetyInstruction();
        instruction.setNumber("106п");
        instruction.setDescription("Тестовое описание");
        safetyInstructionService.save(instruction);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isConflict(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(409)
                );
    }

    @Test
    void saveInstruction_throwsValidationException() throws Exception {
        var requestBuilder = post("/api/v1/library-service/safety-instructions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_INSTRUCTION_JSON);

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
    void findById_shouldReturnInstruction() throws Exception {
        var instructionId = safetyInstructionService.save(createInstruction()).getId();

        var requestBuilder = get("/api/v1/library-service/safety-instructions/{id}", instructionId);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isOk(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.id").value(1L),
                        jsonPath("$.number").exists(),
                        jsonPath("$.description").value("Тестовое описание"),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists()
                );
    }

    @Test
    void findById_throwsEntityNotFound() throws Exception {
        var requestBuilder = get("/api/v1/library-service/safety-instructions/{id}", 1L);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404)
                );
    }

    @Test
    void findAll_shouldReturnInstructionListWithSizeThree() throws Exception {
        var requestBuilder = get("/api/v1/library-service/safety-instructions");
        safetyInstructionService.save(createInstruction());
        safetyInstructionService.save(createInstruction());
        safetyInstructionService.save(createInstruction());

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isOk(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$[0].id").value(1L),
                        jsonPath("$[0].number").exists(),
                        jsonPath("$[0].description").value("Тестовое описание"),
                        jsonPath("$[0].createdDate").exists(),
                        jsonPath("$[0].updatedDate").exists(),
                        jsonPath("$[1].id").value(2L),
                        jsonPath("$[1].number").exists(),
                        jsonPath("$[1].description").value("Тестовое описание"),
                        jsonPath("$[1].createdDate").exists(),
                        jsonPath("$[1].updatedDate").exists(),
                        jsonPath("$[2].id").value(3L),
                        jsonPath("$[2].number").exists(),
                        jsonPath("$[2].description").value("Тестовое описание"),
                        jsonPath("$[2].createdDate").exists(),
                        jsonPath("$[2].updatedDate").exists()
                );
    }

    @Test
    void deleteById_success() throws Exception {
        var instructionId = safetyInstructionService.save(createInstruction()).getId();
        var requestBuilder = delete("/api/v1/library-service/safety-instructions/{id}", instructionId);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNoContent()
                );

        assertEquals(0, safetyInstructionService.findAll().size());
    }

    @Test
    void deleteById_shouldReturnNotFound_whenMaterialDoesNotExist() throws Exception {
        var requestBuilder = delete("/api/v1/library-service/safety-instructions/{id}", 1L);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404)
                );
    }

    @Test
    void update_success() throws Exception {
        var instruction = safetyInstructionService.save(createInstruction());
        instruction.setNumber("111");
        instruction.setDescription("Тестовое описание 123");

        var requestBuilder = put("/api/v1/library-service/safety-instructions/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_INSTRUCTION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isOk(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json("""
                                {
                                    "number": "106п",
                                    "description": "Тестовое описание"
                                }
                                """),
                        jsonPath("$.id").value(instruction.getId()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists()
                );
    }

    @Test
    void update_throwsEntityNotFound() throws Exception {
        var requestBuilder = put("/api/v1/library-service/safety-instructions/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_INSTRUCTION_JSON);

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
    void update_throwsValidationException() throws Exception {
        safetyInstructionService.save(createInstruction());
        var requestBuilder = put("/api/v1/library-service/safety-instructions/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_INSTRUCTION_JSON);

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
    void update_throwsEntityAlreadyExist() throws Exception {
        var first = new SafetyInstruction();
        first.setNumber("111");
        first.setDescription("Я первая");
        safetyInstructionService.save(first);

        var toUpdate = new SafetyInstruction();
        toUpdate.setNumber("777");
        toUpdate.setDescription("Я вторая");
        var saved = safetyInstructionService.save(toUpdate);

        var requestBuilder = put("/api/v1/library-service/safety-instructions/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "number": "111",
                            "description": "Тестовое описание"
                        }
                        """);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isConflict(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(409)
                );
    }

    private SafetyInstruction createInstruction() {
        var instruction = new SafetyInstruction();
        instruction.setNumber(UUID.randomUUID().toString().substring(0, 9));
        instruction.setDescription("Тестовое описание");

        return instruction;
    }
}
