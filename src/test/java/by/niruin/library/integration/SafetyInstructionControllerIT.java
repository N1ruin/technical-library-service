package by.niruin.library.integration;

import by.niruin.library.domain.SafetyInstruction;
import by.niruin.library.repository.SafetyInstructionRepository;
import by.niruin.library.repository.TransactionOutboxRepository;
import by.niruin.library.service.SafetyInstructionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SafetyInstructionControllerIT extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SafetyInstructionService safetyInstructionService;
    @Autowired
    private SafetyInstructionRepository instructionRepository;
    @Autowired
    private TransactionOutboxRepository outboxRepository;

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
        instructionRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Test
    void saveInstruction_shouldReturnCreatedInstruction() throws Exception {
        var requestBuilder = post("/api/v1/library-service/safety-instructions")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_INSTRUCTION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isCreated(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json(VALID_INSTRUCTION_JSON),
                        jsonPath("$.id").exists(),
                        jsonPath("$.number").value("106п"),
                        jsonPath("$.description").value("Тестовое описание"));

        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    void saveInstruction_throwsEntityAlreadyExist() throws Exception {
        var requestBuilder = post("/api/v1/library-service/safety-instructions")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_INSTRUCTION_JSON);
        var instruction = new SafetyInstruction();
        instruction.setNumber("106п");
        instruction.setDescription("Тестовое описание");
        safetyInstructionService.save(instruction);
        outboxRepository.deleteAll();

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isConflict(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(409));

        assertThat(outboxRepository.findAll()).hasSize(0);
    }

    @Test
    void saveInstruction_throwsValidationException() throws Exception {
        var requestBuilder = post("/api/v1/library-service/safety-instructions")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_INSTRUCTION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400));

        assertThat(outboxRepository.findAll()).hasSize(0);
    }

    @Test
    void findById_shouldReturnInstruction() throws Exception {
        var instructionId = safetyInstructionService.save(createInstruction()).getId();

        var requestBuilder = get("/api/v1/library-service/safety-instructions/{id}", instructionId)
                .with(jwt());

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isOk(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.id").value(instructionId),
                        jsonPath("$.number").exists(),
                        jsonPath("$.description").value("Тестовое описание"),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists());
    }

    @Test
    void findById_throwsEntityNotFound() throws Exception {
        var requestBuilder = get("/api/v1/library-service/safety-instructions/{id}", 999L)
                .with(jwt());

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404));
    }

    @Test
    void findAll_shouldReturnInstructionListWithSizeThree() throws Exception {
        var requestBuilder = get("/api/v1/library-service/safety-instructions")
                .with(jwt());
        safetyInstructionService.save(createInstruction());
        safetyInstructionService.save(createInstruction());
        safetyInstructionService.save(createInstruction());

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isOk(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.content[0].id").exists(),
                        jsonPath("$.content[0].number").exists(),
                        jsonPath("$.content[0].description").exists(),
                        jsonPath("$.content[0].createdDate").exists(),
                        jsonPath("$.content[0].updatedDate").exists(),
                        jsonPath("$.content[1].id").exists(),
                        jsonPath("$.content[1].number").exists(),
                        jsonPath("$.content[1].description").exists(),
                        jsonPath("$.content[1].createdDate").exists(),
                        jsonPath("$.content[1].updatedDate").exists(),
                        jsonPath("$.content[2].id").exists(),
                        jsonPath("$.content[2].number").exists(),
                        jsonPath("$.content[2].description").exists(),
                        jsonPath("$.content[2].createdDate").exists(),
                        jsonPath("$.content[2].updatedDate").exists());
    }

    @Test
    void deleteById_success() throws Exception {
        var instructionId = safetyInstructionService.save(createInstruction()).getId();
        var requestBuilder = delete("/api/v1/library-service/safety-instructions/{id}", instructionId)
                .with(jwt());
        outboxRepository.deleteAll();

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNoContent());

        assertThat(outboxRepository.findAll()).hasSize(1);
        assertThat(safetyInstructionService.findAll(PageRequest.of(1, 10)).get().count())
                .isEqualTo(0);
    }

    @Test
    void deleteById_shouldReturnNotFound_whenMaterialDoesNotExist() throws Exception {
        var requestBuilder = delete("/api/v1/library-service/safety-instructions/{id}", 1L)
                .with(jwt());

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404));

        assertThat(outboxRepository.findAll()).hasSize(0);
    }

    @Test
    void update_success() throws Exception {
        var instruction = safetyInstructionService.save(createInstruction());
        instruction.setNumber("111");
        instruction.setDescription("Тестовое описание 123");
        outboxRepository.deleteAll();
        var requestBuilder = put("/api/v1/library-service/safety-instructions/{id}", instruction.getId())
                .with(jwt())
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
                        jsonPath("$.updatedDate").exists());

        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    void update_throwsEntityNotFound() throws Exception {
        var requestBuilder = put("/api/v1/library-service/safety-instructions/{id}", 999L)
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_INSTRUCTION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404));

        assertThat(outboxRepository.findAll()).hasSize(0);
    }

    @Test
    void update_throwsValidationException() throws Exception {
        safetyInstructionService.save(createInstruction());
        var requestBuilder = put("/api/v1/library-service/safety-instructions/{id}", 1L)
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_INSTRUCTION_JSON);
        outboxRepository.deleteAll();

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400));

        assertThat(outboxRepository.findAll()).hasSize(0);
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
        outboxRepository.deleteAll();

        var requestBuilder = put("/api/v1/library-service/safety-instructions/{id}", saved.getId())
                .with(jwt())
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
                        jsonPath("$.code").value(409));

        assertThat(outboxRepository.findAll()).hasSize(0);
    }

    private SafetyInstruction createInstruction() {
        var instruction = new SafetyInstruction();
        instruction.setNumber(UUID.randomUUID().toString().substring(0, 9));
        instruction.setDescription("Тестовое описание");

        return instruction;
    }
}
