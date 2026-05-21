package by.niruin.library.service;

import by.niruin.library.client.FileClient;
import by.niruin.library.domain.Equipment;
import by.niruin.library.domain.EquipmentType;
import by.niruin.library.exception.EntityAlreadyExistException;
import by.niruin.library.exception.EntityNotFoundException;
import by.niruin.library.mapper.EquipmentMapper;
import by.niruin.library.model.equipment.UpdateEquipmentRequest;
import by.niruin.library.model.event.EventType;
import by.niruin.library.model.event.file.EquipmentOperationSuccessfulEvent;
import by.niruin.library.model.event.file.FileDeletedEvent;
import by.niruin.library.repository.EquipmentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EquipmentService {
    private final FileClient fileClient;
    private final EquipmentRepository equipmentRepository;
    private final TransactionOutboxService outboxService;
    private final EquipmentMapper equipmentMapper;
    private final TransactionTemplate transactionTemplate;

    public EquipmentService(FileClient fileClient, EquipmentRepository equipmentRepository,
                            TransactionOutboxService outboxService, EquipmentMapper equipmentMapper,
                            TransactionTemplate transactionTemplate) {
        this.fileClient = fileClient;
        this.equipmentRepository = equipmentRepository;
        this.outboxService = outboxService;
        this.equipmentMapper = equipmentMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @CachePut(value = "equipment", key = "#result.id")
    public Equipment save(Equipment equipment, MultipartFile file) {
        final String fileName = uploadFile(file);

        return transactionTemplate.execute(status -> {
            validateEquipmentIndex(equipment.getIndex());

            equipment.setImageName(fileName);
            var saved = equipmentRepository.save(equipment);

            publishEquipmentCreatedEvent(saved);

            if (fileName != null) {
                publishMoveFileOutboxRecord(fileName);
            }

            return saved;
        });
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
        final String newFileName = uploadFile(request.file());

        return transactionTemplate.execute(status -> {
            var existing = findEquipmentById(id);
            validateUniqueIndex(existing.getIndex(), request.index());

            var oldFileName = existing.getImageName();
            updateFields(existing, request, newFileName);

            publishEquipmentUpdatedEvent(existing);

            if (newFileName != null) {
                publishMoveFileOutboxRecord(newFileName);

                if (oldFileName != null) {
                    publishFileDeletionEvent(oldFileName);
                }
            }

            return existing;
        });
    }

    @Transactional
    @CacheEvict(value = "equipment", key = "#id")
    public void delete(Long id) {
        var equipment = findEquipmentById(id);

        equipmentRepository.delete(equipment);

        publishEquipmentDeletedEvent(equipment);

        publishFileDeletionEvent(equipment.getImageName());
    }

    private String uploadFile(MultipartFile file) {
        return hasImage(file) ? fileClient.uploadImage(file).fileName() : null;
    }

    private boolean hasImage(MultipartFile multipartFile) {
        return multipartFile != null && !multipartFile.isEmpty();
    }

    private void validateEquipmentIndex(String index) {
        if (equipmentRepository.existsByIndex(index)) {
            throw new EntityAlreadyExistException();
        }
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

    private void updateFields(Equipment equipment, UpdateEquipmentRequest request, String newFileName) {
        equipment.setName(request.name());
        equipment.setIndex(request.index());
        equipment.setDescription(request.description());
        equipment.setType(EquipmentType.valueOf(request.type()));

        if (newFileName != null) {
            equipment.setImageName(newFileName);
        }
    }

    private void publishEquipmentCreatedEvent(Equipment equipment) {
        var equipmentCreatedEvent = outboxService.createOutboxRecord(
                EventType.EQUIPMENT_CREATED,
                equipment,
                equipmentMapper::toCreatedEvent);
        outboxService.save(equipmentCreatedEvent);
    }

    private void publishEquipmentDeletedEvent(Equipment equipment) {
        var equipmentDeletionEvent = outboxService.createOutboxRecord(EventType.EQUIPMENT_DELETED,
                equipment,
                equipmentMapper::toDeletedEvent);
        outboxService.save(equipmentDeletionEvent);
    }

    private void publishEquipmentUpdatedEvent(Equipment equipment) {
        var notificationOutboxRecord = outboxService.createOutboxRecord(
                EventType.EQUIPMENT_UPDATED,
                equipment,
                equipmentMapper::toUpdatedEvent);
        outboxService.save(notificationOutboxRecord);
    }

    private void publishMoveFileOutboxRecord(String fileName) {
        var moveFileOutboxRecord = outboxService.createOutboxRecord(
                EventType.FILE_MOVE_TO_PERMANENT_STORAGE,
                fileName,
                EquipmentOperationSuccessfulEvent::new);
        outboxService.save(moveFileOutboxRecord);
    }

    private void publishFileDeletionEvent(String oldFileName) {
        if (oldFileName != null) {
            var deleteFileOutboxRecord = outboxService.createOutboxRecord(
                    EventType.FILE_MARKED_FOR_DELETION,
                    oldFileName,
                    FileDeletedEvent::new);
            outboxService.save(deleteFileOutboxRecord);
        }
    }
}
