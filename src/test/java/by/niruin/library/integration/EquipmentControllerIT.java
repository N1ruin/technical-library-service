package by.niruin.library.integration;

import by.niruin.library.domain.Equipment;
import by.niruin.library.domain.EquipmentType;
import by.niruin.library.model.error.ErrorResponse;
import by.niruin.library.model.file.UploadFileResponse;
import by.niruin.library.repository.EquipmentRepository;
import by.niruin.library.repository.TransactionOutboxRepository;
import by.niruin.library.service.EquipmentService;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;
import wiremock.org.eclipse.jetty.http.HttpHeader;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WireMockTest
public class EquipmentControllerIT extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private TransactionOutboxRepository outboxRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @RegisterExtension
    static WireMockExtension fileServiceWireMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void overrideFeignProperties(DynamicPropertyRegistry registry) {
        registry.add("file-service.url", fileServiceWireMock::baseUrl);
    }

    @BeforeEach
    void cleanDatabase() {
        equipmentRepository.deleteAll();
        outboxRepository.deleteAll();
        fileServiceWireMock.resetAll();
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void save_shouldReturnSavedEquipment() throws Exception {
        var multipartFile = getValidMultipartFile();
        var generatedFileName = UUID.randomUUID() + ".png";
        var uploadResponse = new UploadFileResponse(generatedFileName);
        var responseBody = objectMapper.writeValueAsString(uploadResponse);

        fileServiceWireMock.stubFor(post("/api/v1/file-service/files/images")
                .withHeader(HttpHeader.CONTENT_TYPE.toString(), containing(MediaType.MULTIPART_FORM_DATA_VALUE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeader.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/library-service/equipments")
                        .file(multipartFile)
                        .param("name", "Гайковерт")
                        .param("index", "2125PTi")
                        .param("description", "Максимальный момент затяжки до 300 Нм")
                        .param("type", EquipmentType.ASSEMBLY.name()))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").exists(),
                        jsonPath("$.name").value("Гайковерт"),
                        jsonPath("$.index").value("2125PTi"),
                        jsonPath("$.type").value(EquipmentType.ASSEMBLY.name()),
                        jsonPath("$.imageName").value(generatedFileName));

        assertThat(outboxRepository.findAll()).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void save_shouldReturnSavedEquipmentWithoutFile() throws Exception {
        var equipment = getEquipment();

        mockMvc.perform(multipart("/api/v1/library-service/equipments")
                        .param("name", equipment.getName())
                        .param("index", equipment.getIndex())
                        .param("description", equipment.getDescription())
                        .param("type", equipment.getType().name()))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").exists(),
                        jsonPath("$.name").value(equipment.getName()),
                        jsonPath("$.index").value(equipment.getIndex()),
                        jsonPath("$.description").value(equipment.getDescription()),
                        jsonPath("$.type").value(EquipmentType.ASSEMBLY.name()),
                        jsonPath("$.imageName").doesNotExist());

        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void save_throwsInvalidFileFormatException() throws Exception {
        var equipment = getEquipment();
        var multipartFile = getInvalidFormatMultipartFile();
        var errorResponseStub = new ErrorResponse(HttpStatus.BAD_REQUEST.toString(), "Error message",
                HttpStatus.BAD_REQUEST.value());
        var responseJson = objectMapper.writeValueAsString(errorResponseStub);
        fileServiceWireMock.stubFor(post("/api/v1/file-service/files/images")
                .withHeader(HttpHeader.CONTENT_TYPE.toString(), containing(MediaType.MULTIPART_FORM_DATA_VALUE))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeader.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseJson)));

        mockMvc.perform(multipart("/api/v1/library-service/equipments")
                        .file(multipartFile)
                        .param("name", equipment.getName())
                        .param("index", equipment.getIndex())
                        .param("description", equipment.getDescription())
                        .param("type", equipment.getType().name()))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400));

        assertThat(outboxRepository.findAll()).hasSize(0);
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void save_throwsValidationException() throws Exception {
        var multipartFile = getValidMultipartFile();

        mockMvc.perform(multipart("/api/v1/library-service/equipments")
                        .file(multipartFile)
                        .param("name", "fgdasfads3215432fdsafasvasfasdwqrweq$#%^$@@#")
                        .param("index", "||/fdas123")
                        .param("description", "Максимальный момент затяжки до 300 Нм")
                        .param("type", "TEST"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400));

        assertThat(outboxRepository.findAll()).hasSize(0);
    }

    @Test
    void findById_shouldReturnEquipment_andSaveImageInMinio() throws Exception {
        var equipment = getEquipment();
        var generatedFileName = UUID.randomUUID() + ".png";
        equipment.setImageName(generatedFileName);
        var saved = equipmentRepository.save(equipment);

        mockMvc.perform(get("/api/v1/library-service/equipments/{id}", saved.getId())
                        .with(jwt()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(saved.getId()),
                        jsonPath("$.name").value(saved.getName()),
                        jsonPath("$.index").value(saved.getIndex()),
                        jsonPath("$.type").value(saved.getType().name()),
                        jsonPath("$.description").value(saved.getDescription()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.imageName").value(saved.getImageName()));
    }

    @Test
    void findById_throwsNotFoundException() throws Exception {
        mockMvc.perform(get("/api/v1/library-service/equipments/{id}", 999L)
                        .with(jwt()))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404));
    }

    @Test
    void findById_ShouldReturnEquipmentWithoutFile() throws Exception {
        var equipment = equipmentService.save(getEquipment(), null);

        mockMvc.perform(get("/api/v1/library-service/equipments/{id}", equipment.getId())
                        .with(jwt()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(equipment.getId()),
                        jsonPath("$.name").value(equipment.getName()),
                        jsonPath("$.index").value(equipment.getIndex()),
                        jsonPath("$.type").value(equipment.getType().name()),
                        jsonPath("$.description").value(equipment.getDescription()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.imageName").value(nullValue()));
    }

    @Test
    void findAll_shouldReturnTwoEquipments() throws Exception {
        var equipmentOne = getEquipment();
        var equipmentTwo = getEquipment();
        equipmentTwo.setIndex("Test equipment");
        equipmentRepository.save(equipmentOne);
        equipmentRepository.save(equipmentTwo);

        mockMvc.perform(get("/api/v1/library-service/equipments")
                        .with(jwt()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.content.length()").value(2),
                        jsonPath("$.content[*].id").value(hasSize(2)));
    }

    @Test
    void findAll_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/library-service/equipments")
                        .with(jwt()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.content.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void update_textOnly_shouldKeepImage() throws Exception {
        var equipment = getEquipment();
        var generatedFileName = UUID.randomUUID() + ".png";
        equipment.setImageName(generatedFileName);
        var saved = equipmentRepository.save(equipment);
        outboxRepository.deleteAll();

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/library-service/equipments/{id}", equipment.getId())
                        .param("name", "New name")
                        .param("index", "New Index")
                        .param("description", "New Description")
                        .param("type", EquipmentType.ASSEMBLY.name()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.imageName").value(saved.getImageName()),
                        jsonPath("$.name").value("New name"),
                        jsonPath("$.description").value("New Description"),
                        jsonPath("$.type").value(EquipmentType.ASSEMBLY.name()));

        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void update_replaceFile_success() throws Exception {
        var existingFileName = UUID.randomUUID() + ".png";
        var equipment = getEquipment();
        equipment.setImageName(existingFileName);
        var saved = equipmentRepository.save(equipment);
        var generatedImageName = UUID.randomUUID() + ".png";
        var newFile = new MockMultipartFile(
                "file",
                "new-file.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "new-content-test".getBytes());
        var uploadFileResponse = new UploadFileResponse(generatedImageName);
        var responseJson = objectMapper.writeValueAsString(uploadFileResponse);
        outboxRepository.deleteAll();

        fileServiceWireMock.stubFor(
                post(urlPathEqualTo("/api/v1/file-service/files/images"))
                        .withHeader(HttpHeader.CONTENT_TYPE.toString(), containing(MediaType.MULTIPART_FORM_DATA_VALUE))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.CREATED.value())
                                .withHeader(HttpHeader.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON_VALUE)
                                .withBody(responseJson)));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/library-service/equipments/{id}", saved.getId())
                        .param("name", saved.getName())
                        .param("index", saved.getIndex())
                        .param("description", saved.getDescription())
                        .param("type", saved.getType().name())
                        .file(newFile))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.imageName").value(generatedImageName),
                        jsonPath("$.index").value(saved.getIndex()),
                        jsonPath("$.name").value(saved.getName()),
                        jsonPath("$.description").value(saved.getDescription()),
                        jsonPath("$.type").value(saved.getType().name()));

        assertThat(outboxRepository.findAll()).hasSize(3);
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void update_shouldReturnErrorResponse_dueToNotFoundException() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/library-service/equipments/{id}", 1L)
                        .param("name", "New name")
                        .param("index", "New Index")
                        .param("description", "New Description")
                        .param("type", EquipmentType.ASSEMBLY.name()))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404));

        assertThat(outboxRepository.findAll()).hasSize(0);
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void update_shouldThrowValidationEx() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/library-service/equipments/{id}", 999L)
                        .param("name", "New name321&%$@#()*$()#@")
                        .param("index", "0432fdsZop^%#$@)(")
                        .param("description", "{}{][)_}")
                        .param("type", "TEST"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400));

        assertThat(outboxRepository.findAll()).hasSize(0);
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void delete_success() throws Exception {
        var equipment = getEquipment();
        var multipartFile = getValidMultipartFile();
        var generatedFileName = UUID.randomUUID() + ".png";
        var uploadResponse = new UploadFileResponse(generatedFileName);
        var responseBody = objectMapper.writeValueAsString(uploadResponse);
        fileServiceWireMock.stubFor(post("/api/v1/file-service/files/images")
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
        var saved = equipmentService.save(equipment, multipartFile);
        outboxRepository.deleteAll();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/library-service/equipments/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(outboxRepository.findAll()).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    void delete_shouldReturnErrorResponse_dueToNotFoundException() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.delete("/api/v1/library-service/equipments/{id}", 1L);
        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404));

        assertThat(outboxRepository.findAll()).hasSize(0);
    }

    private MockMultipartFile getValidMultipartFile() {
        return new MockMultipartFile(
                "file",
                "pneumatic-wrench.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test-content".getBytes());
    }

    private MockMultipartFile getInvalidFormatMultipartFile() {
        return new MockMultipartFile(
                "file",
                "pneumatic-wrench.txt",
                MediaType.IMAGE_JPEG_VALUE,
                "test-content".getBytes());
    }

    private Equipment getEquipment() {
        var equipment = new Equipment();
        equipment.setName("Гайковерт");
        equipment.setIndex("2125QXPA");
        equipment.setDescription("Тестовое описание");
        equipment.setType(EquipmentType.ASSEMBLY);

        return equipment;
    }
}
