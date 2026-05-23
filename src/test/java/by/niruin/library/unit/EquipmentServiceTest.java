package by.niruin.library.unit;

import by.niruin.library.client.FileClient;
import by.niruin.library.domain.Equipment;
import by.niruin.library.domain.EquipmentType;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.kafka.EventPublisher;
import by.niruin.library.model.equipment.UpdateEquipmentRequest;
import by.niruin.library.model.file.UploadFileResponse;
import by.niruin.library.repository.EquipmentRepository;
import by.niruin.library.service.EquipmentService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EquipmentServiceTest {
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private FileClient fileClient;
    @Mock
    private TransactionTemplate transactionTemplate;
    @InjectMocks
    private EquipmentService equipmentService;

    @BeforeEach
    void setUpTransactionalTemplateMock() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void saveSuccess_shouldReturnSavedEquipment_withFile() {
        var equipment = createTestEquipment();
        var image = new MockMultipartFile("file", new byte[10]);
        var savedFileName = UUID.randomUUID() + ".png";
        var fileUploadResponse = new UploadFileResponse(savedFileName);
        when(fileClient.uploadImage(image)).thenReturn(fileUploadResponse);
        when(equipmentRepository.existsByIndex(equipment.getIndex())).thenReturn(false);
        when(equipmentRepository.save(equipment)).thenReturn(equipment);

        var result = equipmentService.save(equipment, image);

        assertThat(result).isNotNull();
        assertThat(result.getImageName()).isEqualTo(savedFileName);
        verify(fileClient).uploadImage(image);
        verify(equipmentRepository).existsByIndex(equipment.getIndex());
        verify(eventPublisher).publishEquipmentSavedEvent(equipment);
        verify(eventPublisher).publishFileMovedToPermanentStorageEvent(savedFileName);
    }

    @Test
    void save_shouldThrowEntityAlreadyExist() {
        var equipment = createTestEquipment();
        when(equipmentRepository.existsByIndex(equipment.getIndex()))
                .thenReturn(true);

        assertThatThrownBy(() -> equipmentService.save(equipment, null))
                .isInstanceOf(EntityAlreadyExistException.class);

        verifyNoInteractions(fileClient);
        verify(equipmentRepository, never()).save(equipment);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void save_shouldReturnSavedEquipment_WithoutFile() {
        var equipment = createTestEquipment();
        when(equipmentRepository.existsByIndex(equipment.getIndex())).thenReturn(false);
        when(equipmentRepository.save(equipment)).thenReturn(equipment);

        var saved = equipmentService.save(equipment, null);

        assertThat(saved.getImageName()).isNull();
        assertThat(saved).usingRecursiveComparison().isEqualTo(equipment);
        verify(eventPublisher, never()).publishFileMovedToPermanentStorageEvent(saved.getImageName());
    }

    @Test
    void save_shouldThrowFeignEx() {
        var equipment = createTestEquipment();
        var image = new MockMultipartFile("file", new byte[10]);
        when(fileClient.uploadImage(image))
                .thenThrow(FeignException.class);

        assertThatThrownBy(() -> equipmentService.save(equipment, image))
                .isInstanceOf(FeignException.class);

        verify(fileClient).uploadImage(image);
        verify(equipmentRepository, never()).existsByIndex(equipment.getIndex());
        verify(equipmentRepository, never()).save(equipment);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void findById_shouldReturnEquipment() {
        var equipment = createTestEquipment();
        when(equipmentRepository.findById(anyLong()))
                .thenReturn(Optional.of(equipment));

        var result = equipmentService.findById(1L);

        assertThat(equipment).usingRecursiveComparison()
                .isEqualTo(result);
    }

    @Test
    void findById_shouldThrowEntityNotFound() {
        var id = 1L;
        when(equipmentRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.findById(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAll_shouldReturnEmptyList() {
        when(equipmentRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());
        var pageRequest = PageRequest.of(0, 10);

        var equipmentPage = equipmentService.findAll(pageRequest);

        assertThat(equipmentPage).hasSize(0);
        verify(equipmentRepository).findAll(any(PageRequest.class));
    }

    @Test
    void findAll_shouldReturnListWithSize1() {
        var equipment = createTestEquipment();
        var expectedPage = new PageImpl<>(List.of(equipment));
        when(equipmentRepository.findAll(any(Pageable.class)))
                .thenReturn(expectedPage);
        var pageRequest = PageRequest.of(0, 10);

        var equipmentPage = equipmentService.findAll(pageRequest);

        assertThat(equipmentPage).hasSize(1);
        assertThat(equipmentPage.getContent().getFirst()).usingRecursiveComparison()
                .isEqualTo(equipment);
        verify(equipmentRepository).findAll(any(PageRequest.class));
    }

    @Test
    void update_shouldReturnUpdatedMaterial() {
        var id = 1L;
        var oldFileName = "old-image.png";
        var savedFileName = UUID.randomUUID() + ".png";
        var fileUploadResponse = new UploadFileResponse(savedFileName);
        var equipment = createTestEquipment();
        equipment.setImageName(oldFileName);
        var mockFile = new MockMultipartFile(
                "file",
                "image.png",
                MediaType.IMAGE_JPEG.toString(),
                new byte[10]);
        var updateRequest = createUpdateEquipmentRequest(mockFile);
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.of(equipment));
        when(equipmentRepository.existsByIndex(updateRequest.index()))
                .thenReturn(false);
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.of(equipment));
        when(equipmentRepository.existsByIndex(updateRequest.index()))
                .thenReturn(false);
        when(fileClient.uploadImage(mockFile))
                .thenReturn(fileUploadResponse);

        var result = equipmentService.update(id, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getImageName()).isEqualTo(fileUploadResponse.fileName());
        verify(equipmentRepository).findById(id);
        verify(equipmentRepository).existsByIndex(updateRequest.index());
        verify(fileClient).uploadImage(mockFile);
        verify(eventPublisher).publishEquipmentUpdatedEvent(equipment);
        verify(eventPublisher).publishFileDeletedEvent(oldFileName);
        verify(eventPublisher).publishFileMovedToPermanentStorageEvent(result.getImageName());
    }

    @Test
    void update_shouldReturnUpdatedEquipment_withoutNewFile() {
        var id = 1L;
        var oldFileName = "old-image.png";
        var equipment = createTestEquipment();
        equipment.setImageName(oldFileName);
        var updateRequest = createUpdateEquipmentRequest(null);
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.of(equipment));
        when(equipmentRepository.existsByIndex(updateRequest.index()))
                .thenReturn(false);

        var result = equipmentService.update(id, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getImageName()).isEqualTo(oldFileName);
        verify(equipmentRepository).findById(id);
        verify(equipmentRepository).existsByIndex(updateRequest.index());
        verifyNoInteractions(fileClient);
        verify(eventPublisher).publishEquipmentUpdatedEvent(equipment);
        verify(eventPublisher, never()).publishFileDeletedEvent(oldFileName);
        verify(eventPublisher, never()).publishFileMovedToPermanentStorageEvent(result.getImageName());
    }

    @Test
    void update_shouldThrowEntityNotFound() {
        var id = 1L;
        var updateRequest = createUpdateEquipmentRequest(null);
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class);

        verify(equipmentRepository).findById(id);
        verify(equipmentRepository, never()).existsByIndex(updateRequest.index());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void update_shouldThrowEntityAlreadyExist_whenIndexAlreadyExist() {
        var id = 1L;
        var equipment = createTestEquipment();
        equipment.setIndex("old-index");
        var updateRequest = createUpdateEquipmentRequest(null);
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.of(equipment));
        when(equipmentRepository.existsByIndex(updateRequest.index()))
                .thenReturn(true);

        assertThatThrownBy(() -> equipmentService.update(id, updateRequest))
                .isInstanceOf(EntityAlreadyExistException.class);

        verify(equipmentRepository).findById(id);
        verify(equipmentRepository).existsByIndex(updateRequest.index());
        verify(equipmentRepository, never()).save(any());
        verifyNoInteractions(fileClient);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void update_shouldSuccess_whenIndexHasNotChanged() {
        var id = 1L;
        var equipment = createTestEquipment();
        var updateRequest = createUpdateEquipmentRequest(null);
        equipment.setIndex(updateRequest.index());
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.of(equipment));

        var result = equipmentService.update(id, updateRequest);

        assertThat(result).isNotNull();
        verify(equipmentRepository).findById(id);
        verify(equipmentRepository, never()).existsByIndex(anyString());
        verify(eventPublisher).publishEquipmentUpdatedEvent(equipment);
    }

    @Test
    void update_shouldNotCreateDeleteOutbox_whenOldImageWasNull() {
        var id = 1L;
        var savedFileName = UUID.randomUUID() + ".png";
        var fileUploadResponse = new UploadFileResponse(savedFileName);
        var equipment = createTestEquipment();
        equipment.setImageName(null);
        var mockFile = new MockMultipartFile("file", "image.png", MediaType.IMAGE_JPEG.toString(), new byte[10]);
        var updateRequest = createUpdateEquipmentRequest(mockFile);
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.of(equipment));
        when(equipmentRepository.existsByIndex(updateRequest.index()))
                .thenReturn(false);
        when(fileClient.uploadImage(mockFile))
                .thenReturn(fileUploadResponse);

        var result = equipmentService.update(id, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getImageName()).isEqualTo(fileUploadResponse.fileName());
        verify(equipmentRepository).findById(id);
        verify(equipmentRepository).existsByIndex(updateRequest.index());
        verify(fileClient).uploadImage(mockFile);
        verify(eventPublisher).publishEquipmentUpdatedEvent(result);
        verify(eventPublisher, never()).publishFileDeletedEvent(anyString());
    }

    @Test
    void deleteById_shouldDeleteSuccess() {
        var id = 1L;
        var equipment = createTestEquipment();
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.of(equipment));

        equipmentService.delete(id);

        verify(eventPublisher).publishEquipmentDeletedEvent(equipment);
        verify(equipmentRepository).findById(id);
    }

    @Test
    void deleteById_shouldThrowEntityNotFound() {
        var id = 1L;
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);
        verify(equipmentRepository).findById(id);
        verify(equipmentRepository, never()).deleteById(id);
        verifyNoInteractions(eventPublisher);
    }

    private Equipment createTestEquipment() {
        var equipment = new Equipment();
        equipment.setType(EquipmentType.ASSEMBLY);
        equipment.setDescription("Тестовое описание");
        equipment.setIndex("1234-1235");
        equipment.setName("Ключ");

        return equipment;
    }

    private UpdateEquipmentRequest createUpdateEquipmentRequest(MultipartFile file) {
        var newType = EquipmentType.CONTROL;
        var newDescription = "Новое описание";
        var newIndex = "1231-9999";
        var newName = "Новое название";

        return new UpdateEquipmentRequest(newName, newIndex, newDescription, file, newType.name());
    }
}
