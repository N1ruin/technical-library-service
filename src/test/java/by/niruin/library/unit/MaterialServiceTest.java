package by.niruin.library.unit;

import by.niruin.library.model.event.EventType;
import by.niruin.library.domain.Material;
import by.niruin.library.domain.TransactionOutboxRecord;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.mapper.MaterialMapper;
import by.niruin.library.model.material.UpdateMaterialRequest;
import by.niruin.library.repository.MaterialRepository;
import by.niruin.library.service.MaterialService;
import by.niruin.library.service.TransactionOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private TransactionOutboxService outboxService;
    @Mock
    private MaterialMapper materialMapper;
    @InjectMocks
    private MaterialService materialService;

    @Test
    void saveSuccess() {
        var material = createTestMaterial();
        when(materialRepository.existsByNameAndStandard(material.getName(), material.getStandard()))
                .thenReturn(false);
        when(materialRepository.save(material))
                .thenReturn(material);
        var outboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.MATERIAL_CREATED), eq(material), any()))
                .thenReturn(outboxRecord);

        var saved = materialService.save(material);

        assertThat(material).usingRecursiveComparison()
                .isEqualTo(saved);
        verify(outboxService).createOutboxRecord(eq(EventType.MATERIAL_CREATED), eq(material), any());
        verify(materialRepository).existsByNameAndStandard(material.getName(), material.getStandard());
        verify(materialRepository).save(material);
        verify(outboxService).save(outboxRecord);
    }

    @Test
    void save_shouldThrowsEntityAlreadyExist() {
        var material = createTestMaterial();
        when(materialRepository.existsByNameAndStandard(material.getName(), material.getStandard()))
                .thenReturn(true);

        assertThatThrownBy(() -> materialService.save(material))
                .isInstanceOf(EntityAlreadyExistException.class);

        verify(materialRepository).existsByNameAndStandard(material.getName(), material.getStandard());
        verify(materialRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void findById_shouldReturnMaterial() {
        var material = createTestMaterial();
        var id = 1L;
        when(materialRepository.findById(id))
                .thenReturn(Optional.of(material));

        var found = materialService.findById(id);

        assertThat(material).usingRecursiveComparison()
                .isEqualTo(found);
        verify(materialRepository).findById(id);
    }

    @Test
    void findById_shouldThrowsEntityNotFound() {
        var id = 10L;
        when(materialRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.findById(id))
                .isInstanceOf(EntityNotFoundException.class);
        verify(materialRepository).findById(id);
    }

    @Test
    void findAll_shouldReturnEmptyList() {
        when(materialRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());
        var pageRequest = PageRequest.of(0, 10);

        var materialPage = materialService.findAll(pageRequest);

        assertThat(materialPage).hasSize(0);
        verify(materialRepository).findAll(any(PageRequest.class));
    }

    @Test
    void findAll_shouldReturnListWithSizeOne() {
        var material = createTestMaterial();
        var expectedPage = new PageImpl<>(List.of(material));
        when(materialRepository.findAll(any(Pageable.class)))
                .thenReturn(expectedPage);
        var pageRequest = PageRequest.of(0, 10);

        var materialPage = materialService.findAll(pageRequest);

        assertThat(materialPage).hasSize(1);
        assertThat(materialPage.getContent().getFirst()).usingRecursiveComparison()
                .isEqualTo(material);
        verify(materialRepository).findAll(any(PageRequest.class));
    }

    @Test
    void update_shouldReturnUpdatedMaterial() {
        var id = 1L;
        var material = createTestMaterial();
        var updateRequest = createUpdateMaterialRequest();
        when(materialRepository.findById(id))
                .thenReturn(Optional.of(material));
        when(materialRepository.existsByNameAndStandard(updateRequest.name(), updateRequest.standard()))
                .thenReturn(false);
        var outboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.MATERIAL_UPDATED), eq(material), any()))
                .thenReturn(outboxRecord);

        var updated = materialService.update(id, updateRequest);

        assertThat(updated.getName()).isEqualTo(updateRequest.name());
        assertThat(updated.getSupplierCode()).isEqualTo(updateRequest.supplierCode());
        assertThat(updated.getDescription()).isEqualTo(updateRequest.description());
        assertThat(updated.getStandard()).isEqualTo(updateRequest.standard());
        verify(materialRepository).findById(id);
        verify(materialRepository).existsByNameAndStandard(updateRequest.name(), updateRequest.standard());
        verify(outboxService).save(outboxRecord);
    }

    @Test
    void update_shouldThrowEntityNotFound() {
        var id = 1L;
        var updateRequest = createUpdateMaterialRequest();
        when(materialRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class);

        verify(materialRepository).findById(id);
        verify(materialRepository, never()).existsByNameAndStandard(updateRequest.name(), updateRequest.standard());
        verifyNoInteractions(outboxService);
    }

    @Test
    void update_shouldThrowEntityAlreadyExist_whenNewNameAndStandardExist() {
        var id = 1L;
        var updateRequest = createUpdateMaterialRequest();
        when(materialRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.update(id, updateRequest))
                .isInstanceOf(EntityNotFoundException.class);
        verify(materialRepository).findById(id);
        verify(materialRepository, never()).existsByNameAndStandard(updateRequest.name(), updateRequest.standard());
        verifyNoInteractions(outboxService);
    }

    @Test
    void deleteById_shouldDeleteSuccess() {
        var id = 1L;
        var material = createTestMaterial();
        when(materialRepository.findById(id))
                .thenReturn(Optional.of(material));
        var outboxRecord = mock(TransactionOutboxRecord.class);
        when(outboxService.createOutboxRecord(eq(EventType.MATERIAL_DELETED), eq(material), any()))
                .thenReturn(outboxRecord);

        materialService.deleteById(id);

        verify(outboxService).createOutboxRecord(eq(EventType.MATERIAL_DELETED), eq(material), any());
        verify(materialRepository).findById(id);
        verify(outboxService).save(outboxRecord);
    }

    @Test
    void deleteById_shouldThrowEntityNotFound() {
        var id = 1L;
        when(materialRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.deleteById(id))
                .isInstanceOf(EntityNotFoundException.class);
        verify(materialRepository).findById(id);
        verify(materialRepository, never()).deleteById(id);
        verifyNoInteractions(outboxService);
    }

    private Material createTestMaterial() {
        var material = new Material();
        material.setName("Литол-24");
        material.setDescription("Для смазки");
        material.setSupplierCode("245");
        material.setStandard("ГОСТ 1234-1234");

        return material;
    }

    private UpdateMaterialRequest createUpdateMaterialRequest() {
        var name = "Масло И-20А";
        var description = "Масло для заправки трансмиссии";
        var standard = "ГОСТ 1111-2222";
        var supplierCode = "111";

        return new UpdateMaterialRequest(name, description, standard, supplierCode);
    }
}
