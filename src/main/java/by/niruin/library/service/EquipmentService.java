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
        validateEquipmentIndex(equipment);

        return hasImage(image) ? saveWithFile(equipment, image) : saveWithoutFile(equipment);
    }

    @Cacheable(value = "equipment", key = "#id")
    @Transactional(readOnly = true)
    public Equipment findById(Long id) {
        return findEquipmentById(id);
    }

    @Transactional(readOnly = true)
    public Page<Equipment> findAll(Pageable pageable) {
        return equipmentRepository.findAll(pageable);
    }

    @CachePut(value = "equipment", key = "#id")
    public Equipment update(Long id, UpdateEquipmentRequest request) {
        var equipment = findEquipmentById(id);

        validateUniqueIndex(equipment.getIndex(), request.index());

        return hasImage(request.file()) ? updateWithFile(equipment, request) : updateWithoutFile(equipment, request);
    }

    @CacheEvict(value = "equipment", key = "#id")
    public void delete(Long id) {
        var equipment = findEquipmentById(id);

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

    private void validateEquipmentIndex(Equipment equipment) {
        if (equipmentRepository.existsByIndex(equipment.getIndex())) {
            throw new EntityAlreadyExistException();
        }
    }

    private boolean hasImage(MultipartFile multipartFile) {
        return multipartFile != null && !multipartFile.isEmpty();
    }

    private Equipment saveWithFile(Equipment equipment, MultipartFile image) {
        String newFileName = fileClient.uploadImage(image).fileName();

        try {
            return saveEquipment(equipment, newFileName);
        } catch (Exception e) {
            rollbackUploadedImage(newFileName);

            throw e;
        }
    }

    private Equipment saveWithoutFile(Equipment equipment) {
        return saveEquipment(equipment, null);
    }

    private Equipment saveEquipment(Equipment equipment, String imageName) {
        equipment.setImageName(imageName);
        var result = equipmentRepository.save(equipment);

        var outboxRecord = outboxService.createOutboxRecord(
                EventType.EQUIPMENT_CREATED,
                result,
                equipmentMapper::toCreatedEvent);
        outboxService.save(outboxRecord);

        return result;
    }

    private void rollbackUploadedImage(String newFileName) {
        if (newFileName == null) {
            return;
        }

        var fileOutboxRecord = outboxService.createOutboxRecord(EventType.IMAGE_DELETED,
                newFileName,
                ImageDeletedEvent::new);
        outboxService.saveInNewTransaction(fileOutboxRecord);
    }

    private Equipment findEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
    }

    private void validateUniqueIndex(String currentIndex, String newIndex) {
        if (!currentIndex.equals(newIndex) && equipmentRepository.existsByIndex(newIndex)) {
            throw new EntityAlreadyExistException();
        }
    }

    private Equipment updateWithFile(Equipment equipment, UpdateEquipmentRequest request) {
        var oldFileName = equipment.getImageName();
        var newFileName = fileClient.uploadImage(request.file()).fileName();

        try {
            var updated = updateEquipment(equipment, request, newFileName);

            if (oldFileName != null) {
                var fileOutboxRecord = outboxService.createOutboxRecord(EventType.IMAGE_DELETED,
                        oldFileName,
                        ImageDeletedEvent::new);
                outboxService.save(fileOutboxRecord);
            }

            return updated;
        } catch (Exception e) {
            rollbackUploadedImage(newFileName);
            throw e;
        }
    }

    private Equipment updateWithoutFile(Equipment equipment, UpdateEquipmentRequest request) {
        return updateEquipment(equipment, request, null);
    }

    private Equipment updateEquipment(Equipment equipment, UpdateEquipmentRequest request, String newFileName) {
        updateFields(equipment, request, newFileName);
        var updated = equipmentRepository.save(equipment);

        var outboxRecord = outboxService.createOutboxRecord(
                EventType.EQUIPMENT_UPDATED,
                updated,
                equipmentMapper::toUpdatedEvent);
        outboxService.save(outboxRecord);

        return updated;
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
