package by.niruin.library.integration;

import by.niruin.library.domain.Equipment;
import by.niruin.library.domain.EquipmentType;
import by.niruin.library.service.EquipmentService;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MinIOContainer;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

public class EquipmentControllerIT extends BaseTest {
    public static final String MINIO_BUCKET_NAME = "equipments";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private EntityManager em;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    static MinIOContainer minIOContainer;

    @BeforeEach
    void cleanDatabase() throws MinioException {
        em.createNativeQuery("TRUNCATE TABLE library.equipment RESTART IDENTITY CASCADE")
                .executeUpdate();
        em.flush();

        for (var file : getAllFiles()) {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(MINIO_BUCKET_NAME)
                            .object(file.get().objectName())
                            .build()
            );
        }
    }

    @Test
    void save_shouldReturnSavedEquipment() throws Exception {
        var multipartFile = getValidMultipartFile();

        mockMvc.perform(multipart(HttpMethod.POST, "/api/v1/library-service/equipments")
                        .file(multipartFile)
                        .param("name", "Гайковерт")
                        .param("index", "2125PTi")
                        .param("description", "Максимальный момент затяжки до 300 Нм")
                        .param("type", "ASSEMBLY"))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(1L),
                        jsonPath("$.name").value("Гайковерт"),
                        jsonPath("$.index").value("2125PTi"),
                        jsonPath("$.type").value("ASSEMBLY"),
                        jsonPath("$.imageName").exists()
                );

        var saved = equipmentService.findById(1L);
        assertDoesNotThrow(() -> minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(MINIO_BUCKET_NAME)
                        .object(saved.getImageName())
                        .build()));
    }

    @Test
    void save_shouldReturnSavedEquipmentWithoutFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/library-service/equipments")
                        .param("name", "Гайковерт")
                        .param("index", "2125PTi")
                        .param("description", "Максимальный момент затяжки до 300 Нм")
                        .param("type", "ASSEMBLY"))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(1L),
                        jsonPath("$.name").value("Гайковерт"),
                        jsonPath("$.index").value("2125PTi"),
                        jsonPath("$.type").value("ASSEMBLY")
                );

        var saved = equipmentService.findById(1L);
        assertNull(saved.getImageName());

        var filesInMinio = getAllFiles();
        assertFalse(filesInMinio.iterator().hasNext());
    }

    @Test
    void save_throwsInvalidFileFormatException() throws Exception {
        var multipartFile = getInvalidFormatMultipartFile();

        mockMvc.perform(multipart("/api/v1/library-service/equipments")
                        .file(multipartFile)
                        .param("name", "Гайковерт")
                        .param("index", "2125PTi")
                        .param("description", "Максимальный момент затяжки до 300 Нм")
                        .param("type", "ASSEMBLY"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400)
                );

        assertFalse(getAllFiles().iterator().hasNext());
    }

    @Test
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
                        jsonPath("$.code").value(400)
                );

        assertFalse(getAllFiles().iterator().hasNext());
    }

    @Test
    void findById_shouldReturnEquipment_andSaveImageInMinio() throws Exception {
        var equipment = equipmentService.save(getEquipment(), getValidMultipartFile());

        mockMvc.perform(get("/api/v1/library-service/equipments/{id}", equipment.getId()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(equipment.getId()),
                        jsonPath("$.name").value(equipment.getName()),
                        jsonPath("$.index").value(equipment.getIndex()),
                        jsonPath("$.type").value(equipment.getType().name()),
                        jsonPath("$.description").value(equipment.getDescription()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.imageName").value(equipment.getImageName())
                );

        assertDoesNotThrow(() ->
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(MINIO_BUCKET_NAME)
                                .object(equipment.getImageName())
                                .build()
                ));
    }

    @Test
    void findById_throwsNotFoundException() throws Exception {
        mockMvc.perform(get("/api/v1/library-service/equipments/{id}", 1L))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404)
                );
    }

    @Test
    void findById_ShouldReturnEquipmentWithoutFile() throws Exception {
        var equipment = equipmentService.save(getEquipment(), null);

        mockMvc.perform(get("/api/v1/library-service/equipments/{id}", equipment.getId()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(equipment.getId()),
                        jsonPath("$.name").value(equipment.getName()),
                        jsonPath("$.index").value(equipment.getIndex()),
                        jsonPath("$.type").value(equipment.getType().name()),
                        jsonPath("$.description").value(equipment.getDescription()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.imageName").value(nullValue())
                );
    }

    @Test
    void findAll_shouldReturnTwoEquipments() throws Exception {
        var equipmentOne = getEquipment();
        var equipmentTwo = getEquipment();
        equipmentTwo.setIndex("Test equipment");
        var savedOne = equipmentService.save(equipmentOne, getValidMultipartFile());
        var savedTwo = equipmentService.save(equipmentTwo, null);

        mockMvc.perform(get("/api/v1/library-service/equipments"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(2),
                        jsonPath("$[0].id").value(savedOne.getId()),
                        jsonPath("$[0].index").value(savedOne.getIndex()),
                        jsonPath("$[0].imageName").value(savedOne.getImageName()),
                        jsonPath("$[1].id").value(savedTwo.getId()),
                        jsonPath("$[1].index").value("Test equipment"),
                        jsonPath("$[1].imageName").value(nullValue())
                );
    }

    @Test
    void findAll_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/library-service/equipments"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(0)
                );
    }

    @Test
    void update_textOnly_shouldKeepImage() throws Exception {
        var equipment = equipmentService.save(getEquipment(), getValidMultipartFile());
        var fileName = equipment.getImageName();

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/library-service/equipments/{id}", equipment.getId())
                        .param("name", "New name")
                        .param("index", "New Index")
                        .param("description", "New Description")
                        .param("type", "ASSEMBLY"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.imageName").value(fileName),
                        jsonPath("$.name").value("New name"),
                        jsonPath("$.description").value("New Description"),
                        jsonPath("$.type").value("ASSEMBLY")
                );

        assertDoesNotThrow(() -> minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(MINIO_BUCKET_NAME)
                        .object(fileName)
                        .build()));
    }

    @Test
    void update_replaceFile_success() throws Exception {
        var equipment = equipmentService.save(getEquipment(), getValidMultipartFile());
        var fileName = equipment.getImageName();
        var newFile = new MockMultipartFile(
                "file",
                "new-file.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "new-content-test".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/library-service/equipments/{id}", equipment.getId())
                        .param("name", equipment.getName())
                        .param("index", equipment.getIndex())
                        .param("description", equipment.getDescription())
                        .param("type", equipment.getType().name())
                        .file(newFile))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.imageName").value(equipment.getImageName()),
                        jsonPath("$.index").value(equipment.getIndex()),
                        jsonPath("$.name").value(equipment.getName()),
                        jsonPath("$.description").value(equipment.getDescription()),
                        jsonPath("$.type").value(equipment.getType().name())
                );

        assertNotEquals(fileName, equipment.getImageName());

        assertDoesNotThrow(() -> minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(MINIO_BUCKET_NAME)
                        .object(equipment.getImageName())
                        .build()));

        assertThrows(MinioException.class, () ->
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(MINIO_BUCKET_NAME)
                                .object(fileName)
                                .build()));
    }

    @Test
    void update_shouldReturnErrorResponse_dueToNotFoundException() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/library-service/equipments/{id}", 1L)
                        .param("name", "New name")
                        .param("index", "New Index")
                        .param("description", "New Description")
                        .param("type", "ASSEMBLY"))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404)
                );
    }

    @Test
    void update_shouldReturnErrorResponse_dueToValidationException() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/library-service/equipments/{id}", 1L)
                        .param("name", "New name321&%$@#()*$()#@")
                        .param("index", "0432fdsZop^%#$@)(")
                        .param("description", "{}{][)_}")
                        .param("type", "TEST"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400)
                );
    }

    @Test
    void delete_success() throws Exception {
        var equipment = equipmentService.save(getEquipment(), getValidMultipartFile());
        var fileName = equipment.getImageName();
        var requestBuilder = MockMvcRequestBuilders.delete("/api/v1/library-service/equipments/{id}", 1L);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNoContent()
                );

        assertThrows(MinioException.class, () -> minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(MINIO_BUCKET_NAME)
                        .object(fileName)
                        .build()));
    }

    @Test
    void delete_shouldReturnErrorResponse_dueToNotFoundException() throws Exception {
        var requestBuilder = MockMvcRequestBuilders.delete("/api/v1/library-service/equipments/{id}", 1L);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isNotFound(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404)
                );
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

    private Iterable<Result<Item>> getAllFiles() {
        return minioClient.listObjects(
                ListObjectsArgs.builder().bucket(MINIO_BUCKET_NAME).build());
    }
}
