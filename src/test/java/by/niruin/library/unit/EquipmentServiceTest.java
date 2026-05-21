package by.niruin.library.unit;

import by.niruin.library.client.FileClient;
import by.niruin.library.domain.Equipment;
import by.niruin.library.domain.EquipmentType;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.mapper.EquipmentMapper;
import by.niruin.library.model.equipment.UpdateEquipmentRequest;
import by.niruin.library.model.event.EventType;
import by.niruin.library.model.file.UploadFileResponse;
import by.niruin.library.repository.EquipmentRepository;
import by.niruin.library.service.EquipmentService;
import by.niruin.library.service.TransactionOutboxService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EquipmentServiceTest {
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private TransactionOutboxService outboxService;
    @Mock
    private EquipmentMapper equipmentMapper;
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
        var equipmentCreatedOutboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.EQUIPMENT_CREATED), eq(equipment), any()))
                .thenReturn(equipmentCreatedOutboxRecord);

        var result = equipmentService.save(equipment, image);

        assertThat(result).isNotNull();
        assertThat(result.getImageName()).isEqualTo(savedFileName);
        verify(fileClient).uploadImage(image);
        verify(equipmentRepository).existsByIndex(equipment.getIndex());
        verify(outboxService).createOutboxRecord(eq(EventType.EQUIPMENT_CREATED), eq(equipment), any());
        verify(outboxService).createOutboxRecord(eq(EventType.FILE_MOVE_TO_PERMANENT_STORAGE), any(), any());
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
        verifyNoInteractions(outboxService);
    }

    @Test
    void save_shouldReturnSavedEquipment_WithoutFile() {
        var equipment = createTestEquipment();
        when(equipmentRepository.existsByIndex(equipment.getIndex())).thenReturn(false);
        var equipmentCreatedRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.EQUIPMENT_CREATED), eq(equipment), any()))
                .thenReturn(equipmentCreatedRecord);
        when(equipmentRepository.save(equipment)).thenReturn(equipment);

        var saved = equipmentService.save(equipment, null);

        assertThat(saved.getImageName()).isNull();
        assertThat(saved).usingRecursiveComparison().isEqualTo(equipment);
        verify(outboxService).save(equipmentCreatedRecord);
        verify(outboxService, never()).createOutboxRecord(eq(EventType.FILE_MOVE_TO_PERMANENT_STORAGE), eq(equipment), any());
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
        verifyNoInteractions(outboxService);
    }

    @Test
    void save_shouldThrowEx_whereOutboxServiceThrownEx() {
        var equipment = createTestEquipment();
        var image = new MockMultipartFile("file", new byte[10]);
        var savedFileName = UUID.randomUUID() + ".png";
        var fileUploadResponse = new UploadFileResponse(savedFileName);
        when(fileClient.uploadImage(image))
                .thenReturn(fileUploadResponse);
        when(equipmentRepository.existsByIndex(equipment.getIndex()))
                .thenReturn(false);
        when(equipmentRepository.save(equipment))
                .thenReturn(equipment);
        var outboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.EQUIPMENT_CREATED), eq(equipment), any()))
                .thenReturn(outboxRecord);
        doThrow(RuntimeException.class).when(outboxService).save(outboxRecord);

        assertThatThrownBy(() -> equipmentService.save(equipment, image))
                .isInstanceOf(RuntimeException.class);

        verify(equipmentRepository).existsByIndex(equipment.getIndex());
        verify(fileClient).uploadImage(image);
        verify(equipmentRepository).save(equipment);
        verify(outboxService).createOutboxRecord(eq(EventType.EQUIPMENT_CREATED), eq(equipment), any());
        verify(outboxService).save(outboxRecord);
        verify(outboxService, never()).createOutboxRecord(eq(EventType.FILE_MARKED_FOR_DELETION),
                eq(equipment.getImageName()), any());
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
        var updateOutboxRecord = mock(TransactionOutboxRecord.class);
        var deleteOutboxRecord = mock(TransactionOutboxRecord.class);
        var moveToPermanentStorage = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.EQUIPMENT_UPDATED), any(Equipment.class), any()))
                .thenReturn(updateOutboxRecord);
        when(outboxService.createOutboxRecord(eq(EventType.FILE_MARKED_FOR_DELETION), eq(oldFileName), any()))
                .thenReturn(deleteOutboxRecord);
        when(outboxService.createOutboxRecord(eq(EventType.FILE_MOVE_TO_PERMANENT_STORAGE), eq(savedFileName), any()))
                .thenReturn(moveToPermanentStorage);

        var result = equipmentService.update(id, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getImageName()).isEqualTo(fileUploadResponse.fileName());
        verify(equipmentRepository).findById(id);
        verify(equipmentRepository).existsByIndex(updateRequest.index());
        verify(fileClient).uploadImage(mockFile);
        verify(outboxService).createOutboxRecord(eq(EventType.EQUIPMENT_UPDATED), eq(equipment), any());
        verify(outboxService).save(updateOutboxRecord);
        verify(outboxService).createOutboxRecord(eq(EventType.FILE_MARKED_FOR_DELETION), eq(oldFileName), any());
        verify(outboxService).save(deleteOutboxRecord);
        verify(outboxService).createOutboxRecord(eq(EventType.FILE_MOVE_TO_PERMANENT_STORAGE), eq(savedFileName), any());
        verify(outboxService).save(moveToPermanentStorage);
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

        var updateOutboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.EQUIPMENT_UPDATED), eq(equipment), any()))
                .thenReturn(updateOutboxRecord);

        var result = equipmentService.update(id, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getImageName()).isEqualTo(oldFileName);
        verify(equipmentRepository).findById(id);
        verify(equipmentRepository).existsByIndex(updateRequest.index());
        verify(outboxService).createOutboxRecord(eq(EventType.EQUIPMENT_UPDATED), eq(equipment), any());
        verify(outboxService).save(updateOutboxRecord);
        verifyNoInteractions(fileClient);
        verify(outboxService, never()).createOutboxRecord(eq(EventType.FILE_MARKED_FOR_DELETION), any(), any());
        verify(outboxService, never()).createOutboxRecord(eq(EventType.FILE_MOVE_TO_PERMANENT_STORAGE), any(), any());
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
        verifyNoInteractions(outboxService);
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
        verifyNoInteractions(outboxService);
    }

    @Test
    void update_shouldSuccess_whenIndexHasNotChanged() {
        var id = 1L;
        var equipment = createTestEquipment();
        var updateRequest = createUpdateEquipmentRequest(null);
        equipment.setIndex(updateRequest.index());
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.of(equipment));
        var updateOutboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.EQUIPMENT_UPDATED), eq(equipment), any()))
                .thenReturn(updateOutboxRecord);

        var result = equipmentService.update(id, updateRequest);

        assertThat(result).isNotNull();
        verify(equipmentRepository).findById(id);
        verify(equipmentRepository, never()).existsByIndex(anyString());
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
        var updateOutboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.EQUIPMENT_UPDATED), eq(equipment), any()))
                .thenReturn(updateOutboxRecord);

        var result = equipmentService.update(id, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getImageName()).isEqualTo(fileUploadResponse.fileName());
        verify(equipmentRepository).findById(id);
        verify(equipmentRepository).existsByIndex(updateRequest.index());
        verify(fileClient).uploadImage(mockFile);
        verify(outboxService).createOutboxRecord(eq(EventType.EQUIPMENT_UPDATED), eq(equipment), any());
        verify(outboxService).save(updateOutboxRecord);
        verify(outboxService, never()).createOutboxRecord(eq(EventType.FILE_MARKED_FOR_DELETION), any(), any());
    }

    @Test
    void deleteById_shouldDeleteSuccess() {
        var id = 1L;
        var equipment = createTestEquipment();
        when(equipmentRepository.findById(id))
                .thenReturn(Optional.of(equipment));
        var outboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.EQUIPMENT_DELETED), eq(equipment), any()))
                .thenReturn(outboxRecord);

        equipmentService.delete(id);

        verify(outboxService).createOutboxRecord(eq(EventType.EQUIPMENT_DELETED), eq(equipment), any());
        verify(equipmentRepository).findById(id);
        verify(outboxService).save(outboxRecord);
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
        verifyNoInteractions(outboxService);
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
