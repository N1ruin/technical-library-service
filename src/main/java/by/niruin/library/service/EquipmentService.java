package by.niruin.library.service;

import by.niruin.library.client.FileClient;
import by.niruin.library.domain.Equipment;
import by.niruin.library.domain.EquipmentType;
import by.niruin.library.model.event.EventType;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.mapper.EquipmentMapper;
import by.niruin.library.model.equipment.UpdateEquipmentRequest;
import by.niruin.library.model.event.file.ImageDeletedEvent;
import by.niruin.library.repository.EquipmentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class EquipmentService {
    private final FileClient fileClient;
    private final EquipmentRepository equipmentRepository;
    private final TransactionOutboxService outboxService;
    private final EquipmentMapper equipmentMapper;

    public EquipmentService(FileClient fileClient, EquipmentRepository equipmentRepository,
                            TransactionOutboxService outboxService, EquipmentMapper equipmentMapper) {
        this.fileClient = fileClient;
        this.equipmentRepository = equipmentRepository;
        this.outboxService = outboxService;
        this.equipmentMapper = equipmentMapper;
    }

    @CachePut(value = "equipment", key = "#result.id", unless = "#result == null")
    public Equipment save(Equipment equipment, MultipartFile image) {
        if (equipmentRepository.existsByIndex(equipment.getIndex())) {
            throw new EntityAlreadyExistException();
        }

        String uploadedFileName = null;
        if (image != null && !image.isEmpty()) {
            uploadedFileName = fileClient.uploadImage(image).fileName();
        }

        try {
            equipment.setImageName(uploadedFileName);
            var result = equipmentRepository.save(equipment);
            var outboxRecord = outboxService.createOutboxRecord(
                    EventType.EQUIPMENT_CREATED,
                    result,
                    equipmentMapper::toCreatedEvent);
            outboxService.save(outboxRecord);

            return result;
        } catch (Exception e) {
            if (uploadedFileName != null) {
                var fileOutboxRecord = outboxService.createOutboxRecord(
                        EventType.IMAGE_DELETED,
                        uploadedFileName,
                        ImageDeletedEvent::new);

                outboxService.saveInNewTransaction(fileOutboxRecord);
            }
            throw e;
        }
    }

    @Cacheable(value = "equipment", key = "#id")
    @Transactional(readOnly = true)
    public Equipment findById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Equipment> findAll(Pageable pageable) {
        return equipmentRepository.findAll(pageable);
    }

    @CachePut(value = "equipment", key = "#id")
    public Equipment update(Long id, UpdateEquipmentRequest request) {
        var equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        if (!equipment.getIndex().equals(request.index()) && equipmentRepository.existsByIndex(request.index())) {
            throw new EntityAlreadyExistException();
        }

        String oldFileName = equipment.getImageName();
        String newFileName = null;

        if (request.file() != null && !request.file().isEmpty()) {
            newFileName = fileClient.uploadImage(request.file()).fileName();
        }

        updateFields(equipment, request, newFileName);

        var updated = equipmentRepository.save(equipment);

        var outboxRecord = outboxService.createOutboxRecord(EventType.EQUIPMENT_UPDATED,
                equipment,
                equipmentMapper::toUpdatedEvent);
        outboxService.save(outboxRecord);

        if (newFileName != null && oldFileName != null) {
            var fileOutboxRecord = outboxService.createOutboxRecord(EventType.IMAGE_DELETED,
                    oldFileName,
                    ImageDeletedEvent::new);
            outboxService.save(fileOutboxRecord);
        }

        return updated;
    }

    @CacheEvict(value = "equipment", key = "#id")
    public void delete(Long id) {
        var equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));

        equipmentRepository.delete(equipment);

        var outboxRecord = outboxService.createOutboxRecord(EventType.EQUIPMENT_DELETED,
                equipment,
                equipmentMapper::toDeletedEvent);
        outboxService.save(outboxRecord);

        var fileName = equipment.getImageName();
        if (fileName != null) {
            var fileOutboxRecord = outboxService.createOutboxRecord(EventType.IMAGE_DELETED,
                    fileName,
                    ImageDeletedEvent::new);
            outboxService.save(fileOutboxRecord);
        }
    }

    private void updateFields(Equipment equipment, UpdateEquipmentRequest request, String newFileName) {
        equipment.setName(request.name());
        equipment.setIndex(request.index());
        equipment.setDescription(request.description());
        equipment.setType(EquipmentType.valueOf(request.type()));

        if (newFileName != null) {
            equipment.setImageName(newFileName);
        }
    }
}
